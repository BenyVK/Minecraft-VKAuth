package ir.gharri.vkauth;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The link with RankVK (the rank / permission plugin), switched by online-rankvk and match-signalVK.
 * <ul>
 *   <li>online-rankvk: when an account disappears from vkauth (deleted by /vkauth delete, or by hand in
 *       accounts.yml / the database) the RankVK data of that player is removed too.</li>
 *   <li>match-signalVK: accounts and RankVK data live in one table (see {@link SharedMysqlStorage}).</li>
 * </ul>
 * The plugins do not share classes: RankVK is called through reflection.
 */
public final class RankLink {

    /** removeFromRank: RankVK is not installed / not started yet, try again later. */
    private static final int NOT_READY = -2;
    /** removeFromRank: RankVK has online-authvk: false and ignored the request. */
    private static final int REFUSED = -1;

    private final Vkauth plugin;
    private final ConfigVk cfg;
    private final StorageManager storage;
    private final File ledgerFile;
    private boolean refusedLogged;

    public RankLink(Vkauth plugin, ConfigVk cfg, StorageManager storage) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.storage = storage;
        this.ledgerFile = new File(plugin.getDataFolder(), "linked-accounts.yml");
    }

    // ------------------------------------------------------------ match-signalVK

    /**
     * Returns the table to share with RankVK, or null. Sharing needs: match-signalVK true here AND in RankVK,
     * mysql in both, the same host / port / database and the same match-table. Otherwise it explains why not.
     */
    public static String sharedTable(Vkauth plugin, ConfigVk cfg, List<String> types) {
        if (!cfg.matchSignal()) return null;
        if (!types.contains("mysql")) return skip(plugin, "storage.type must contain mysql");
        if (cfg.matchTable().equalsIgnoreCase(cfg.str("storage.mysql.table"))) {
            return skip(plugin, "match-table must differ from storage.mysql.table ('" + cfg.str("storage.mysql.table")
                    + "'), otherwise the old accounts table would be mixed up with the shared one");
        }

        File rankFile = new File(plugin.getDataFolder().getParentFile(), "RankVK/configvk.yml");
        if (!rankFile.isFile()) {
            return skip(plugin, "RankVK has no configvk.yml in plugins/RankVK (is it installed and started once?)");
        }
        YamlConfiguration rank = YamlConfiguration.loadConfiguration(rankFile);

        if (!rank.getBoolean("match-signalVK", false)) return skip(plugin, "match-signalVK is false in RankVK");
        String method = rank.getString("storage-method", "json").trim().toLowerCase();
        if (!method.equals("mysql") && !method.equals("mariadb")) {
            return skip(plugin, "RankVK storage-method is " + method + " (it must be mysql or mariadb)");
        }

        String address = rank.getString("data.address", "127.0.0.1:3306").trim();
        String rankHost = address;
        int rankPort = 3306;
        int colon = address.lastIndexOf(':');
        if (colon > 0) {
            rankHost = address.substring(0, colon);
            try {
                rankPort = Integer.parseInt(address.substring(colon + 1).trim());
            } catch (NumberFormatException e) {
                return skip(plugin, "RankVK data.address is not host:port");
            }
        }
        String rankDb = rank.getString("data.database", "rankvk");

        String host = cfg.str("storage.mysql.host");
        int port = cfg.integer("storage.mysql.port");
        String db = cfg.str("storage.mysql.database");
        if (!normalizeHost(host).equals(normalizeHost(rankHost)) || port != rankPort || !db.equalsIgnoreCase(rankDb)) {
            return skip(plugin, "vkauth uses " + host + ":" + port + "/" + db + " but RankVK uses "
                    + rankHost + ":" + rankPort + "/" + rankDb + ". Enter the same database in both configs");
        }
        String rankTable = rank.getString("match-table", "players").trim();
        if (!cfg.matchTable().equalsIgnoreCase(rankTable)) {
            return skip(plugin, "match-table is '" + cfg.matchTable() + "' here but '" + rankTable + "' in RankVK");
        }
        plugin.getLogger().info("match-signalVK is on: accounts are shared with RankVK in table '" + cfg.matchTable() + "'.");
        return cfg.matchTable();
    }

    private static String normalizeHost(String h) {
        String x = h == null ? "" : h.trim().toLowerCase();
        return x.equals("localhost") || x.equals("::1") ? "127.0.0.1" : x;
    }

    private static String skip(Vkauth plugin, String reason) {
        plugin.getLogger().warning("match-signalVK is true but not active: " + reason
                + ". Accounts stay in the normal storage.");
        return null;
    }

    // ------------------------------------------------------------ online-rankvk

    /**
     * Compares who has an account now with who had one at the last check. Everyone who vanished
     * is removed from RankVK. Blocking - call from an async thread (the first run only takes a snapshot).
     */
    public synchronized void scan() {
        if (!cfg.onlineRankvk() && !cfg.deleteWorldData()) return;
        boolean byName = !cfg.replayIdAccount();

        Map<String, String> current = storage.identities(byName);
        if (current == null) return; // a storage could not be read: change nothing

        Ledger known = readLedger();
        if (known == null || known.byName != byName) {
            writeLedger(byName, current); // first run (or replay-id-account changed): remember, delete nothing
            return;
        }

        List<String> gone = new ArrayList<>();
        for (String id : known.accounts.keySet()) {
            if (!current.containsKey(id)) gone.add(id);
        }
        if (gone.isEmpty()) {
            if (!known.accounts.equals(current)) writeLedger(byName, current);
            return;
        }
        if (current.isEmpty()) {
            plugin.getLogger().warning("RankVK link: all " + gone.size() + " accounts look deleted (the storage is "
                    + "empty). RankVK data was NOT touched. Delete accounts one by one if that is what you want.");
            return;
        }

        Map<String, String> keep = new LinkedHashMap<>(current);
        Map<String, String> purge = new LinkedHashMap<>();
        for (String id : gone) {
            String name = known.accounts.get(id);
            int result = cfg.onlineRankvk() ? removeFromRank(id, name, byName) : 0;
            if (result != NOT_READY) purge.put(id, name);
            if (result == NOT_READY) {
                keep.put(id, name); // RankVK is not running: try again at the next check
            } else if (result == REFUSED) {
                if (!refusedLogged) {
                    refusedLogged = true;
                    plugin.getLogger().info("RankVK ignored the deletion because online-authvk is false in its configvk.yml.");
                }
            } else {
                plugin.getLogger().info("Account " + (name == null || name.isEmpty() ? id : name)
                        + " was deleted, so RankVK removed " + result + " player record(s) as well.");
            }
        }
        writeLedger(byName, keep);
        if (cfg.deleteWorldData()) purgeWorldData(purge, byName);
    }

    /**
     * Removes a player completely: RankVK data (online-rankvk) and, with delete-world-data, the data files in the
     * worlds. Used by /vkauth purge. {@code id} is the uuid, or the name when {@code byName}. Async thread.
     */
    public void forget(String id, String name, boolean byName) {
        if (cfg.onlineRankvk()) removeFromRank(id, name, byName);
        if (cfg.deleteWorldData()) purgeWorldData(Map.of(id, name == null ? "" : name), byName);
    }

    /**
     * Deletes playerdata / stats / advancements of the given players in every world. Players who are online are
     * kicked first (their data is written again when they leave), the files are deleted a moment later.
     *
     * @param targets id -> name (id = uuid, or the lower-cased name when {@code byName})
     */
    public void purgeWorldData(Map<String, String> targets, boolean byName) {
        if (targets.isEmpty()) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            java.util.Set<UUID> uuids = new java.util.LinkedHashSet<>();
            for (Map.Entry<String, String> t : targets.entrySet()) {
                if (!byName) {
                    try {
                        uuids.add(UUID.fromString(t.getKey()));
                    } catch (IllegalArgumentException ignored) {
                    }
                    continue;
                }
                String name = t.getValue();
                if (name == null || name.isBlank()) continue;
                for (org.bukkit.OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                    if (name.equalsIgnoreCase(op.getName())) uuids.add(op.getUniqueId());
                }
            }
            for (UUID uuid : uuids) {
                org.bukkit.entity.Player online = Bukkit.getPlayer(uuid);
                if (online != null) online.kick(cfg.msg("account-deleted-kick"));
            }
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                int files = 0;
                for (UUID uuid : uuids) files += deleteWorldFiles(uuid);
                if (files > 0) {
                    plugin.getLogger().info("delete-world-data: removed " + files + " file(s) of "
                            + uuids.size() + " player(s) from the worlds.");
                }
            }, 40L);
        });
    }

    private static int deleteWorldFiles(UUID uuid) {
        String[] files = {"playerdata/" + uuid + ".dat", "playerdata/" + uuid + ".dat_old",
                "stats/" + uuid + ".json", "advancements/" + uuid + ".json"};
        int count = 0;
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (String f : files) {
                File file = new File(world.getWorldFolder(), f);
                if (file.isFile() && file.delete()) count++;
            }
        }
        return count;
    }

    private int removeFromRank(String id, String name, boolean byName) {
        Plugin rank = Bukkit.getPluginManager().getPlugin("RankVK");
        if (rank == null || !rank.isEnabled()) return NOT_READY;
        try {
            UUID uuid = byName ? new UUID(0L, 0L) : UUID.fromString(id);
            Method m = rank.getClass().getMethod("removeLinkedUser", UUID.class, String.class, boolean.class);
            Object result = m.invoke(rank, uuid, name, byName);
            return result instanceof Integer i ? i : NOT_READY;
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            plugin.getLogger().warning("Could not tell RankVK about the deleted account " + id + ": " + e);
            return NOT_READY;
        }
    }

    // ------------------------------------------------------------ remembered accounts

    private record Ledger(boolean byName, Map<String, String> accounts) {
    }

    private Ledger readLedger() {
        if (!ledgerFile.isFile()) return null;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(ledgerFile);
        Map<String, String> accounts = new LinkedHashMap<>();
        for (Map<?, ?> entry : y.getMapList("accounts")) {
            Object id = entry.get("id");
            if (id == null) continue;
            Object name = entry.get("name");
            accounts.put(String.valueOf(id), name == null ? "" : String.valueOf(name));
        }
        return new Ledger("name".equals(y.getString("mode")), accounts);
    }

    private void writeLedger(boolean byName, Map<String, String> accounts) {
        YamlConfiguration y = new YamlConfiguration();
        y.options().header("vkauth: who had an account at the last check (used to find deleted accounts for RankVK). Do not edit.");
        y.set("mode", byName ? "name" : "uuid");
        List<Map<String, String>> list = new ArrayList<>();
        for (Map.Entry<String, String> e : accounts.entrySet()) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("id", e.getKey());
            row.put("name", e.getValue());
            list.add(row);
        }
        y.set("accounts", list);
        try {
            File tmp = new File(ledgerFile.getParentFile(), ledgerFile.getName() + ".tmp");
            y.save(tmp);
            java.nio.file.Files.move(tmp.toPath(), ledgerFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save linked-accounts.yml: " + e.getMessage());
        }
    }
}
