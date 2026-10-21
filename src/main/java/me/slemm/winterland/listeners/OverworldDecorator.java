package me.slemm.winterland.listeners;

import me.slemm.winterland.Settings;
import me.slemm.winterland.WinterlandPlugin;
import me.slemm.winterland.world.Placer;
import me.slemm.winterland.world.Structures;
import org.bukkit.Chunk;
import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntBinaryOperator;

/**
 * Sprinkles Christmas decorations into newly generated overworld chunks:
 * fairly common in snowy biomes, rare (and funnier) everywhere else —
 * a decorated tree in the desert, a snowman in the jungle...
 * Only brand-new chunks are touched, never anything players have already explored.
 */
public final class OverworldDecorator implements Listener {

    private static final Set<Biome> SNOWY = Set.of(
            Biome.SNOWY_PLAINS, Biome.SNOWY_TAIGA, Biome.SNOWY_SLOPES, Biome.SNOWY_BEACH,
            Biome.ICE_SPIKES, Biome.GROVE, Biome.FROZEN_PEAKS, Biome.JAGGED_PEAKS, Biome.FROZEN_RIVER);

    private final WinterlandPlugin plugin;

    public OverworldDecorator(WinterlandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!event.isNewChunk()) return;
        Chunk chunk = event.getChunk();
        if (!shouldDecorate(chunk.getWorld())) return;
        // One tick later so the chunk is fully settled before we build in it.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (chunk.isLoaded()) decorate(chunk);
        });
    }

    private boolean shouldDecorate(World w) {
        Settings s = plugin.settings();
        if (!s.overworldEnabled() || plugin.isWinterland(w)) return false;
        if (w.getEnvironment() != World.Environment.NORMAL) return false;
        if (!s.overworldWorlds().isEmpty() && !s.overworldWorlds().contains(w.getName())) return false;
        return !s.overworldDecemberOnly() || LocalDate.now(plugin.zone()).getMonth() == Month.DECEMBER;
    }

    private void decorate(Chunk chunk) {
        World w = chunk.getWorld();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double f = plugin.settings().overworldFrequency();
        int baseX = chunk.getX() << 4, baseZ = chunk.getZ() << 4;
        Placer p = Placer.of(chunk);
        IntBinaryOperator ground = (x, z) -> groundAt(chunk, x, z);

        int cy = ground.applyAsInt(baseX + 8, baseZ + 8);
        Biome biome = w.getBiome(baseX + 8, cy == Structures.NO_GROUND ? 64 : cy, baseZ + 8);
        boolean snowy = SNOWY.contains(biome);

        double treeChance = (snowy ? 0.10 : 0.015) * f;
        double snowmanChance = (snowy ? 0.06 : 0.012) * f;
        double caneChance = (snowy ? 0.05 : 0.012) * f;
        double houseChance = (snowy ? 0.008 : 0.002) * f;
        List<int[]> chests = new ArrayList<>();

        if (r.nextDouble() < houseChance) {
            Structures.gingerbreadHouse(p, baseX + 4, baseZ + 4, ground, chests);
        }
        if (r.nextDouble() < treeChance) {
            int x = baseX + 4 + r.nextInt(8), z = baseZ + 4 + r.nextInt(8);
            int y = ground.applyAsInt(x, z);
            if (y != Structures.NO_GROUND) Structures.tree(p, r, x, y, z, ground, snowy ? 0.5 : 0.3, chests);
        }
        if (r.nextDouble() < caneChance) {
            int x = baseX + 4 + r.nextInt(8), z = baseZ + 4 + r.nextInt(8);
            int y = ground.applyAsInt(x, z);
            if (y != Structures.NO_GROUND) Structures.candyCane(p, r, x, y, z);
        }
        if (r.nextDouble() < snowmanChance) {
            int x = baseX + 2 + r.nextInt(12), z = baseZ + 2 + r.nextInt(12);
            int y = ground.applyAsInt(x, z);
            if (y != Structures.NO_GROUND) Structures.snowman(p, r, x, y, z);
        }

        if (!chests.isEmpty()) {
            PersistentDataContainer pdc = chunk.getPersistentDataContainer();
            StringBuilder sb = new StringBuilder(pdc.getOrDefault(plugin.keys().giftSpawned, PersistentDataType.STRING, ""));
            for (int[] c : chests) {
                if (!sb.isEmpty()) sb.append(';');
                sb.append(c[0]).append(':').append(c[1]).append(':').append(c[2]);
            }
            pdc.set(plugin.keys().giftSpawned, PersistentDataType.STRING, sb.toString());
        }
    }

    /** Top solid, dry, natural ground inside this chunk — or NO_GROUND. */
    private static int groundAt(Chunk chunk, int x, int z) {
        if ((x >> 4) != chunk.getX() || (z >> 4) != chunk.getZ()) return Structures.NO_GROUND;
        World w = chunk.getWorld();
        int y = w.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        if (y <= w.getMinHeight()) return Structures.NO_GROUND;
        Block b = chunk.getBlock(x & 15, y, z & 15);
        if (b.getType() == Material.SNOW) { // thin snow layer: stand on what's under it
            y--;
            b = chunk.getBlock(x & 15, y, z & 15);
        }
        Material m = b.getType();
        if (b.isLiquid() || !m.isSolid() || Tag.LOGS.isTagged(m) || Tag.LEAVES.isTagged(m)) return Structures.NO_GROUND;
        return y;
    }
}
