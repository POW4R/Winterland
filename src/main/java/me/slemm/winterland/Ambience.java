package me.slemm.winterland;

import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowman;
import org.bukkit.persistence.PersistentDataType;

import java.util.concurrent.ThreadLocalRandom;

/** Keeps Winterland snowy and at the right time of day, and adds sleigh bells and Santa's ho-ho-hos. */
final class Ambience {

    private static final float[] BELL_PITCHES = {1.0f, 1.12f, 1.26f, 1.5f};
    private final WinterlandPlugin plugin;

    Ambience(WinterlandPlugin plugin) {
        this.plugin = plugin;
    }

    void start() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 40L, 100L);
    }

    private void tick() {
        World w = plugin.winterland();
        if (w == null) return;
        Settings s = plugin.settings();

        if (s.lockedTime() >= 0) w.setTime(s.lockedTime());
        if (s.alwaysSnowing()) {
            if (!w.hasStorm()) w.setStorm(true);
            w.setThundering(false);
            w.setWeatherDuration(20 * 60 * 30);
        }
        ThreadLocalRandom r = ThreadLocalRandom.current();
        if (s.snowGolems()) {
            for (Player p : w.getPlayers()) if (r.nextInt(4) == 0) spawnGolemNear(p, r);
        }
        if (!s.ambience()) return;

        for (Player p : w.getPlayers()) {
            p.spawnParticle(Particle.SNOWFLAKE, p.getLocation().add(0, 3, 0), 25, 6, 2, 6, 0.01);
            if (r.nextInt(8) == 0) jingle(p, r);
            for (Entity e : p.getNearbyEntities(12, 6, 12)) {
                if (e.getPersistentDataContainer().has(plugin.keys().santa, PersistentDataType.BYTE) && r.nextInt(3) == 0) {
                    p.playSound(e.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, 0.8f, 0.6f);
                    p.spawnParticle(Particle.SNOWFLAKE, e.getLocation().add(0, 2.3, 0), 10, 0.4, 0.3, 0.4, 0.01);
                }
            }
        }
    }

    /** Keeps a few Snow Golems around each player, out of sight (16–40 blocks away) on snowy ground. */
    private void spawnGolemNear(Player p, ThreadLocalRandom r) {
        Location at = p.getLocation();
        if (p.getNearbyEntities(48, 32, 48).stream().filter(e -> e instanceof Snowman).count() >= 3) return;
        double angle = r.nextDouble(Math.PI * 2), dist = 16 + r.nextDouble(24);
        int x = at.getBlockX() + (int) (Math.cos(angle) * dist);
        int z = at.getBlockZ() + (int) (Math.sin(angle) * dist);
        World w = p.getWorld();
        if (!w.isChunkLoaded(x >> 4, z >> 4)) return;
        Block ground = w.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        Material m = ground.getType();
        if (m != Material.GRASS_BLOCK && m != Material.SNOW_BLOCK) return;
        Block feet = ground.getRelative(0, 1, 0), head = ground.getRelative(0, 2, 0);
        if (!feet.isPassable() || feet.isLiquid() || !head.isPassable() || head.isLiquid()) return;
        w.spawn(ground.getLocation().add(0.5, 1, 0.5), Snowman.class);
    }

    /** A short sleigh-bell jingle, far away and quiet. */
    private void jingle(Player p, ThreadLocalRandom r) {
        for (int i = 0; i < 4; i++) {
            float pitch = BELL_PITCHES[r.nextInt(BELL_PITCHES.length)];
            int delay = i * 4;
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.25f, pitch), delay);
        }
    }
}
