package de.bonescraft.land;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class PlaytimeCommand implements CommandExecutor, TabCompleter {

    private final BonescraftLand plugin;

    public PlaytimeCommand(BonescraftLand plugin) {
        this.plugin = plugin;
    }

    private String c(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        String month = plugin.playtime().currentMonthKey();

        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("Usage: /playtime <player|top>");
                return true;
            }
            long sec = plugin.playtime().getSeconds(month, p.getUniqueId());
            p.sendMessage(c("&8[&bPlaytime&8]&r &7Deine Spielzeit (" + month + "): &a" + PlaytimeTracker.formatDuration(sec)));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("top")) {
            if (!sender.hasPermission("bonescraft.playtime.admin")) {
                sender.sendMessage(c("&cKeine Rechte."));
                return true;
            }
            var top = plugin.playtime().getTop(month, 10);
            sender.sendMessage(c("&8[&bPlaytime&8]&r &7Top 10 (" + month + "):"));
            int i = 1;
            for (var e : top.entrySet()) {
                sender.sendMessage(c("&7" + (i++) + ". &f" + plugin.playtime().resolveName(e.getKey()) + " &7- &a" + PlaytimeTracker.formatDuration(e.getValue())));
            }
            return true;
        }

        // /playtime <player>
        if (args.length >= 1) {
            if (!(sender instanceof Player) && args.length < 1) return true;

            if (!(sender instanceof Player) && !sender.hasPermission("bonescraft.playtime.admin")) {
                sender.sendMessage(c("&cKeine Rechte."));
                return true;
            }

            if (sender instanceof Player sp && !sp.hasPermission("bonescraft.playtime.admin") && !sp.hasPermission("bonescraft.playtime.view")) {
                sp.sendMessage(c("&cKeine Rechte."));
                return true;
            }

            OfflinePlayer target = plugin.getServer().getOfflinePlayer(args[0]);
            long sec = plugin.playtime().getSeconds(month, target.getUniqueId());
            sender.sendMessage(c("&8[&bPlaytime&8]&r &7Spielzeit von &f" + (target.getName() != null ? target.getName() : target.getUniqueId()) + "&7 (" + month + "): &a" + PlaytimeTracker.formatDuration(sec)));
            return true;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> opts = new ArrayList<>();
            opts.add("top");
            if (sender instanceof Player p && p.hasPermission("bonescraft.playtime.admin")) {
                // cannot list all players reliably
            }
            return opts;
        }
        return Collections.emptyList();
    }
}
