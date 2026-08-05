package com.miplugin.protecciones.claims;

import com.miplugin.protecciones.ProteccionesPlugin;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * Calcula cuántas protecciones puede tener un jugador según su permiso/rango,
 * usando la lista "claim-limits.tiers" de config.yml. Si el jugador tiene varios
 * permisos, se queda con el límite MÁS ALTO de todos los que le apliquen.
 */
public class ClaimLimitService {

    private final ProteccionesPlugin plugin;

    public ClaimLimitService(ProteccionesPlugin plugin) {
        this.plugin = plugin;
    }

    /** Límite de protecciones para este jugador. Los admins (proteccion.admin) no tienen límite. */
    public int getLimit(Player player) {
        if (player.hasPermission("proteccion.admin")) {
            return Integer.MAX_VALUE;
        }

        var config = plugin.getConfig();
        int limit = config.getInt("claim-limits.default", 2);

        List<Map<?, ?>> tiers = config.getMapList("claim-limits.tiers");
        for (Map<?, ?> tier : tiers) {
            Object permObj = tier.get("permission");
            Object limitObj = tier.get("limit");
            if (permObj == null || limitObj == null) continue;
            if (!(limitObj instanceof Number)) continue;

            String permission = permObj.toString();
            int tierLimit = ((Number) limitObj).intValue();

            if (player.hasPermission(permission) && tierLimit > limit) {
                limit = tierLimit;
            }
        }
        return limit;
    }

    /** Cuántas protecciones le quedan disponibles al jugador (límite - actuales, mínimo 0). */
    public int getRemaining(Player player) {
        int limit = getLimit(player);
        if (limit == Integer.MAX_VALUE) return Integer.MAX_VALUE;
        int current = plugin.getClaimManager().getClaimsOf(player.getUniqueId()).size();
        return Math.max(0, limit - current);
    }
}
