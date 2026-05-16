package com.blockdoom.model;

import org.bukkit.Chunk;
import org.bukkit.World;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable record representing a specific chunk in a specific world.
 */
public record ChunkPos(UUID worldId, int x, int z) {

    public static ChunkPos fromChunk(Chunk chunk) {
        return new ChunkPos(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
    }

    public static ChunkPos fromLocation(World world, int blockX, int blockZ) {
        return new ChunkPos(world.getUID(), blockX >> 4, blockZ >> 4);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChunkPos chunkPos = (ChunkPos) o;
        return x == chunkPos.x && z == chunkPos.z && Objects.equals(worldId, chunkPos.worldId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(worldId, x, z);
    }
}
