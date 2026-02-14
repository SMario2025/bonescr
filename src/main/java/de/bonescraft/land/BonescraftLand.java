package de.bonescraft.land;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class BonescraftLand extends JavaPlugin {

    private ClaimManager claimManager;
    private Economy economy; // may be null if Vault isn't installed
    private PlaytimeTracker playtimeTracker;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.claimManager = new ClaimManager(this);
        claimManager.load();

        setupVault();

        this.playtimeTracker = new PlaytimeTracker(this);
        playtimeTracker.load();

        LandCommand landCmd = new LandCommand(this);
        getCommand("land").setExecutor(landCmd);
        getCommand("land").setTabCompleter(landCmd);

        PlaytimeCommand playCmd = new PlaytimeCommand(this);
        getCommand("playtime").setExecutor(playCmd);
        getCommand("playtime").setTabCompleter(playCmd);

        Bukkit.getPluginManager().registerEvents(new ProtectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlaytimeListener(this), this);

        // Autosave playtime
        int autosaveSeconds = getConfig().getInt("playtime.autosave-seconds", 300);
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            try { playtimeTracker.save(); } catch (Exception ignored) {}
        }, autosaveSeconds * 20L, autosaveSeconds * 20L);

        getLogger().info("BonescraftLand enabled. Vault: " + (economy != null));
    }

    @Override
    public void onDisable() {
        try { playtimeTracker.flushAllOnline(); } catch (Exception ignored) {}
        try { playtimeTracker.save(); } catch (Exception ignored) {}
        claimManager.save();
    }

    public ClaimManager claims() {
        return claimManager;
    }

    public Economy economy() {
        return economy;
    }

    public PlaytimeTracker playtime() {
        return playtimeTracker;
    }

    private void setupVault() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            economy = null;
            return;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            economy = null;
            return;
        }
        economy = rsp.getProvider();
    }
}
