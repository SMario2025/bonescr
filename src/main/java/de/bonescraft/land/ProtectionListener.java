package de.bonescraft.land;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;

import java.util.EnumSet;
import java.util.Set;

public class ProtectionListener implements Listener {

    private final BonescraftLand plugin;

    private static final Set<Material> CONTAINERS = EnumSet.of(
            Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL, Material.HOPPER,
            Material.DROPPER, Material.DISPENSER, Material.SHULKER_BOX,
            Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER
    );

    public ProtectionListener(BonescraftLand plugin) {
        this.plugin = plugin;
    }

    private String tr(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private String msgKey(String key, String fallback) {
        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6BonesLand&8]&r ");
        String m = plugin.getConfig().getString("messages." + key, fallback);
        return tr(prefix + m);
    }

    private boolean hasAdminBypass(Player p) {
        return p.hasPermission("bonescraft.land.admin");
    }

    private boolean hasGlobalBuild(Player p) {
        String perm = plugin.getConfig().getString("global-build-permission", "bonescraft.land.globalbuild");
        return p.hasPermission(perm);
    }

    private boolean needsBuildPerm(Player p) {
        if (!plugin.getConfig().getBoolean("require-permission-to-build", false)) return false;
        String perm = plugin.getConfig().getString("build-permission", "bonescraft.land.build");
        return !p.hasPermission(perm) && !hasAdminBypass(p) && !hasGlobalBuild(p);
    }

    /**
     * only-build-in-owned-land = true:
     * - Unclaimed chunks are fully protected (no place/break)
     * - Claimed chunks: only owner/trusted may place/break (and optional build permission)
     *
     * Global build permission bypasses land requirement.
     */
    private boolean canBuildHere(Player p, Block b) {
        if (hasAdminBypass(p) || hasGlobalBuild(p)) return true;

        boolean onlyOwned = plugin.getConfig().getBoolean("only-build-in-owned-land", true);
        Claim c = plugin.claims().get(b.getChunk());

        if (!onlyOwned) {
            if (c == null) return true;
            return c.isAllowed(p.getUniqueId());
        }

        if (c == null) return false;
        if (!c.isAllowed(p.getUniqueId())) return false;
        if (needsBuildPerm(p)) return false;
        return true;
    }

    private boolean isContainer(Material m) {
        if (CONTAINERS.contains(m)) return true;
        String n = m.name();
        return n.endsWith("_SHULKER_BOX") || n.contains("CHEST");
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (!canBuildHere(e.getPlayer(), e.getBlock())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(msgKey("no-build", "&cDu darfst nur in deinem eigenen Land bauen."));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (!canBuildHere(e.getPlayer(), e.getBlock())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(msgKey("no-break", "&cDu darfst nur in deinem eigenen Land abbauen."));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getClickedBlock() == null) return;

        Player p = e.getPlayer();
        Block b = e.getClickedBlock();
        Material type = b.getType();

        boolean protectInteractions = plugin.getConfig().getBoolean("protect-interactions", true);
        boolean protectContainers = plugin.getConfig().getBoolean("protect-containers", true);

        Claim c = plugin.claims().get(b.getChunk());

        // Containers: allow opening (view-only) — looting blocked in inventory events
        if (protectContainers && isContainer(type)) {
            return; // never cancel open
        }

        // Doors/buttons/levers in claimed chunks: only owner/trusted (globalbuild/admin bypass)
        if (protectInteractions && c != null && !hasAdminBypass(p) && !hasGlobalBuild(p) && !c.isAllowed(p.getUniqueId())) {
            String name = type.name();
            boolean isInteract =
                    name.contains("DOOR") || name.contains("BUTTON") || name.contains("LEVER") ||
                    name.contains("TRAPDOOR") || name.contains("FENCE_GATE");
            if (isInteract) e.setCancelled(true);
        }
    }

    private Block resolveInventoryBlock(InventoryHolder holder) {
        if (holder == null) return null;

        if (holder instanceof TileState ts) {
            return ts.getBlock();
        }
        if (holder instanceof org.bukkit.block.Chest chest) {
            return chest.getBlock();
        }
        if (holder instanceof DoubleChest dc) {
            InventoryHolder left = dc.getLeftSide();
            if (left instanceof org.bukkit.block.Chest c) return c.getBlock();
        }
        return null;
    }

    private boolean isTopInventoryContainer(InventoryHolder holder) {
        Block b = resolveInventoryBlock(holder);
        if (b == null) return false;
        return isContainer(b.getType());
    }

    private boolean mayLoot(Player p, Block containerBlock) {
        if (hasAdminBypass(p)) return true;

        Claim c = plugin.claims().get(containerBlock.getChunk());

        // Unclaimed: allow looting by default (if you want unclaimed also view-only, change to false)
        if (c == null) return true;

        if (c.isAllowed(p.getUniqueId())) return true;

        String lootPerm = plugin.getConfig().getString("loot-permission", "bonescraft.land.loot");
        return p.hasPermission(lootPerm);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getView() == null) return;

        InventoryHolder topHolder = e.getView().getTopInventory().getHolder();
        if (!isTopInventoryContainer(topHolder)) return;

        Block containerBlock = resolveInventoryBlock(topHolder);
        if (containerBlock == null) return;

        if (mayLoot(p, containerBlock)) return;

        int topSize = e.getView().getTopInventory().getSize();
        int raw = e.getRawSlot();
        boolean clickedTop = raw >= 0 && raw < topSize;

        // If they click any slot in top inventory, or shift-click to move, block.
        if (clickedTop || e.isShiftClick()) {
            e.setCancelled(true);
            p.sendMessage(msgKey("no-loot", "&cDu darfst hier nur schauen – kein Rang zum Entnehmen."));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        InventoryHolder topHolder = e.getView().getTopInventory().getHolder();
        if (!isTopInventoryContainer(topHolder)) return;

        Block containerBlock = resolveInventoryBlock(topHolder);
        if (containerBlock == null) return;

        if (mayLoot(p, containerBlock)) return;

        int topSize = e.getView().getTopInventory().getSize();
        boolean touchesTop = e.getRawSlots().stream().anyMatch(slot -> slot >= 0 && slot < topSize);
        if (touchesTop) {
            e.setCancelled(true);
            p.sendMessage(msgKey("no-loot", "&cDu darfst hier nur schauen – kein Rang zum Entnehmen."));
        }
    }
}
