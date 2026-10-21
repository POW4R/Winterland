package me.slemm.winterland;

import org.bukkit.configuration.file.FileConfiguration;

public record Settings(
        String worldName,
        long lockedTime,
        boolean alwaysSnowing,
        double elfChance,
        double grinchChance,
        boolean ambience,
        boolean snowGolems,
        int naughtyHours,
        boolean snowballEverywhere,
        double snowballKnockback,
        int globeCooldownSeconds,
        String timezone,
        boolean adventCatchUp,
        int adventTestDay,
        boolean overworldEnabled,
        boolean overworldDecemberOnly,
        double overworldFrequency,
        java.util.List<String> overworldWorlds
) {
    public Settings(FileConfiguration c) {
        this(
                c.getString("winterland.world-name", "winterland"),
                c.getLong("winterland.locked-time", 18000),
                c.getBoolean("winterland.always-snowing", true),
                c.getDouble("winterland.elf-chance", 0.35),
                c.getDouble("winterland.grinch-chance", 0.01),
                c.getBoolean("winterland.ambience", true),
                c.getBoolean("winterland.snow-golems", true),
                c.getInt("naughty-list.hours", 24),
                c.getBoolean("snowball-fights.everywhere", true),
                c.getDouble("snowball-fights.knockback", 0.45),
                c.getInt("snow-globe.cooldown-seconds", 10),
                c.getString("advent.timezone", ""),
                c.getBoolean("advent.allow-catch-up", true),
                c.getInt("advent.test-day", 0),
                c.getBoolean("overworld-decorations.enabled", true),
                c.getBoolean("overworld-decorations.only-in-december", false),
                c.getDouble("overworld-decorations.frequency", 1.0),
                c.getStringList("overworld-decorations.worlds")
        );
    }
}
