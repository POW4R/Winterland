package me.slemm.winterland.world;

import org.bukkit.block.Biome;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.IntBinaryOperator;

/** Decorates Winterland: trees, candy canes, snowmen and gingerbread houses. */
final class WinterlandPopulator extends BlockPopulator {

    private final WinterlandGenerator gen;

    WinterlandPopulator(WinterlandGenerator gen) {
        this.gen = gen;
    }

    @Override
    public void populate(@NotNull WorldInfo info, @NotNull Random random, int chunkX, int chunkZ, @NotNull LimitedRegion region) {
        long seed = info.getSeed();
        int baseX = chunkX << 4, baseZ = chunkZ << 4;
        Placer p = Placer.of(region);
        IntBinaryOperator ground = (x, z) -> {
            int h = gen.heightAt(seed, x, z);
            return h < WinterlandGenerator.SEA_LEVEL ? Structures.NO_GROUND : h; // frozen lakes are off limits
        };
        List<int[]> chests = new ArrayList<>(); // Winterland chests are gifts anyway

        // Gingerbread house first so it gets the open space
        if (random.nextInt(40) == 0) {
            Biome b = gen.biomeAt(seed, baseX + 7, baseZ + 7);
            if (b == Biome.SNOWY_PLAINS || b == Biome.SNOWY_TAIGA) {
                Structures.gingerbreadHouse(p, baseX + 4, baseZ + 4, ground, chests);
            }
        }

        Biome biome = gen.biomeAt(seed, baseX + 8, baseZ + 8);
        int trees;
        if (biome == Biome.SNOWY_TAIGA) trees = 1 + random.nextInt(3);
        else if (biome == Biome.SNOWY_PLAINS) trees = random.nextInt(4) == 0 ? 1 : 0;
        else if (biome == Biome.GROVE) trees = random.nextInt(2);
        else trees = 0;
        for (int i = 0; i < trees; i++) {
            int x = baseX + 4 + random.nextInt(8), z = baseZ + 4 + random.nextInt(8);
            int y = ground.applyAsInt(x, z);
            if (y != Structures.NO_GROUND && y <= 110) Structures.tree(p, random, x, y, z, ground, 0.4, chests);
        }

        if (random.nextInt(6) == 0 && (biome == Biome.SNOWY_PLAINS || biome == Biome.SNOWY_TAIGA)) {
            int x = baseX + 4 + random.nextInt(8), z = baseZ + 4 + random.nextInt(8);
            int y = ground.applyAsInt(x, z);
            if (y != Structures.NO_GROUND) Structures.candyCane(p, random, x, y, z);
        }

        if (random.nextInt(7) == 0) {
            int x = baseX + 2 + random.nextInt(12), z = baseZ + 2 + random.nextInt(12);
            int y = ground.applyAsInt(x, z);
            if (y != Structures.NO_GROUND && y <= 100) Structures.snowman(p, random, x, y, z);
        }
    }
}
