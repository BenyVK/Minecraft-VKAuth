package ir.gharri.vkauth;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** /vkauth delete <name> and /vkauth purge [confirm]. */
public final class AdminCommand implements CommandExecutor {

    private final Vkauth plugin;
    private final ConfigVk cfg;
    private final StorageManager storage;
    private final RankLink link;

    public AdminCommand(Vkauth plugin, ConfigVk cfg, StorageManager storage, RankLink link) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.storage = storage;
        this.link = link;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("vkauth.admin")) {
            sender.sendMessage(cfg.msg("no-permission"));
            return true;
        }
        final boolean ignoreCase = !cfg.replayIdAccount();
        if (args.length >= 1 && args[0].equalsIgnoreCase("purge") && args.length <= 2) {
            purge(sender, args.length == 2 && args[1].equalsIgnoreCase("confirm"));
            return true;
        }
        if (args.length != 2 || !args[0].equalsIgnoreCase("delete")) {
            sender.sendMessage(cfg.msg("admin-usage"));
            return true;
        }
        final String name = args[1];

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<StoredAccount> removed = storage.deleteByName(name, ignoreCase);
            if (!removed.isEmpty()) {
                link.scan(); // tells RankVK right away (only when online-rankvk is true)
                if (cfg.deleteWorldData()) {
                    Map<String, String> targets = new LinkedHashMap<>();
                    for (StoredAccount a : removed) {
                        if (ignoreCase) targets.put(name.toLowerCase(Locale.ROOT), name);
                        else targets.put(a.uuid().toString(), name);
                    }
                    link.purgeWorldData(targets, ignoreCase);
                }
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (removed.isEmpty()) {
                    sender.sendMessage(cfg.msg("admin-not-found", "player", name));
                    return;
                }
                sender.sendMessage(cfg.msg("admin-deleted", "player", name, "count", String.valueOf(removed.size())));
                for (Player p : new ArrayList<>(Bukkit.getOnlinePlayers())) {
                    boolean same = ignoreCase ? p.getName().equalsIgnoreCase(name) : p.getName().equals(name);
                    if (same) p.kick(cfg.msg("account-deleted-kick"));
                }
            });
        });
        return true;
    }

    /**
     * /vkauth purge          -> lists the offline players that have no account
     * /vkauth purge confirm  -> forgets them (RankVK data with online-rankvk, world files with delete-world-data)
     */
    private void purge(CommandSender sender, boolean confirm) {
        final boolean byName = !cfg.replayIdAccount();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<String, String> accounts = storage.identities(byName);
            if (accounts == null || accounts.isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(cfg.msg("purge-refused")));
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                Map<String, String> lost = new LinkedHashMap<>(); // id -> name
                for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                    String n = op.getName();
                    if (n == null || !op.hasPlayedBefore() || op.isOnline()) continue;
                    String id = byName ? n.toLowerCase(Locale.ROOT) : op.getUniqueId().toString();
                    if (!accounts.containsKey(id)) lost.putIfAbsent(id, n);
                }
                if (lost.isEmpty()) {
                    sender.sendMessage(cfg.msg("purge-none"));
                    return;
                }
                if (!confirm) {
                    List<String> names = new ArrayList<>(lost.values());
                    String shown = String.join(", ", names.subList(0, Math.min(20, names.size())))
                            + (names.size() > 20 ? ", ..." : "");
                    sender.sendMessage(cfg.msg("purge-list", "count", String.valueOf(lost.size()), "players", shown));
                    return;
                }
                sender.sendMessage(cfg.msg("purge-done", "count", String.valueOf(lost.size())));
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    for (Map.Entry<String, String> e : lost.entrySet()) link.forget(e.getKey(), e.getValue(), byName);
                });
            });
        });
    }
}
