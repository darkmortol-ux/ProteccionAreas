package com.miplugin.protecciones.economy;

import com.miplugin.protecciones.ProteccionesPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {

    private final ProteccionesPlugin plugin;
    private Economy economy;
    private boolean enabled = false;

    public EconomyManager(ProteccionesPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault no está instalado. La compra de protecciones quedará desactivada.");
            enabled = false;
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer()
                .getServicesManager()
                .getRegistration(Economy.class);

        if (rsp == null) {
            plugin.getLogger().warning("Vault está instalado pero no hay ningún plugin de economía registrado. La compra de protecciones quedará desactivada.");
            enabled = false;
            return false;
        }

        economy = rsp.getProvider();
        enabled = true;
        plugin.getLogger().info("Conectado correctamente con Vault (" + economy.getName() + ").");
        return true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public double getBalance(Player player) {
        if (!enabled || economy == null) return 0;
        return economy.getBalance(player);
    }

    public boolean has(Player player, double amount) {
        return enabled && economy != null && economy.has(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        if (!enabled || economy == null) return false;
        try {
            return economy.withdrawPlayer(player, amount).transactionSuccess();
        } catch (Exception e) {
            plugin.getLogger().warning("Error al cobrar con Vault a " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    public String format(double amount) {
        if (enabled && economy != null) {
            try {
                return economy.format(amount);
            } catch (Exception ignored) {
            }
        }
        return "$" + amount;
    }
}
