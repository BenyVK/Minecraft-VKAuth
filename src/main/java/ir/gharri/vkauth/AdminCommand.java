package ir.gharri.vkauth;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** /vkauth delete <name>  -  deletes an account (and, with online-rankvk, the RankVK data of that player). */
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
        if (args.length != 2 || !args[0].equalsIgnoreCase("delete")) {
            sender.sendMessage(cfg.msg("admin-usage"));
            return true;
        }
        final String name = args[1];
        final boolean ignoreCase = !cfg.replayIdAccount();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<StoredAccount> removed = storage.deleteByName(name, ignoreCase);
            if (!removed.isEmpty()) link.scan(); // tells RankVK right away (only when online-rankvk is true)
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
}
