package de.bonescraft.land;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlaytimeListener implements Listener {

    private final BonescraftLand plugin;

    public PlaytimeListener(BonescraftLand plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.playtime().onJoin(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.playtime().onQuit(e.getPlayer());
        plugin.playtime().save();
    }
}
