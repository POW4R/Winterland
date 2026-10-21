package me.slemm.winterland.world;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;

import java.util.List;
import java.util.Random;
import java.util.function.IntBinaryOperator;

/**
 * All Christmas decorations, shared by Winterland world-gen and the overworld decorator.
 * {@code ground} returns the y of the top solid block at (x, z), or Integer.MIN_VALUE if
 * that spot is unusable (water, outside the chunk, ...).
 * Every chest placed is added to {@code chests} as {x, y, z} so it can become a gift chest.
 */
public final class Structures {
    private Structures() {}

    public static final int NO_GROUND = Integer.MIN_VALUE;

    private static final Material[] BAUBLES = {
            Material.OCHRE_FROGLIGHT, Material.VERDANT_FROGLIGHT, Material.PEARLESCENT_FROGLIGHT,
            Material.SHROOMLIGHT, Material.SEA_LANTERN, Material.RED_WOOL
    };

    private static final BlockData LEAVES;
    static {
        BlockData d = Material.SPRUCE_LEAVES.createBlockData();
        if (d instanceof Leaves l) l.setPersistent(true); // never decay
        LEAVES = d;
    }

    // ------------------------------------------------------- Christmas tree

    /** Decorated tree with glowing baubles and a star. Presents underneath with {@code giftChance}. */
    public static boolean tree(Placer p, Random random, int x, int ground, int z,
                               IntBinaryOperator groundAt, double giftChance, List<int[]> chests) {
        int trunk = 8 + random.nextInt(5);
        int top = ground + trunk;
        for (int y = ground + 1; y <= top + 2; y++) if (!p.isFree(x, y, z)) return false;

        for (int y = ground + 1; y < top; y++) p.set(x, y, z, Material.SPRUCE_LOG);

        int leafStart = ground + 3;
        for (int y = leafStart; y <= top; y++) {
            int fromTop = top - y;
            int radius = Math.min(4, (fromTop + 1) / 2);
            if ((y - leafStart) % 3 == 2) radius = Math.max(0, radius - 1); // tiered "fir" look
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int d2 = dx * dx + dz * dz;
                    if (d2 > radius * radius + radius) continue;
                    if (dx == 0 && dz == 0 && y < top) continue; // trunk
                    int bx = x + dx, bz = z + dz;
                    if (!p.isFree(bx, y, bz)) continue;
                    boolean edge = d2 >= (radius - 1) * (radius - 1) + (radius - 1);
                    if (edge && radius > 0 && random.nextDouble() < 0.12) {
                        p.set(bx, y, bz, BAUBLES[random.nextInt(BAUBLES.length)]);
                    } else {
                        p.set(bx, y, bz, LEAVES);
                    }
                }
            }
        }
        p.set(x, top + 1, z, Material.GLOWSTONE); // the star
        p.set(x, top + 2, z, Material.END_ROD);

        if (random.nextDouble() < giftChance) {
            int[][] spots = {{2, 0}, {-2, 1}, {1, -2}, {-1, 2}};
            int count = 1 + random.nextInt(2);
            for (int i = 0; i < count; i++) {
                int[] s = spots[random.nextInt(spots.length)];
                int cx = x + s[0], cz = z + s[1];
                int cy = groundAt.applyAsInt(cx, cz);
                if (cy == NO_GROUND || Math.abs(cy - ground) > 1 || !p.isFree(cx, cy + 1, cz)) continue;
                p.set(cx, cy + 1, cz, Placer.facing(Material.CHEST, Placer.randomFace(random)));
                chests.add(new int[]{cx, cy + 1, cz});
            }
        }
        return true;
    }

    // ----------------------------------------------------------- candy cane

    public static boolean candyCane(Placer p, Random random, int x, int ground, int z) {
        int height = 5 + random.nextInt(4);
        for (int y = ground + 1; y <= ground + height + 1; y++) if (!p.isFree(x, y, z)) return false;
        int[] dir = switch (random.nextInt(4)) {
            case 0 -> new int[]{1, 0};
            case 1 -> new int[]{-1, 0};
            case 2 -> new int[]{0, 1};
            default -> new int[]{0, -1};
        };
        int stripe = 0;
        for (int y = ground + 1; y <= ground + height; y++) stripe(p, x, y, z, stripe++);
        int top = ground + height;
        stripe(p, x + dir[0], top + 1, z + dir[1], stripe++);
        stripe(p, x + dir[0] * 2, top + 1, z + dir[1] * 2, stripe++);
        stripe(p, x + dir[0] * 3, top, z + dir[1] * 3, stripe++);
        stripe(p, x + dir[0] * 3, top - 1, z + dir[1] * 3, stripe);
        return true;
    }

    private static void stripe(Placer p, int x, int y, int z, int i) {
        if (p.isFree(x, y, z)) p.set(x, y, z, i % 2 == 0 ? Material.RED_CONCRETE : Material.WHITE_CONCRETE);
    }

    // -------------------------------------------------------------- snowman

    /** A living Snow Golem; where one would melt, a snow-block statue of one instead. */
    public static boolean snowman(Placer p, Random random, int x, int ground, int z) {
        for (int y = ground + 1; y <= ground + 4; y++) if (!p.isFree(x, y, z)) return false;
        if (p.spawnSnowGolem(x, ground + 1, z)) return true;
        p.set(x, ground + 1, z, Material.SNOW_BLOCK);
        p.set(x, ground + 2, z, Material.SNOW_BLOCK);
        p.set(x, ground + 3, z, Placer.facing(Material.CARVED_PUMPKIN, Placer.randomFace(random)));
        if (random.nextBoolean()) p.set(x, ground + 4, z, Material.RED_CARPET); // festive hat
        return true;
    }

    // ---------------------------------------------------- gingerbread house

    private static final int SIZE = 7;

    /** 7x7 house whose corner is (x0, z0). Needs fairly flat, dry ground. */
    public static boolean gingerbreadHouse(Placer p, int x0, int z0, IntBinaryOperator groundAt, List<int[]> chests) {
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int dx = 0; dx < SIZE; dx++) {
            for (int dz = 0; dz < SIZE; dz++) {
                int h = groundAt.applyAsInt(x0 + dx, z0 + dz);
                if (h == NO_GROUND) return false;
                min = Math.min(min, h);
                max = Math.max(max, h);
            }
        }
        if (max - min > 3) return false;
        int y0 = max + 1; // floor level

        // Only build into open space (leaves/grass are fine, someone's base is not)
        for (int dx = -1; dx <= SIZE; dx++)
            for (int dz = -1; dz <= SIZE; dz++)
                for (int y = y0 + 1; y <= y0 + 9; y++) {
                    Material m = p.get(x0 + dx, y, z0 + dz);
                    if (!p.isFree(x0 + dx, y, z0 + dz) && !org.bukkit.Tag.LEAVES.isTagged(m) && !org.bukkit.Tag.LOGS.isTagged(m)) return false;
                }

        for (int dx = -1; dx <= SIZE; dx++)
            for (int dz = -1; dz <= SIZE; dz++)
                for (int y = y0; y <= y0 + 10; y++) p.set(x0 + dx, y, z0 + dz, Material.AIR);

        for (int dx = 0; dx < SIZE; dx++) {
            for (int dz = 0; dz < SIZE; dz++) {
                int ground = groundAt.applyAsInt(x0 + dx, z0 + dz);
                for (int y = ground + 1; y < y0; y++) p.set(x0 + dx, y, z0 + dz, Material.PACKED_MUD);
                p.set(x0 + dx, y0, z0 + dz, Material.SPRUCE_PLANKS);
            }
        }

        // Gingerbread walls, icing corners and an icing band on top
        for (int y = y0 + 1; y <= y0 + 4; y++) {
            for (int dx = 0; dx < SIZE; dx++) {
                for (int dz = 0; dz < SIZE; dz++) {
                    boolean wallX = dx == 0 || dx == SIZE - 1;
                    boolean wallZ = dz == 0 || dz == SIZE - 1;
                    if (!wallX && !wallZ) continue;
                    Material m = (wallX && wallZ) || y == y0 + 4 ? Material.WHITE_CONCRETE : Material.TERRACOTTA;
                    p.set(x0 + dx, y, z0 + dz, m);
                }
            }
        }

        // Door, mini candy canes, glowing windows
        p.set(x0 + 3, y0 + 1, z0, Material.AIR);
        p.set(x0 + 3, y0 + 2, z0, Material.AIR);
        p.set(x0 + 2, y0 + 1, z0 - 1, Material.RED_CONCRETE);
        p.set(x0 + 2, y0 + 2, z0 - 1, Material.WHITE_CONCRETE);
        p.set(x0 + 4, y0 + 1, z0 - 1, Material.RED_CONCRETE);
        p.set(x0 + 4, y0 + 2, z0 - 1, Material.WHITE_CONCRETE);
        p.set(x0, y0 + 2, z0 + 3, Material.YELLOW_STAINED_GLASS);
        p.set(x0 + SIZE - 1, y0 + 2, z0 + 3, Material.YELLOW_STAINED_GLASS);
        p.set(x0 + 3, y0 + 2, z0 + SIZE - 1, Material.YELLOW_STAINED_GLASS);

        // Icing ceiling, candy-striped stepped roof, light on top
        for (int dx = 0; dx < SIZE; dx++)
            for (int dz = 0; dz < SIZE; dz++)
                p.set(x0 + dx, y0 + 5, z0 + dz, Material.WHITE_CONCRETE);
        for (int i = 1; i <= 3; i++) {
            int y = y0 + 5 + i;
            Material m = i % 2 == 1 ? Material.RED_CONCRETE : Material.WHITE_CONCRETE;
            for (int dx = i; dx < SIZE - i; dx++)
                for (int dz = i; dz < SIZE - i; dz++) {
                    boolean edge = dx == i || dx == SIZE - 1 - i || dz == i || dz == SIZE - 1 - i;
                    if (edge) p.set(x0 + dx, y, z0 + dz, m);
                }
        }
        p.set(x0 + 3, y0 + 9, z0 + 3, Material.GLOWSTONE);

        // Cosy interior with a gift chest
        p.set(x0 + 1, y0 + 1, z0 + 5, Placer.facing(Material.CHEST, BlockFace.NORTH));
        chests.add(new int[]{x0 + 1, y0 + 1, z0 + 5});
        p.set(x0 + 5, y0 + 1, z0 + 5, Material.LANTERN);
        p.set(x0 + 5, y0 + 1, z0 + 1, Material.CRAFTING_TABLE);
        p.set(x0 + 3, y0 + 1, z0 + 5, Material.RED_CARPET);
        p.set(x0 + 1, y0 + 4, z0 + 1, Material.OCHRE_FROGLIGHT);
        p.set(x0 + 5, y0 + 4, z0 + 5, Material.VERDANT_FROGLIGHT);
        return true;
    }
}
