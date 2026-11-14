package ir.gharri.vkauth;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;

public final class Vkauth extends JavaPlugin {

    private StorageManager storage;
    private AuthManager auth;
    private BukkitTask linkTask;

    @Override
    public void onEnable() {
        ConfigVk cfg = new ConfigVk(this);

        storage = new StorageManager(this, cfg);
        if (!storage.init()) {
            getLogger().severe("No storage backend could be started. Disabling vkauth.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        auth = new AuthManager(this, cfg, storage);
        getServer().getPluginManager().registerEvents(new AuthListener(auth, cfg), this);

        AuthCommand command = new AuthCommand(auth, cfg);
        getCommand("register").setExecutor(command);
        getCommand("login").setExecutor(command);
        getCommand("logout").setExecutor(command);

        auth.startTask();

        RankLink link = new RankLink(this, cfg, storage);
        getCommand("vkauth").setExecutor(new AdminCommand(this, cfg, storage, link));
        if (cfg.onlineRankvk() || cfg.deleteWorldData()) {
            long period = cfg.linkCheckMinutes() * 1200L;
            // the first run only remembers who has an account; every later run removes deleted accounts from RankVK
            linkTask = getServer().getScheduler().runTaskTimerAsynchronously(this, link::scan, 40L, period);
        }

        for (Player player : new ArrayList<>(getServer().getOnlinePlayers())) {
            auth.handleJoin(player);
        }
    }

    @Override
    public void onDisable() {
        if (linkTask != null) linkTask.cancel();
        if (auth != null) auth.shutdown();
        if (storage != null) storage.close();
    }
}
