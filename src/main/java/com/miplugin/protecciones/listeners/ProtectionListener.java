package com.miplugin.protecciones.listeners;

import com.miplugin.protecciones.ProteccionesPlugin;
import com.miplugin.protecciones.claims.Claim;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Set;

public class ProtectionListener implements Listener {

    private final ProteccionesPlugin plugin;

    // Bloques considerados "contenedor/interactivo" que solo dueño/trusted pueden usar
    // (a menos que el dueño esté en estado RAID).
    private static final Set<Material> INTERACTIVE_BLOCKS = Set.of(
            Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL, Material.ENDER_CHEST,
            Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER,
            Material.CRAFTING_TABLE, Material.ENCHANTING_TABLE,
            Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL,
            Material.BREWING_STAND, Material.CARTOGRAPHY_TABLE, Material.FLETCHING_TABLE,
            Material.GRINDSTONE, Material.LOOM, Material.SMITHING_TABLE, Material.STONECUTTER,
            Material.SHULKER_BOX, Material.BEACON, Material.LECTERN,
            Material.DISPENSER, Material.DROPPER, Material.HOPPER
    );

    public ProtectionListener(ProteccionesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Claim claim = plugin.getClaimManager().getClaimAt(event.getBlock().getLocation());
        if (claim == null) return;
        Player player = event.getPlayer();
        if (canBuild(player, claim)) return;

        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + "Esta zona está protegida. No puedes romper bloques aquí.");
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Claim claim = plugin.getClaimManager().getClaimAt(event.getBlock().getLocation());
        if (claim == null) {
            trackIfHeavyTnt(event);
            return;
        }
        Player player = event.getPlayer();
        if (!canBuild(player, claim)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Esta zona está protegida. No puedes colocar bloques aquí.");
            return;
        }
        trackIfHeavyTnt(event);
    }

    private void trackIfHeavyTnt(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.TNT
                && plugin.getCustomItems().isHeavyTnt(event.getItemInHand())) {
            plugin.getExplosionListener().markHeavyTntBlock(event.getBlock().getLocation());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (!isInteractive(block.getType())) return;

        Claim claim = plugin.getClaimManager().getClaimAt(block.getLocation());
        if (claim == null) return;

        Player player = event.getPlayer();
        if (player.hasPermission("proteccion.admin")) return;
        if (claim.isOwnerOrTrusted(player.getUniqueId())) return;

        // Si el dueño ya perdió todas sus vidas, la protección queda "raideable": cualquiera puede usar cofres, etc.
        if (plugin.getLivesManager().isRaided(claim.getOwner())) return;

        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + "No tienes permiso para usar esto. Pertenece a la protección de otro jugador.");
    }

    private boolean canBuild(Player player, Claim claim) {
        if (player.hasPermission("proteccion.admin")) return true;
        return claim.isOwnerOrTrusted(player.getUniqueId());
    }

    private boolean isInteractive(Material material) {
        return INTERACTIVE_BLOCKS.contains(material) || material.name().endsWith("SHULKER_BOX");
    }
}
