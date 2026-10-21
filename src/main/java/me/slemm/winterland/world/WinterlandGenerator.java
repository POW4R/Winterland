package me.slemm.winterland.world;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Snowable;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.util.noise.SimplexOctaveGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Winterland terrain: rolling snowy hills, frozen lakes and snow-capped mountains,
 * decorated with Christmas trees, candy canes, snowmen and gingerbread houses.
 */
public final class WinterlandGenerator extends ChunkGenerator {

    public static final int SEA_LEVEL = 62;

    private static final BlockData SNOWY_GRASS;
    static {
        BlockData d = Material.GRASS_BLOCK.createBlockData();
        if (d instanceof Snowable s) s.setSnowy(true);
        SNOWY_GRASS = d;
    }

    /** Noise is cached per seed; generation runs on several threads. */
    private final Map<Long, Noise> noiseCache = new ConcurrentHashMap<>();

    private record Noise(SimplexOctaveGenerator hills, SimplexOctaveGenerator detail,
                         SimplexOctaveGenerator peaks, SimplexOctaveGenerator forest) {}

    private Noise noise(long seed) {
        return noiseCache.computeIfAbsent(seed, s -> {
            SimplexOctaveGenerator hills = new SimplexOctaveGenerator(s, 4);
            hills.setScale(1 / 160.0);
            SimplexOctaveGenerator detail = new SimplexOctaveGenerator(s + 1, 3);
            detail.setScale(1 / 32.0);
            SimplexOctaveGenerator peaks = new SimplexOctaveGenerator(s + 2, 4);
            peaks.setScale(1 / 420.0);
            SimplexOctaveGenerator forest = new SimplexOctaveGenerator(s + 3, 2);
            forest.setScale(1 / 220.0);
            return new Noise(hills, detail, peaks, forest);
        });
    }

    /** Surface height (y of the top solid block) at a world x/z. */
    public int heightAt(long seed, int x, int z) {
        Noise n = noise(seed);
        double base = n.hills().noise(x, z, 0.5, 0.5, true);   // -1..1
        double small = n.detail().noise(x, z, 0.5, 0.5, true); // -1..1
        double peak = n.peaks().noise(x, z, 0.5, 0.5, true);   // -1..1
        double h = 67 + base * 9 + small * 2.5;
        if (peak > 0.2) {
            double t = (peak - 0.2) / 0.8;
            h += t * t * 90; // smooth ramp into tall mountains
        }
        return (int) Math.round(h);
    }

    /** 0..1, higher = denser forest. */
    public double forestAt(long seed, int x, int z) {
        return (noise(seed).forest().noise(x, z, 0.5, 0.5, true) + 1) / 2;
    }

    public Biome biomeAt(long seed, int x, int z) {
        int h = heightAt(seed, x, z);
        if (h < SEA_LEVEL) return Biome.FROZEN_RIVER;
        if (h > 115) return Biome.FROZEN_PEAKS;
        if (h > 92) return Biome.GROVE;
        return forestAt(seed, x, z) > 0.55 ? Biome.SNOWY_TAIGA : Biome.SNOWY_PLAINS;
    }

    // ------------------------------------------------------------ generation

    @Override
    public void generateNoise(@NotNull WorldInfo info, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData data) {
        long seed = info.getSeed();
        int minY = data.getMinHeight();
        int maxY = data.getMaxHeight();

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int wx = (chunkX << 4) + lx;
                int wz = (chunkZ << 4) + lz;
                int h = Math.min(heightAt(seed, wx, wz), maxY - 2);

                data.setBlock(lx, minY, lz, Material.BEDROCK);
                int deepTop = Math.min(0, h - 4);
                if (deepTop > minY + 1) data.setRegion(lx, minY + 1, lz, lx + 1, deepTop, lz + 1, Material.DEEPSLATE);
                int stoneFrom = Math.max(minY + 1, deepTop);
                if (h - 3 > stoneFrom) data.setRegion(lx, stoneFrom, lz, lx + 1, h - 3, lz + 1, Material.STONE);

                if (h < SEA_LEVEL) {
                    // Frozen lake: gravel bed, water, ice lid
                    data.setRegion(lx, h - 3, lz, lx + 1, h, lz + 1, Material.DIRT);
                    data.setBlock(lx, h, lz, Material.GRAVEL);
                    if (SEA_LEVEL - 1 > h + 1) data.setRegion(lx, h + 1, lz, lx + 1, SEA_LEVEL, lz + 1, Material.WATER);
                    else data.setBlock(lx, h + 1, lz, Material.WATER);
                    data.setBlock(lx, SEA_LEVEL, lz, Material.ICE);
                } else if (h > 100) {
                    // Mountain: stone with a thick snow cap, packed ice deep in the peaks
                    data.setRegion(lx, h - 3, lz, lx + 1, h + 1, lz + 1, h > 125 ? Material.PACKED_ICE : Material.SNOW_BLOCK);
                    data.setBlock(lx, h, lz, Material.SNOW_BLOCK);
                } else {
                    data.setRegion(lx, h - 3, lz, lx + 1, h, lz + 1, Material.DIRT);
                    data.setBlock(lx, h, lz, SNOWY_GRASS);
                    data.setBlock(lx, h + 1, lz, Material.SNOW);
                }
            }
        }
        placeOres(data, random, minY);
    }

    private void placeOres(ChunkData data, Random random, int minY) {
        vein(data, random, Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE, 18, 0, 120, 8);
        vein(data, random, Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE, 14, minY + 5, 70, 6);
        vein(data, random, Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE, 8, 0, 90, 7);
        vein(data, random, Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE, 4, minY + 5, 30, 5);
        vein(data, random, Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE, 5, minY + 5, 10, 5);
        vein(data, random, Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE, 3, minY + 5, 30, 5);
        vein(data, random, Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE, 3, minY + 5, -20, 4);
        vein(data, random, Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE, 2, 40, 140, 1);
    }

    private void vein(ChunkData data, Random r, Material ore, Material deepOre, int tries, int minY, int maxY, int size) {
        if (maxY <= minY) return;
        for (int i = 0; i < tries; i++) {
            int x = r.nextInt(16), z = r.nextInt(16), y = minY + r.nextInt(maxY - minY);
            for (int s = 0; s < size; s++) {
                int bx = Math.max(0, Math.min(15, x + r.nextInt(3) - 1));
                int bz = Math.max(0, Math.min(15, z + r.nextInt(3) - 1));
                int by = y + r.nextInt(3) - 1;
                if (by <= data.getMinHeight() || by >= data.getMaxHeight()) continue;
                Material cur = data.getType(bx, by, bz);
                if (cur == Material.STONE) data.setBlock(bx, by, bz, ore);
                else if (cur == Material.DEEPSLATE) data.setBlock(bx, by, bz, deepOre);
            }
        }
    }

    // ------------------------------------------------------------ settings

    @Override
    public @Nullable BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo) {
        return new BiomeProvider() {
            @Override
            public @NotNull Biome getBiome(@NotNull WorldInfo info, int x, int y, int z) {
                return biomeAt(info.getSeed(), x, z);
            }

            @Override
            public @NotNull List<Biome> getBiomes(@NotNull WorldInfo info) {
                return List.of(Biome.SNOWY_PLAINS, Biome.SNOWY_TAIGA, Biome.GROVE, Biome.FROZEN_PEAKS, Biome.FROZEN_RIVER);
            }
        };
    }

    @Override
    public @NotNull List<BlockPopulator> getDefaultPopulators(@NotNull World world) {
        return List.of(new WinterlandPopulator(this));
    }

    @Override
    public @Nullable Location getFixedSpawnLocation(@NotNull World world, @NotNull Random random) {
        int h = heightAt(world.getSeed(), 0, 0);
        int y = Math.max(h, SEA_LEVEL) + 2;
        return new Location(world, 0.5, y, 0.5);
    }

    @Override public boolean shouldGenerateNoise() { return false; }
    @Override public boolean shouldGenerateSurface() { return false; }
    @Override public boolean shouldGenerateCaves() { return false; }
    @Override public boolean shouldGenerateDecorations() { return false; }
    @Override public boolean shouldGenerateMobs() { return true; }
    @Override public boolean shouldGenerateStructures() { return false; }
}
