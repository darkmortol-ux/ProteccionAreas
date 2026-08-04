package com.miplugin.protecciones.lives;

import com.miplugin.protecciones.ProteccionesPlugin;
import org.bukkit.Location;
import org.bukkit.Statistic;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LivesManager {

    private final ProteccionesPlugin plugin;
    private final File file;
    private final Map<UUID, Integer> deaths = new HashMap<>();
    private final Map<UUID, Integer> bonusLives = new HashMap<>(); // vidas extra: tiempo jugado, compras, admin
    private final Map<UUID, Integer> playtimeMilestonesAwarded = new HashMap<>();
    private final Map<UUID, int[]> firstClaimCoords = new HashMap<>(); // [x, z]
    private final Map<UUID, String> firstClaimWorld = new HashMap<>();

    public LivesManager(ProteccionesPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "vidas.yml");
    }

    public int getStartingLives() {
        return plugin.getConfig().getInt("starting-lives", 15);
    }

    public int getDeaths(UUID player) {
        return deaths.getOrDefault(player, 0);
    }

    public int getBonusLives(UUID player) {
        return bonusLives.getOrDefault(player, 0);
    }

    public int getRemainingLives(UUID player) {
        return Math.max(0, getStartingLives() + getBonusLives(player) - getDeaths(player));
    }

    public boolean isRaided(UUID player) {
        return getRemainingLives(player) <= 0;
    }

    /** Suma (o resta, si amount es negativo) vidas extra permanentes a un jugador. */
    public void addBonusLives(UUID player, int amount) {
        bonusLives.merge(player, amount, Integer::sum);
        save();
    }

    /** Registra una muerte y devuelve true si esta muerte fue la que dejó al jugador en 0 vidas (recién raideado). */
    public boolean registerDeath(UUID player) {
        int before = getRemainingLives(player);
        deaths.merge(player, 1, Integer::sum);
        save();
        int after = getRemainingLives(player);
        return before > 0 && after <= 0;
    }

    /**
     * Revisa si el jugador ya acumuló suficiente tiempo jugado para ganar vidas extra
     * automáticamente (config: lives-per-days-played). Se debe llamar periódicamente
     * para jugadores conectados (el tiempo jugado solo se acumula estando en línea).
     * Devuelve la cantidad de vidas nuevas otorgadas en esta revisión (0 si ninguna).
     */
    public int checkPlaytimeMilestone(Player player) {
        if (!plugin.getConfig().getBoolean("lives-per-days-played.enabled", true)) return 0;

        int intervalDays = Math.max(1, plugin.getConfig().getInt("lives-per-days-played.days", 500));
        long ticksPlayed = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        int daysPlayed = (int) (ticksPlayed / 24000L);
        int milestonesEarned = daysPlayed / intervalDays;

        UUID id = player.getUniqueId();
        int alreadyAwarded = playtimeMilestonesAwarded.getOrDefault(id, 0);
        if (milestonesEarned <= alreadyAwarded) return 0;

        int newLives = milestonesEarned - alreadyAwarded;
        playtimeMilestonesAwarded.put(id, milestonesEarned);
        addBonusLives(id, newLives);
        return newLives;
    }

    /** Registra la ubicación de la PRIMERA protección que el jugador haya creado en su vida (no se sobreescribe). */
    public void registerFirstClaimIfAbsent(UUID player, Location loc) {
        if (firstClaimCoords.containsKey(player)) return;
        firstClaimCoords.put(player, new int[]{loc.getBlockX(), loc.getBlockZ()});
        firstClaimWorld.put(player, loc.getWorld() != null ? loc.getWorld().getName() : "world");
        save();
    }

    public int[] getFirstClaimCoords(UUID player) {
        return firstClaimCoords.get(player);
    }

    public void load() {
        deaths.clear();
        bonusLives.clear();
        playtimeMilestonesAwarded.clear();
        firstClaimCoords.clear();
        firstClaimWorld.clear();
        if (!file.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfig(file);
        var section = yaml.getConfigurationSection("jugadores");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                int deathCount = section.getInt(key + ".muertes", 0);
                deaths.put(id, deathCount);

                int bonus = section.getInt(key + ".vidasExtra", 0);
                bonusLives.put(id, bonus);

                int milestones = section.getInt(key + ".hitosTiempoJugado", 0);
                playtimeMilestonesAwarded.put(id, milestones);

                if (section.isSet(key + ".primeraProteccion.x")) {
                    int x = section.getInt(key + ".primeraProteccion.x");
                    int z = section.getInt(key + ".primeraProteccion.z");
                    String world = section.getString(key + ".primeraProteccion.world", "world");
                    firstClaimCoords.put(id, new int[]{x, z});
                    firstClaimWorld.put(id, world);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (UUID id : deaths.keySet()) {
            yaml.set("jugadores." + id + ".muertes", deaths.get(id));
        }
        for (UUID id : bonusLives.keySet()) {
            yaml.set("jugadores." + id + ".vidasExtra", bonusLives.get(id));
        }
        for (UUID id : playtimeMilestonesAwarded.keySet()) {
            yaml.set("jugadores." + id + ".hitosTiempoJugado", playtimeMilestonesAwarded.get(id));
        }
        for (UUID id : firstClaimCoords.keySet()) {
            int[] coords = firstClaimCoords.get(id);
            yaml.set("jugadores." + id + ".primeraProteccion.x", coords[0]);
            yaml.set("jugadores." + id + ".primeraProteccion.z", coords[1]);
            yaml.set("jugadores." + id + ".primeraProteccion.world", firstClaimWorld.get(id));
        }

        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("No se pudo guardar vidas.yml: " + e.getMessage());
        }
    }
}

