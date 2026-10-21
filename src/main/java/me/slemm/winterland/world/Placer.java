package me.slemm.winterland.world;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.generator.LimitedRegion;

import java.util.Random;

/**
 * Where structures get built: either a world-gen region (Winterland) or a freshly
 * generated overworld chunk. Writes outside the allowed area are silently skipped,
 * so a structure can never load or touch neighbouring chunks.
 */
public interface Placer {

    BlockFace[] HORIZONTAL = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

    void set(int x, int y, int z, Material m);

    void set(int x, int y, int z, BlockData d);

    /** Block type, or BEDROCK if outside the allowed area. */
    Material get(int x, int y, int z);

    /** Free space a decoration may build into (air, snow layers, grass, flowers...). */
    default boolean isFree(int x, int y, int z) {
        Material m = get(x, y, z);
        return m.isAir() || m == Material.SNOW || Tag.REPLACEABLE.isTagged(m) || Tag.FLOWERS.isTagged(m);
    }

    static BlockData facing(Material m, BlockFace face) {
        BlockData d = m.createBlockData();
        if (d instanceof Directional dir && dir.getFaces().contains(face)) dir.setFacing(face);
        return d;
    }

    static BlockFace randomFace(Random random) {
        return HORIZONTAL[random.nextInt(HORIZONTAL.length)];
    }

    // ------------------------------------------------------------- impls

    /** World-gen populator region (used in Winterland). */
    static Placer of(LimitedRegion r) {
        return new Placer() {
            @Override
            public void set(int x, int y, int z, Material m) {
                if (r.isInRegion(x, y, z)) r.setType(x, y, z, m);
            }

            @Override
            public void set(int x, int y, int z, BlockData d) {
                if (r.isInRegion(x, y, z)) r.setBlockData(x, y, z, d);
            }

            @Override
            public Material get(int x, int y, int z) {
                return r.isInRegion(x, y, z) ? r.getType(x, y, z) : Material.BEDROCK;
            }
        };
    }

    /** A single loaded chunk (used for overworld decorations). */
    static Placer of(Chunk chunk) {
        World w = chunk.getWorld();
        int cx = chunk.getX(), cz = chunk.getZ();
        int minY = w.getMinHeight(), maxY = w.getMaxHeight();
        return new Placer() {
            private boolean inside(int x, int y, int z) {
                return (x >> 4) == cx && (z >> 4) == cz && y >= minY && y < maxY;
            }

            @Override
            public void set(int x, int y, int z, Material m) {
                if (inside(x, y, z)) chunk.getBlock(x & 15, y, z & 15).setType(m, false);
            }

            @Override
            public void set(int x, int y, int z, BlockData d) {
                if (inside(x, y, z)) chunk.getBlock(x & 15, y, z & 15).setBlockData(d, false);
            }

            @Override
            public Material get(int x, int y, int z) {
                return inside(x, y, z) ? chunk.getBlock(x & 15, y, z & 15).getType() : Material.BEDROCK;
            }
        };
    }
}
