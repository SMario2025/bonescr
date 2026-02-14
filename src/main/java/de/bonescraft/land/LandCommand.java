package de.bonescraft.land;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class LandCommand implements CommandExecutor, TabCompleter {

    private final BonescraftLand plugin;

    public LandCommand(BonescraftLand plugin) {
        this.plugin = plugin;
    }

    private String msg(String s) {
        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6BonesLand&8]&r ");
        return ChatColor.translateAlternateColorCodes('&', prefix + s);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Player only.");
            return true;
        }
        if (!p.hasPermission("bonescraft.land.use")) {
            p.sendMessage(msg("&cKeine Rechte."));
            return true;
        }

        if (args.length == 0) {
            p.sendMessage(msg("&e/land claim &7- kauft den aktuellen Chunk"));
            p.sendMessage(msg("&e/land unclaim &7- entfernt deinen Claim"));
            p.sendMessage(msg("&e/land trust <name> &7- Spieler freigeben"));
            p.sendMessage(msg("&e/land untrust <name> &7- Freigabe entfernen"));
            p.sendMessage(msg("&e/land info &7- Claim Info"));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        Chunk chunk = p.getLocation().getChunk();
        Claim claim = plugin.claims().get(chunk);

        switch (sub) {
            case "claim" -> {
                if (!p.hasPermission("bonescraft.land.claim")) {
                    p.sendMessage(msg("&cDu brauchst einen Rang/Permission um Land zu claimen."));
                    return true;
                }
                if (plugin.claims().isClaimed(chunk)) {
                    p.sendMessage(msg("&cDieser Chunk ist bereits geclaimt."));
                    return true;
                }

                int max = plugin.getConfig().getInt("max-claims-per-player", 5);
                int have = plugin.claims().countClaims(p.getUniqueId());
                if (!p.hasPermission("bonescraft.land.admin") && have >= max) {
                    p.sendMessage(msg("&cDu hast dein Claim-Limit erreicht: &e" + max));
                    return true;
                }

                double price = plugin.getConfig().getDouble("price-per-claim", 2500);
                Economy eco = plugin.economy();
                if (eco != null && !p.hasPermission("bonescraft.land.admin")) {
                    if (eco.getBalance(p) < price) {
                        p.sendMessage(msg("&cNicht genug Geld. Preis: &e" + price));
                        return true;
                    }
                    eco.withdrawPlayer(p, price);
                }

                plugin.claims().set(chunk, new Claim(p.getUniqueId()));
                plugin.claims().save();
                p.sendMessage(msg("&aChunk gekauft/geclaimt! &7(" + chunk.getX() + "," + chunk.getZ() + ")"));
                return true;
            }

            case "unclaim" -> {
                if (claim == null) {
                    p.sendMessage(msg("&cHier ist kein Claim."));
                    return true;
                }
                boolean isOwner = claim.owner.equals(p.getUniqueId());
                if (!isOwner && !p.hasPermission("bonescraft.land.admin")) {
                    p.sendMessage(msg("&cNur der Owner kann unclaimen."));
                    return true;
                }
                plugin.claims().remove(chunk);
                plugin.claims().save();
                p.sendMessage(msg("&aClaim entfernt."));
                return true;
            }

            case "trust" -> {
                if (args.length < 2) {
                    p.sendMessage(msg("&cNutze: /land trust <name>"));
                    return true;
                }
                if (claim == null) {
                    p.sendMessage(msg("&cHier ist kein Claim."));
                    return true;
                }
                if (!claim.owner.equals(p.getUniqueId()) && !p.hasPermission("bonescraft.land.admin")) {
                    p.sendMessage(msg("&cNur der Owner kann freigeben."));
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                claim.trusted.add(target.getUniqueId());
                plugin.claims().save();
                p.sendMessage(msg("&aFreigegeben: &e" + target.getName()));
                return true;
            }

            case "untrust" -> {
                if (args.length < 2) {
                    p.sendMessage(msg("&cNutze: /land untrust <name>"));
                    return true;
                }
                if (claim == null) {
                    p.sendMessage(msg("&cHier ist kein Claim."));
                    return true;
                }
                if (!claim.owner.equals(p.getUniqueId()) && !p.hasPermission("bonescraft.land.admin")) {
                    p.sendMessage(msg("&cNur der Owner kann entfernen."));
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                claim.trusted.remove(target.getUniqueId());
                plugin.claims().save();
                p.sendMessage(msg("&aEntfernt: &e" + target.getName()));
                return true;
            }

            case "info" -> {
                if (claim == null) {
                    p.sendMessage(msg("&eKein Claim in diesem Chunk."));
                    return true;
                }
                OfflinePlayer owner = Bukkit.getOfflinePlayer(claim.owner);
                p.sendMessage(msg("&6Owner: &e" + (owner.getName() != null ? owner.getName() : claim.owner)));
                p.sendMessage(msg("&6Trusted: &e" + claim.trusted.size()));
                return true;
            }

            default -> {
                p.sendMessage(msg("&cUnbekannt. /land"));
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return Arrays.asList("claim", "unclaim", "trust", "untrust", "info");
        return Collections.emptyList();
    }
}
