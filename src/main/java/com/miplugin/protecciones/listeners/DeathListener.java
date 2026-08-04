package com.miplugin.protecciones.listeners;

import com.miplugin.protecciones.ProteccionesPlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class DeathListener implements Listener {

    private final ProteccionesPlugin plugin;

    public DeathListener(ProteccionesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getNametagManager().updateTag(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        boolean justRaided = plugin.getLivesManager().registerDeath(player.getUniqueId());

        int remaining = plugin.getLivesManager().getRemainingLives(player.getUniqueId());
        if (!justRaided && remaining > 0) {
            String msg = plugin.getConfig().getString("death-warning", "&cTe quedan %vidas% vidas.")
                    .replace("%vidas%", String.valueOf(remaining));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
        }

        if (justRaided) {
            String broadcast = plugin.getConfig().getString("raid-triggered-broadcast", "&4[RAID] &c%jugador% se quedó sin vidas.")
                    .replace("%jugador%", player.getName());
            plugin.getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', broadcast));
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        // Se actualiza en el siguiente tick para asegurar que el jugador ya esté completamente respawneado.
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getNametagManager().updateTag(event.getPlayer()));
    }
}
