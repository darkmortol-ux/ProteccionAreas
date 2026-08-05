package com.miplugin.protecciones.listeners;

import com.miplugin.protecciones.ProteccionesPlugin;
import com.miplugin.protecciones.claims.Claim;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class WandListener implements Listener {

    private final ProteccionesPlugin plugin;

    public WandListener(ProteccionesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        int size = plugin.getClaimWand().getSize(event.getItem());
        if (size <= 0) return; // no es un palo de protección

        event.setCancelled(true);
        Block block = event.getClickedBlock();
        if (block == null) return;

        int centerX = block.getX();
        int centerZ = block.getZ();
        String world = block.getWorld().getName();

        if (plugin.getClaimManager().overlapsAny(world, centerX, centerZ, size)) {
            player.sendMessage(ChatColor.RED + "No puedes crear una protección aquí: se solapa con otra ya existente.");
            return;
        }

        int limit = plugin.getClaimLimitService().getLimit(player);
        int current = plugin.getClaimManager().getClaimsOf(player.getUniqueId()).size();
        if (current >= limit) {
            player.sendMessage(ChatColor.RED + "Ya alcanzaste tu límite de protecciones (" + current + "/" + limit + "). "
                    + ChatColor.GRAY + "Elimina una con /proteccion eliminar o consigue un rango con más límite.");
            return;
        }

        if (!isAllowedByWorldGuard(player, block.getWorld(), centerX, centerZ, size)) {
            player.sendMessage(ChatColor.RED + "No puedes crear una protección aquí: el área se solapa con una región de WorldGuard donde no tienes permiso de construir.");
            return;
        }

        Claim claim = plugin.getClaimManager().createClaim(player.getUniqueId(), world, centerX, centerZ, size);
        plugin.getLivesManager().registerFirstClaimIfAbsent(player.getUniqueId(), block.getLocation());

        // Consumir el palo
        if (event.getItem() != null) {
            event.getItem().setAmount(event.getItem().getAmount() - 1);
        }

        player.sendMessage(ChatColor.GREEN + "¡Protección de " + size + "x" + size + " creada! "
                + ChatColor.GRAY + "(" + claim.getMinX() + ", " + claim.getMinZ() + ") a ("
                + claim.getMaxX() + ", " + claim.getMaxZ() + ")");
    }

    /**
     * Si WorldGuard está instalado, verifica que el jugador tenga permiso de construir
     * (flag BUILD) en las 4 esquinas y el centro del área propuesta. Si WorldGuard no
     * está instalado, siempre devuelve true (no aplica ningún chequeo extra).
     */
    private boolean isAllowedByWorldGuard(Player player, org.bukkit.World bukkitWorld, int centerX, int centerZ, int size) {
        var hook = plugin.getWorldGuardHook();
        if (hook == null) return true;

        int half = size / 2;
        int minX = centerX - half;
        int maxX = centerX + half - 1;
        int minZ = centerZ - half;
        int maxZ = centerZ + half - 1;
        int y = player.getLocation().getBlockY();

        int[][] points = {
                {minX, minZ}, {minX, maxZ}, {maxX, minZ}, {maxX, maxZ}, {centerX, centerZ}
        };

        for (int[] p : points) {
            Location loc = new Location(bukkitWorld, p[0], y, p[1]);
            if (!hook.canBuild(player, loc)) {
                return false;
            }
        }
        return true;
    }
}
