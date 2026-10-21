package me.slemm.winterland;

import me.slemm.winterland.listeners.AdventCalendar;
import me.slemm.winterland.listeners.GiftChestListener;
import me.slemm.winterland.listeners.ItemListener;
import me.slemm.winterland.listeners.MobListener;
import me.slemm.winterland.listeners.OverworldDecorator;
import me.slemm.winterland.listeners.SantaListener;
import me.slemm.winterland.listeners.SnowballListener;
import me.slemm.winterland.listeners.WorldListener;
import me.slemm.winterland.world.WinterlandGenerator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZoneId;

public final class WinterlandPlugin extends JavaPlugin {

    private Keys keys;
    private XmasItems items;
    private Settings settings;
    private SnowballStats stats;
    private AdventCalendar advent;
    private World winterland;
    private Advancements advancements;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = new Settings(getConfig());
        keys = new Keys(this);
        items = new XmasItems(this);
        stats = new SnowballStats(this);
        items.registerRecipes();

        winterland = createWinterland();
        advancements = new Advancements(this);
        advancements.register();

        advent = new AdventCalendar(this);
        var pm = getServer().getPluginManager();
        pm.registerEvents(new ItemListener(this), this);
        pm.registerEvents(new GiftChestListener(this), this);
        pm.registerEvents(new MobListener(this), this);
        pm.registerEvents(new SantaListener(this), this);
        pm.registerEvents(new SnowballListener(this), this);
        pm.registerEvents(new WorldListener(this), this);
        pm.registerEvents(new OverworldDecorator(this), this);
        pm.registerEvents(advent, this);

        XmasCommand cmd = new XmasCommand(this);
        PluginCommand xmas = getCommand("xmas");
        if (xmas != null) {
            xmas.setExecutor(cmd);
            xmas.setTabCompleter(cmd);
        }
        PluginCommand adventCmd = getCommand("advent");
        if (adventCmd != null) {
            adventCmd.setExecutor((sender, command, label, args) -> {
                if (sender instanceof Player p) {
                    if (!p.hasPermission("winterland.advent")) {
                        p.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
                    } else {
                        advent.open(p);
                    }
                }
                return true;
            });
        }

        new Ambience(this).start();
        getLogger().info("❄ Winterland is ready. Merry Christmas! ❄");
    }

    @Override
    public void onDisable() {
        if (stats != null) stats.save();
        if (items != null) items.unregisterRecipes();
    }

    private World createWinterland() {
        String name = settings.worldName();
        World existing = Bukkit.getWorld(name);
        if (existing != null) return existing;
        getLogger().info("Loading Winterland dimension '" + name + "' (first start generates it, this can take a moment)...");
        World w = new WorldCreator(name)
                .environment(World.Environment.NORMAL)
                .generator(new WinterlandGenerator())
                .createWorld();
        if (w == null) {
            getLogger().severe("Could not create the Winterland world!");
        }
        return w;
    }

    /** Lets other tools (bukkit.yml, Multiverse) use "Winterland" as a generator name. */
    @Override
    public @Nullable ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        return new WinterlandGenerator();
    }

    public void reloadSettings() {
        reloadConfig();
        settings = new Settings(getConfig());
    }

    // ---------- travel helpers (used by the Snow Globe and /xmas tp) ----------

    public boolean isWinterland(@Nullable World world) {
        return world != null && winterland != null && world.equals(winterland);
    }

    public void sendToWinterland(Player player) {
        if (winterland == null) {
            player.sendMessage(Component.text("Winterland isn't loaded. Check the server console.", NamedTextColor.RED));
            return;
        }
        if (isWinterland(player.getWorld())) {
            player.sendMessage(Component.text("You're already in Winterland! ❄", NamedTextColor.AQUA));
            return;
        }
        String from = serialize(player.getLocation());
        Location spawn = winterland.getSpawnLocation();
        // Load (or generate) the spawn chunk off the main thread first, then pick a safe spot in it.
        winterland.getChunkAtAsync(spawn).thenAccept(chunk -> {
            if (!player.isOnline()) return;
            Location safe = safeSpot(winterland, spawn.getBlockX(), spawn.getBlockZ());
            safe.setYaw(player.getLocation().getYaw());
            travel(player, safe, () -> player.getPersistentDataContainer().set(keys.returnLoc, PersistentDataType.STRING, from));
        }).exceptionally(e -> {
            getLogger().warning("Could not load the Winterland spawn chunk: " + e);
            player.sendMessage(Component.text("Winterland couldn't be reached right now. Try again in a moment.", NamedTextColor.RED));
            return null;
        });
    }

    public void leaveWinterland(Player player) {
        if (!isWinterland(player.getWorld())) {
            player.sendMessage(Component.text("You're not in Winterland.", NamedTextColor.GRAY));
            return;
        }
        String saved = player.getPersistentDataContainer().get(keys.returnLoc, PersistentDataType.STRING);
        Location back = saved == null ? null : deserialize(saved);
        if (back == null) back = Bukkit.getWorlds().get(0).getSpawnLocation();
        travel(player, back, () -> { });
    }

    /** Teleports asynchronously (chunks load without freezing the server) and tells the player if it failed. */
    private void travel(Player player, Location to, Runnable onSuccess) {
        if (player.isInsideVehicle()) player.leaveVehicle(); // teleports fail while riding something
        player.teleportAsync(to).thenAccept(ok -> {
            if (ok) {
                onSuccess.run();
            } else {
                player.sendMessage(Component.text("The teleport was blocked (another plugin or a passenger may be stopping it).", NamedTextColor.RED));
                getLogger().warning("Teleport of " + player.getName() + " to " + to.getWorld().getName() + " was cancelled.");
            }
        });
    }

    /** Standing spot on real ground near x/z: not on a tree, not inside blocks, not on water. */
    private static Location safeSpot(World w, int x, int z) {
        for (int r = 0; r <= 12; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue; // ring only
                    int bx = x + dx, bz = z + dz;
                    if ((bx >> 4) != (x >> 4) || (bz >> 4) != (z >> 4)) continue; // stay in the loaded chunk
                    Block ground = w.getHighestBlockAt(bx, bz, HeightMap.MOTION_BLOCKING_NO_LEAVES);
                    if (!isGoodGround(ground.getType())) continue;
                    Block feet = ground.getRelative(0, 1, 0), head = ground.getRelative(0, 2, 0);
                    if (feet.isPassable() && !feet.isLiquid() && head.isPassable() && !head.isLiquid()) {
                        return ground.getLocation().add(0.5, 1, 0.5);
                    }
                }
            }
        }
        return w.getHighestBlockAt(x, z).getLocation().add(0.5, 1, 0.5);
    }

    private static boolean isGoodGround(Material m) {
        return switch (m) {
            case GRASS_BLOCK, SNOW_BLOCK, DIRT, STONE, GRAVEL, ICE, PACKED_ICE -> true;
            default -> false;
        };
    }

    private static String serialize(Location l) {
        return l.getWorld().getName() + ";" + l.getX() + ";" + l.getY() + ";" + l.getZ() + ";" + l.getYaw() + ";" + l.getPitch();
    }

    private static @Nullable Location deserialize(String s) {
        try {
            String[] p = s.split(";");
            World w = Bukkit.getWorld(p[0]);
            if (w == null) return null;
            return new Location(w, Double.parseDouble(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3]),
                    Float.parseFloat(p[4]), Float.parseFloat(p[5]));
        } catch (Exception e) {
            return null;
        }
    }

    public ZoneId zone() {
        String tz = settings.timezone();
        if (tz == null || tz.isBlank()) return ZoneId.systemDefault();
        try {
            return ZoneId.of(tz);
        } catch (Exception e) {
            return ZoneId.systemDefault();
        }
    }

    public Keys keys() { return keys; }
    public XmasItems items() { return items; }
    public Settings settings() { return settings; }
    public SnowballStats stats() { return stats; }
    public AdventCalendar advent() { return advent; }
    public Advancements advancements() { return advancements; }
    public @Nullable World winterland() { return winterland; }
}
