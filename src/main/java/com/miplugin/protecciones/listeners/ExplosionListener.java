package com.miplugin.protecciones.listeners;

import com.miplugin.protecciones.ProteccionesPlugin;
import com.miplugin.protecciones.claims.Claim;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Dispenser;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.TNTPrimeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Distingue la TNT especial ("TNT DE USO PESADO") de la TNT normal para que:
 * - La TNT normal (o cualquier otra explosión) NUNCA dañe bloques dentro de una protección.
 * - La TNT especial SÍ pueda destruir bloques de construcción dentro de una protección,
 *   pero jamás los bloques de utilidad (cofres, hornos, mesas, etc.).
 * - Fuera de cualquier protección, todas las explosiones se comportan de forma vanilla.
 */
public class ExplosionListener implements Listener {

    private record PendingHeavy(String world, int x, int y, int z, long timestamp) {
    }

    private final ProteccionesPlugin plugin;
    private final Set<String> heavyTntBlocks = ConcurrentHashMap.newKeySet();
    private final Map<String, PendingHeavy> pendingHeavy = new ConcurrentHashMap<>();
    private Set<Material> protectedMaterials;
    private boolean destroyEverything;

    private static final long MATCH_WINDOW_MILLIS = 15_000L;
    private static final int MATCH_RADIUS = 2;

    public ExplosionListener(ProteccionesPlugin plugin) {
        this.plugin = plugin;
        reloadProtectedMaterials();

        // Limpia entradas viejas cada 20 segundos para no acumular memoria innecesaria.
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::cleanupStaleEntries, 400L, 400L);
    }

    public void reloadProtectedMaterials() {
        Set<Material> materials = new HashSet<>();
        for (String name : plugin.getConfig().getStringList("protected-blocks")) {
            try {
                materials.add(Material.valueOf(name.toUpperCase()));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Material inválido en protected-blocks: " + name);
            }
        }
        this.protectedMaterials = materials;
        this.destroyEverything = plugin.getConfig().getBoolean("tnt-destroys-everything", false);
        if (this.destroyEverything) {
            plugin.getLogger().warning("¡ADVERTENCIA! 'tnt-destroys-everything' está ACTIVADO: "
                    + "la TNT DE USO PESADO destruirá TODO dentro de las protecciones, incluidos cofres y objetos guardados.");
        }
    }

    /** Llamado cuando un jugador coloca un bloque de TNT especial en el mundo. */
    public void markHeavyTntBlock(Location loc) {
        heavyTntBlocks.add(key(loc));
    }

    @EventHandler
    public void onPrime(TNTPrimeEvent event) {
        String k = key(event.getBlock().getLocation());
        if (heavyTntBlocks.remove(k)) {
            Location loc = event.getBlock().getLocation();
            pendingHeavy.put(k, new PendingHeavy(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), System.currentTimeMillis()));
        }
    }

    @EventHandler
    public void onDispense(BlockDispenseEvent event) {
        if (event.getBlock().getType() != Material.DISPENSER) return;

        ItemStack item = event.getItem();
        if (!plugin.getCustomItems().isHeavyTnt(item)) return;
        if (!(event.getBlock().getBlockData() instanceof Dispenser dispenserData)) return;

        BlockFace facing = dispenserData.getFacing();
        Location target = event.getBlock().getLocation().add(facing.getModX(), facing.getModY(), facing.getModZ());
        String k = key(target);
        pendingHeavy.put(k, new PendingHeavy(target.getWorld().getName(), target.getBlockX(), target.getBlockY(), target.getBlockZ(), System.currentTimeMillis()));
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        boolean isHeavy = event.getEntity() instanceof TNTPrimed && consumeMatch(event.getLocation());

        Iterator<Block> it = event.blockList().iterator();
        while (it.hasNext()) {
            Block block = it.next();
            Claim claim = plugin.getClaimManager().getClaimAt(block.getLocation());
            if (claim == null) continue; // fuera de cualquier protección: comportamiento vanilla normal

            if (!isHeavy) {
                // Explosión normal (TNT vanilla, creeper, etc.) dentro de una protección: no destruye nada.
                it.remove();
                continue;
            }

            if (destroyEverything) {
                // Modo sin restricciones activado en config: la TNT especial destruye todo por igual.
                continue;
            }

            // Explosión de TNT especial dentro de una protección: protege solo los bloques de utilidad.
            if (isProtectedMaterial(block.getType())) {
                it.remove();
            }
        }
    }

    private boolean isProtectedMaterial(Material material) {
        return protectedMaterials.contains(material) || material.name().endsWith("SHULKER_BOX");
    }

    private boolean consumeMatch(Location explosionLoc) {
        if (explosionLoc.getWorld() == null) return false;
        long now = System.currentTimeMillis();
        String bestKey = null;
        double bestDistSq = Double.MAX_VALUE;

        for (Map.Entry<String, PendingHeavy> entry : pendingHeavy.entrySet()) {
            PendingHeavy p = entry.getValue();
            if (!p.world().equals(explosionLoc.getWorld().getName())) continue;
            if (now - p.timestamp() > MATCH_WINDOW_MILLIS) continue;

            double dx = p.x() - explosionLoc.getX();
            double dy = p.y() - explosionLoc.getY();
            double dz = p.z() - explosionLoc.getZ();
            double distSq = dx * dx + dy * dy + dz * dz;

            if (distSq <= (double) MATCH_RADIUS * MATCH_RADIUS && distSq < bestDistSq) {
                bestDistSq = distSq;
                bestKey = entry.getKey();
            }
        }

        if (bestKey != null) {
            pendingHeavy.remove(bestKey);
            return true;
        }
        return false;
    }

    private void cleanupStaleEntries() {
        long now = System.currentTimeMillis();
        pendingHeavy.entrySet().removeIf(e -> now - e.getValue().timestamp() > MATCH_WINDOW_MILLIS);
    }

    private String key(Location loc) {
        return loc.getWorld().getName() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ();
    }
}
