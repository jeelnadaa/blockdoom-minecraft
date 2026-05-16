package com.blockdoom.util;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

/**
 * Utility for spawning visual particle effects.
 */
public final class ParticleUtil {

    private ParticleUtil() {}

    public static void spawnDisintegrateParticle(Location location, BlockData blockData) {
        World world = location.getWorld();
        if (world == null) return;

        world.spawnParticle(
                Particle.BLOCK_CRACK,
                location.clone().add(0.5, 0.5, 0.5),
                15,
                0.3, 0.3, 0.3,
                0.05,
                blockData
        );
        world.spawnParticle(
                Particle.SMOKE_NORMAL,
                location.clone().add(0.5, 0.5, 0.5),
                5,
                0.2, 0.2, 0.2,
                0.02
        );
    }
}
