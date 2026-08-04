package com.miplugin.protecciones.commands;

import com.miplugin.protecciones.ProteccionesPlugin;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class VidasCommand implements CommandExecutor {

    private final ProteccionesPlugin plugin;

    public VidasCommand(ProteccionesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("comprar")) {
            return handleComprar(sender);
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("dar")) {
            return handleDar(sender, args);
        }

        return handleVer(sender, args);
    }

    private boolean handleComprar(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Solo un jugador puede comprar vidas.");
            return true;
        }
        if (!plugin.getConfig().getBoolean("lives-purchase.enabled", true)) {
            player.sendMessage(ChatColor.RED + "La compra de vidas está desactivada en este servidor.");
            return true;
        }

        double price = plugin.getConfig().getDouble("lives-purchase.price", 50000);

        if (!plugin.getEconomyManager().isEnabled()) {
            player.sendMessage(ChatColor.RED + "La economía (Vault) no está disponible en este servidor.");
            return true;
        }
        if (!plugin.getEconomyManager().has(player, price)) {
            player.sendMessage(ChatColor.RED + "No tienes suficiente dinero. Necesitas "
                    + plugin.getEconomyManager().format(price) + ".");
            return true;
        }
        if (!plugin.getEconomyManager().withdraw(player, price)) {
            player.sendMessage(ChatColor.RED + "Ocurrió un error al cobrar. Intenta de nuevo.");
            return true;
        }

        plugin.getLivesManager().addBonusLives(player.getUniqueId(), 1);
        plugin.getNametagManager().updateTag(player);

        int remaining = plugin.getLivesManager().getRemainingLives(player.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "Compraste 1 vida extra por "
                + plugin.getEconomyManager().format(price) + ". Ahora tienes " + remaining + " vidas.");
        return true;
    }

    private boolean handleDar(CommandSender sender, String[] args) {
        if (!sender.hasPermission("proteccion.admin")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso para dar vidas.");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Uso: /vidas dar <jugador> <cantidad>");
            return true;
        }

        OfflinePlayer target = plugin.getServer().getOfflinePlayer(args[1]);
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "La cantidad debe ser un número entero (puede ser negativo).");
            return true;
        }

        plugin.getLivesManager().addBonusLives(target.getUniqueId(), amount);
        if (target.isOnline() && target.getPlayer() != null) {
            plugin.getNametagManager().updateTag(target.getPlayer());
        }

        int remaining = plugin.getLivesManager().getRemainingLives(target.getUniqueId());
        sender.sendMessage(ChatColor.GREEN + (amount >= 0 ? "Diste " : "Quitaste ") + Math.abs(amount)
                + " vida(s) " + (amount >= 0 ? "a " : "a ") + target.getName()
                + ". Ahora tiene " + remaining + " vidas.");
        return true;
    }

    private boolean handleVer(CommandSender sender, String[] args) {
        OfflinePlayer target;

        if (args.length >= 1) {
            if (!sender.hasPermission("proteccion.admin")) {
                sender.sendMessage(ChatColor.RED + "No tienes permiso para ver las vidas de otros jugadores.");
                return true;
            }
            target = plugin.getServer().getOfflinePlayer(args[0]);
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(ChatColor.RED + "Uso: /vidas <jugador>");
            return true;
        }

        UUID id = target.getUniqueId();
        int remaining = plugin.getLivesManager().getRemainingLives(id);
        int deaths = plugin.getLivesManager().getDeaths(id);
        int bonus = plugin.getLivesManager().getBonusLives(id);
        boolean raided = plugin.getLivesManager().isRaided(id);

        sender.sendMessage(ChatColor.GOLD + "=== Vidas de " + target.getName() + " ===");
        sender.sendMessage(ChatColor.YELLOW + "Vidas restantes: " + ChatColor.WHITE + remaining);
        sender.sendMessage(ChatColor.YELLOW + "Vidas base: " + ChatColor.WHITE + plugin.getLivesManager().getStartingLives()
                + ChatColor.YELLOW + "  |  Vidas extra: " + ChatColor.WHITE + bonus);
        sender.sendMessage(ChatColor.YELLOW + "Muertes totales: " + ChatColor.WHITE + deaths);
        sender.sendMessage(ChatColor.YELLOW + "Estado: " + (raided
                ? ChatColor.RED + "RAIDEADO"
                : ChatColor.GREEN + "Normal"));
        return true;
    }
}
