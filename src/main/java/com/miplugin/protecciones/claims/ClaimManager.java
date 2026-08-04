package com.miplugin.protecciones.claims;

import com.miplugin.protecciones.ProteccionesPlugin;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClaimManager {

    private final ProteccionesPlugin plugin;
    private final Map<UUID, Claim> claimsById = new LinkedHashMap<>();
    private final File file;

    public ClaimManager(ProteccionesPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "claims.yml");
    }

    public List<Claim> getAll() {
        return new ArrayList<>(claimsById.values());
    }

    public List<Claim> getClaimsOf(UUID owner) {
        List<Claim> result = new ArrayList<>();
        for (Claim c : claimsById.values()) {
            if (c.getOwner().equals(owner)) result.add(c);
        }
        return result;
    }

    /** Devuelve la protección que contiene esa ubicación, o null si no hay ninguna. */
    public Claim getClaimAt(Location loc) {
        for (Claim c : claimsById.values()) {
            if (c.contains(loc)) return c;
        }
        return null;
    }

    public boolean overlapsAny(String world, int centerX, int centerZ, int sizeBlocks) {
        for (Claim c : claimsById.values()) {
            if (c.overlaps(world, centerX, centerZ, sizeBlocks)) return true;
        }
        return false;
    }

    public Claim createClaim(UUID owner, String world, int centerX, int centerZ, int sizeBlocks) {
        Claim claim = new Claim(UUID.randomUUID(), owner, world, centerX, centerZ, sizeBlocks, System.currentTimeMillis());
        claimsById.put(claim.getId(), claim);
        save();
        return claim;
    }

    public void deleteClaim(Claim claim) {
        claimsById.remove(claim.getId());
        save();
    }

    public void load() {
        claimsById.clear();
        if (!file.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfig(file);
        var section = yaml.getConfigurationSection("claims");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            var c = section.getConfigurationSection(key);
            if (c == null) continue;
            try {
                UUID id = UUID.fromString(key);
                UUID owner = UUID.fromString(c.getString("owner"));
                String world = c.getString("world");
                int centerX = c.getInt("centerX");
                int centerZ = c.getInt("centerZ");
                int size = c.getInt("size");
                long createdAt = c.getLong("createdAt");

                Claim claim = new Claim(id, owner, world, centerX, centerZ, size, createdAt);
                for (String trustedStr : c.getStringList("trusted")) {
                    try {
                        claim.getTrusted().add(UUID.fromString(trustedStr));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                claimsById.put(id, claim);
            } catch (Exception e) {
                plugin.getLogger().warning("No se pudo cargar la protección " + key + ": " + e.getMessage());
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Claim claim : claimsById.values()) {
            String path = "claims." + claim.getId();
            int centerX = (claim.getMinX() + claim.getMaxX() + 1) / 2;
            int centerZ = (claim.getMinZ() + claim.getMaxZ() + 1) / 2;

            yaml.set(path + ".owner", claim.getOwner().toString());
            yaml.set(path + ".world", claim.getWorld());
            yaml.set(path + ".centerX", centerX);
            yaml.set(path + ".centerZ", centerZ);
            yaml.set(path + ".size", claim.getSizeBlocks());
            yaml.set(path + ".createdAt", claim.getCreatedAt());

            List<String> trustedList = new ArrayList<>();
            for (UUID u : claim.getTrusted()) trustedList.add(u.toString());
            yaml.set(path + ".trusted", trustedList);
        }

        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("No se pudo guardar claims.yml: " + e.getMessage());
        }
    }
}
