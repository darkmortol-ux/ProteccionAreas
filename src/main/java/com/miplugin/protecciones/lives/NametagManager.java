package com.miplugin.protecciones.lives;

import com.miplugin.protecciones.ProteccionesPlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class NametagManager {

    private final ProteccionesPlugin plugin;

    public NametagManager(ProteccionesPlugin plugin) {
        this.plugin = plugin;
    }

    /** Actualiza el texto sobre la cabeza del jugador según sus vidas restantes o su estado RAID. */
    public void updateTag(Player player) {
        Scoreboard board = plugin.getServer().getScoreboardManager().getMainScoreboard();
        String teamName = teamNameFor(player);

        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
        }
        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }

        var lives = plugin.getLivesManager();
        String text;
        if (lives.isRaided(player.getUniqueId())) {
            int[] coords = lives.getFirstClaimCoords(player.getUniqueId());
            String x = coords != null ? String.valueOf(coords[0]) : "?";
            String z = coords != null ? String.valueOf(coords[1]) : "?";
            text = plugin.getConfig().getString("raid-tag-format", "&c[RAID] &7(%x%, %z%)")
                    .replace("%x%", x)
                    .replace("%z%", z);
        } else {
            int remaining = lives.getRemainingLives(player.getUniqueId());
            text = plugin.getConfig().getString("lives-tag-format", "&7[Vidas: %vidas%]")
                    .replace("%vidas%", String.valueOf(remaining));
        }

        text = ChatColor.translateAlternateColorCodes('&', text);
        // El sufijo de un equipo tiene un límite práctico de longitud; se recorta por seguridad.
        if (text.length() > 64) {
            text = text.substring(0, 64);
        }
        team.setSuffix(" " + text);
    }

    /** Quita al jugador de cualquier equipo propio de este plugin (ej. al desconectarse, opcional). */
    public void clearTag(Player player) {
        Scoreboard board = plugin.getServer().getScoreboardManager().getMainScoreboard();
        Team team = board.getTeam(teamNameFor(player));
        if (team != null) {
            team.removeEntry(player.getName());
        }
    }

    private String teamNameFor(Player player) {
        // Los nombres de equipo están limitados a 16 caracteres en versiones antiguas del protocolo;
        // usamos un prefijo corto + el nombre del jugador recortado por seguridad.
        String base = "prot_" + player.getName();
        return base.length() > 16 ? base.substring(0, 16) : base;
    }
}
