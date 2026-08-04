package com.miplugin.protecciones.integration;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Puente opcional con WorldGuard. Esta clase SOLO se instancia (y por lo tanto solo
 * se carga en la JVM) si el plugin "WorldGuard" está presente en el servidor — así,
 * en servidores sin WorldGuard, estas clases nunca se referencian y no hay riesgo
 * de errores de classloading.
 *
 * Objetivo: antes de crear una protección, verificamos que el jugador tenga permiso
 * de construir (según el flag BUILD de WorldGuard) en el área elegida, para evitar que
 * el sistema de protecciones de este plugin choque con regiones ya definidas por
 * WorldGuard (por ejemplo, el spawn del servidor).
 */
public class WorldGuardHook {

    public WorldGuardHook() {
        // El solo hecho de instanciar esta clase ya fuerza a la JVM a cargar las
        // clases de WorldGuard referenciadas arriba; si la versión instalada en el
        // servidor no es compatible, fallará aquí con un error claro en consola en
        // vez de fallar silenciosamente más adelante durante el juego.
    }

    /** Verifica si el jugador puede construir (flag BUILD) en esa ubicación según WorldGuard. */
    public boolean canBuild(Player player, Location location) {
        try {
            LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
            RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
            return query.testState(BukkitAdapter.adapt(location), localPlayer, Flags.BUILD);
        } catch (Throwable t) {
            // Si la integración falla por cualquier motivo, no bloqueamos al jugador
            // por un problema de nuestra parte; simplemente no aplicamos el chequeo extra.
            return true;
        }
    }

    public static boolean isPresent(Plugin worldGuardPlugin) {
        return worldGuardPlugin != null && worldGuardPlugin.isEnabled();
    }
}
