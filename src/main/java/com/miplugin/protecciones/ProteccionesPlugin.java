package com.miplugin.protecciones;

import com.miplugin.protecciones.claims.ClaimManager;
import com.miplugin.protecciones.commands.ProteccionCommand;
import com.miplugin.protecciones.commands.VidasCommand;
import com.miplugin.protecciones.economy.EconomyManager;
import com.miplugin.protecciones.integration.WorldGuardHook;
import com.miplugin.protecciones.items.ClaimWand;
import com.miplugin.protecciones.items.CustomItems;
import com.miplugin.protecciones.items.CustomRecipes;
import com.miplugin.protecciones.listeners.DeathListener;
import com.miplugin.protecciones.listeners.ExplosionListener;
import com.miplugin.protecciones.listeners.ProtectionListener;
import com.miplugin.protecciones.listeners.WandListener;
import com.miplugin.protecciones.lives.LivesManager;
import com.miplugin.protecciones.lives.NametagManager;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class ProteccionesPlugin extends JavaPlugin {

    private ClaimManager claimManager;
    private LivesManager livesManager;
    private NametagManager nametagManager;
    private EconomyManager economyManager;
    private CustomItems customItems;
    private ClaimWand claimWand;
    private ExplosionListener explosionListener;
    private WorldGuardHook worldGuardHook;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.claimManager = new ClaimManager(this);
        this.claimManager.load();

        this.livesManager = new LivesManager(this);
        this.livesManager.load();

        this.nametagManager = new NametagManager(this);
        this.economyManager = new EconomyManager(this);
        this.customItems = new CustomItems(this);
        this.claimWand = new ClaimWand(this);
        this.explosionListener = new ExplosionListener(this);

        CustomRecipes.registerAll(this, customItems);

        getServer().getPluginManager().registerEvents(new WandListener(this), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(explosionListener, this);
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);

        // Conecta con Vault (y con WorldGuard, si está instalado) una vez que todos los plugins ya cargaron.
        getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onServerLoad(ServerLoadEvent event) {
                economyManager.setup();
                setupWorldGuardHook();
            }
        }, this);

        var proteccionCmd = getCommand("proteccion");
        if (proteccionCmd != null) {
            ProteccionCommand executor = new ProteccionCommand(this);
            proteccionCmd.setExecutor(executor);
            proteccionCmd.setTabCompleter(executor);
        }
        var vidasCmd = getCommand("vidas");
        if (vidasCmd != null) {
            vidasCmd.setExecutor(new VidasCommand(this));
        }

        // Refresca las etiquetas de vidas/RAID de todos los jugadores conectados cada 5 segundos,
        // por si un admin modificó algo o el jugador se unió antes de que cargara todo.
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (var player : getServer().getOnlinePlayers()) {
                nametagManager.updateTag(player);
            }
        }, 100L, 100L);

        // Revisa cada minuto si algún jugador conectado ya acumuló suficiente tiempo
        // jugado para ganar una vida extra automáticamente (config: lives-per-days-played).
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (var player : getServer().getOnlinePlayers()) {
                int gained = livesManager.checkPlaytimeMilestone(player);
                if (gained > 0) {
                    nametagManager.updateTag(player);
                    player.sendMessage(org.bukkit.ChatColor.GREEN + "¡Has ganado " + gained
                            + " vida(s) extra por tiempo jugado!");
                }
            }
        }, 1200L, 1200L);

        getLogger().info("ProteccionesAreas habilitado correctamente.");
    }

    @Override
    public void onDisable() {
        if (claimManager != null) claimManager.save();
        if (livesManager != null) livesManager.save();
        getLogger().info("ProteccionesAreas deshabilitado.");
    }

    private void setupWorldGuardHook() {
        var wgPlugin = getServer().getPluginManager().getPlugin("WorldGuard");
        if (!WorldGuardHook.isPresent(wgPlugin)) {
            worldGuardHook = null;
            return;
        }
        try {
            worldGuardHook = new WorldGuardHook();
            getLogger().info("Integración con WorldGuard activada: se respetarán sus regiones al crear protecciones.");
        } catch (Throwable t) {
            worldGuardHook = null;
            getLogger().warning("WorldGuard está instalado pero no se pudo conectar (posible versión incompatible): " + t.getMessage());
        }
    }

    public WorldGuardHook getWorldGuardHook() {
        return worldGuardHook;
    }

    public ClaimManager getClaimManager() {
        return claimManager;
    }

    public LivesManager getLivesManager() {
        return livesManager;
    }

    public NametagManager getNametagManager() {
        return nametagManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public CustomItems getCustomItems() {
        return customItems;
    }

    public ClaimWand getClaimWand() {
        return claimWand;
    }

    public ExplosionListener getExplosionListener() {
        return explosionListener;
    }
}
