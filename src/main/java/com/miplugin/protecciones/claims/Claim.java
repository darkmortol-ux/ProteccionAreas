package com.miplugin.protecciones.claims;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Representa una zona protegida cuadrada, plana en X/Z y sin límite en Y
 * (protege toda la columna, del fondo del mundo hasta la altura máxima).
 */
public class Claim {

    private final UUID id;
    private final UUID owner;
    private final String world;
    private final int minX;
    private final int maxX;
    private final int minZ;
    private final int maxZ;
    private final int sizeBlocks; // 16 o 32
    private final Set<UUID> trusted = new LinkedHashSet<>();
    private final long createdAt;

    public Claim(UUID id, UUID owner, String world, int centerX, int centerZ, int sizeBlocks, long createdAt) {
        this.id = id;
        this.owner = owner;
        this.world = world;
        int half = sizeBlocks / 2;
        this.minX = centerX - half;
        this.maxX = centerX + half - 1;
        this.minZ = centerZ - half;
        this.maxZ = centerZ + half - 1;
        this.sizeBlocks = sizeBlocks;
        this.createdAt = createdAt;
    }

    public boolean contains(Location loc) {
        if (loc.getWorld() == null || !loc.getWorld().getName().equals(world)) return false;
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    /** Chequea si esta protección se solaparía con un área centrada en (centerX, centerZ) del mismo tamaño dado. */
    public boolean overlaps(String worldName, int centerX, int centerZ, int sizeBlocks) {
        if (!this.world.equals(worldName)) return false;
        int half = sizeBlocks / 2;
        int oMinX = centerX - half;
        int oMaxX = centerX + half - 1;
        int oMinZ = centerZ - half;
        int oMaxZ = centerZ + half - 1;
        return minX <= oMaxX && maxX >= oMinX && minZ <= oMaxZ && maxZ >= oMinZ;
    }

    public Location getCenterLocation(World bukkitWorld) {
        int centerX = (minX + maxX + 1) / 2;
        int centerZ = (minZ + maxZ + 1) / 2;
        int y = bukkitWorld != null ? bukkitWorld.getHighestBlockYAt(centerX, centerZ) + 1 : 100;
        return new Location(bukkitWorld, centerX + 0.5, y, centerZ + 0.5);
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwner() {
        return owner;
    }

    public String getWorld() {
        return world;
    }

    public int getMinX() {
        return minX;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public int getSizeBlocks() {
        return sizeBlocks;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public Set<UUID> getTrusted() {
        return trusted;
    }

    public boolean isOwnerOrTrusted(UUID playerId) {
        return owner.equals(playerId) || trusted.contains(playerId);
    }
}
