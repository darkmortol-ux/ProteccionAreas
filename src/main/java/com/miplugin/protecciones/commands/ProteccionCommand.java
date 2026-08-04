package com.miplugin.protecciones.commands;

import com.miplugin.protecciones.ProteccionesPlugin;
import com.miplugin.protecciones.claims.Claim;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProteccionCommand implements CommandExecutor, TabCompleter {

    private final ProteccionesPlugin plugin;

    public ProteccionCommand(ProteccionesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Este comando solo puede usarse en el juego.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "comprar" -> handleComprar(player, args);
            case "tp" -> handleTp(player);
            case "trust" -> handleTrust(player, args, true);
            case "untrust" -> handleTrust(player, args, false);
            case "info" -> handleInfo(player);
            case "listar" -> handleListar(player);
            case "eliminar" -> handleEliminar(player);
            default -> sendHelp(player);
        }
        return true;
    }

    private void handleComprar(Player player, String[] args) {
        if (args.length < 2 || (!args[1].equals("16") && !args[1].equals("32"))) {
            player.sendMessage(ChatColor.RED + "Uso: /proteccion comprar <16|32>");
            return;
        }

        boolean small = args[1].equals("16");
        String path = small ? "sizes.small" : "sizes.large";
        int size = plugin.getConfig().getInt(path + ".blocks", small ? 16 : 32);
        double price = plugin.getConfig().getDouble(path + ".price", small ? 500 : 1500);

        if (!plugin.getEconomyManager().isEnabled()) {
            player.sendMessage(ChatColor.RED + "La economía (Vault) no está disponible en este servidor. No se puede comprar.");
            return;
        }

        if (!plugin.getEconomyManager().has(player, price)) {
            player.sendMessage(ChatColor.RED + "No tienes suficiente dinero. Necesitas "
                    + plugin.getEconomyManager().format(price) + ".");
            return;
        }

        if (!plugin.getEconomyManager().withdraw(player, price)) {
            player.sendMessage(ChatColor.RED + "Ocurrió un error al cobrar. Intenta de nuevo.");
            return;
        }

        ItemStack wand = plugin.getClaimWand().create(size);
        var leftover = player.getInventory().addItem(wand);
        if (!leftover.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), wand);
            player.sendMessage(ChatColor.YELLOW + "Tu inventario estaba lleno, el palo se dejó caer en el suelo.");
        }

        player.sendMessage(ChatColor.GREEN + "Compraste una protección de " + size + "x" + size
                + " por " + plugin.getEconomyManager().format(price) + ". "
                + ChatColor.GRAY + "Haz click derecho en un bloque para colocarla.");
    }

    private void handleTp(Player player) {
        List<Claim> claims = plugin.getClaimManager().getClaimsOf(player.getUniqueId());
        if (claims.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No tienes ninguna protección.");
            return;
        }
        Claim claim = claims.get(0);
        var world = plugin.getServer().getWorld(claim.getWorld());
        if (world == null) {
            player.sendMessage(ChatColor.RED + "El mundo de tu protección ya no existe.");
            return;
        }
        Location loc = claim.getCenterLocation(world);
        player.teleport(loc);
        player.sendMessage(ChatColor.GREEN + "Teletransportado a tu protección.");
    }

    private void handleTrust(Player player, String[] args, boolean trust) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Uso: /proteccion " + (trust ? "trust" : "untrust") + " <jugador>");
            return;
        }
        Claim claim = plugin.getClaimManager().getClaimAt(player.getLocation());
        if (claim == null) {
            player.sendMessage(ChatColor.RED + "Debes estar dentro de tu protección para usar este comando.");
            return;
        }
        if (!claim.getOwner().equals(player.getUniqueId()) && !player.hasPermission("proteccion.admin")) {
            player.sendMessage(ChatColor.RED + "Solo el dueño de esta protección puede hacer eso.");
            return;
        }

        OfflinePlayer target = plugin.getServer().getOfflinePlayer(args[1]);
        UUID targetId = target.getUniqueId();

        if (trust) {
            claim.getTrusted().add(targetId);
            player.sendMessage(ChatColor.GREEN + args[1] + " ahora puede usar cofres y bloques dentro de tu protección.");
        } else {
            claim.getTrusted().remove(targetId);
            player.sendMessage(ChatColor.YELLOW + args[1] + " ya no tiene acceso a tu protección.");
        }
        plugin.getClaimManager().save();
    }

    private void handleInfo(Player player) {
        Claim claim = plugin.getClaimManager().getClaimAt(player.getLocation());
        if (claim == null) {
            player.sendMessage(ChatColor.RED + "No estás dentro de ninguna protección.");
            return;
        }
        OfflinePlayer owner = plugin.getServer().getOfflinePlayer(claim.getOwner());
        player.sendMessage(ChatColor.GOLD + "=== Información de la protección ===");
        player.sendMessage(ChatColor.YELLOW + "Dueño: " + ChatColor.WHITE + owner.getName());
        player.sendMessage(ChatColor.YELLOW + "Tamaño: " + ChatColor.WHITE + claim.getSizeBlocks() + "x" + claim.getSizeBlocks());
        player.sendMessage(ChatColor.YELLOW + "Límites: " + ChatColor.WHITE
                + "(" + claim.getMinX() + ", " + claim.getMinZ() + ") a (" + claim.getMaxX() + ", " + claim.getMaxZ() + ")");
        boolean raided = plugin.getLivesManager().isRaided(claim.getOwner());
        player.sendMessage(ChatColor.YELLOW + "Estado: " + (raided
                ? ChatColor.RED + "RAIDEABLE (el dueño se quedó sin vidas)"
                : ChatColor.GREEN + "Protegida"));
    }

    private void handleListar(Player player) {
        List<Claim> claims = plugin.getClaimManager().getClaimsOf(player.getUniqueId());
        if (claims.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No tienes ninguna protección.");
            return;
        }
        player.sendMessage(ChatColor.GOLD + "=== Tus protecciones (" + claims.size() + ") ===");
        for (Claim c : claims) {
            player.sendMessage(ChatColor.YELLOW + "- " + c.getSizeBlocks() + "x" + c.getSizeBlocks()
                    + ChatColor.GRAY + " en " + c.getWorld() + " (" + c.getMinX() + ", " + c.getMinZ() + ")");
        }
    }

    private void handleEliminar(Player player) {
        Claim claim = plugin.getClaimManager().getClaimAt(player.getLocation());
        if (claim == null) {
            player.sendMessage(ChatColor.RED + "No estás dentro de ninguna protección.");
            return;
        }
        if (!claim.getOwner().equals(player.getUniqueId()) && !player.hasPermission("proteccion.admin")) {
            player.sendMessage(ChatColor.RED + "Solo el dueño puede eliminar esta protección.");
            return;
        }
        plugin.getClaimManager().deleteClaim(claim);
        player.sendMessage(ChatColor.GREEN + "Protección eliminada.");
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Comandos de Protecciones ===");
        player.sendMessage(ChatColor.YELLOW + "/proteccion comprar <16|32>" + ChatColor.WHITE + " - Comprar un palo de protección");
        player.sendMessage(ChatColor.YELLOW + "/proteccion tp" + ChatColor.WHITE + " - Teletransportarte a tu protección");
        player.sendMessage(ChatColor.YELLOW + "/proteccion trust <jugador>" + ChatColor.WHITE + " - Dar acceso a un jugador");
        player.sendMessage(ChatColor.YELLOW + "/proteccion untrust <jugador>" + ChatColor.WHITE + " - Quitar acceso");
        player.sendMessage(ChatColor.YELLOW + "/proteccion info" + ChatColor.WHITE + " - Ver info de la protección donde estás parado");
        player.sendMessage(ChatColor.YELLOW + "/proteccion listar" + ChatColor.WHITE + " - Ver todas tus protecciones");
        player.sendMessage(ChatColor.YELLOW + "/proteccion eliminar" + ChatColor.WHITE + " - Eliminar la protección donde estás parado");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.addAll(List.of("comprar", "tp", "trust", "untrust", "info", "listar", "eliminar"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("comprar")) {
            options.addAll(List.of("16", "32"));
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("trust") || args[0].equalsIgnoreCase("untrust"))) {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                options.add(p.getName());
            }
        }
        return options;
    }
}
