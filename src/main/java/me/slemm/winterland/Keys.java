package me.slemm.winterland;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/** All persistent-data keys in one place. */
public final class Keys {
    private final Plugin plugin;

    public final NamespacedKey item;          // ItemStack: which Christmas item this is
    public final NamespacedKey santa;         // Entity: this villager is Santa
    public final NamespacedKey mobType;       // Entity: "elf" or "grinch"
    public final NamespacedKey returnLoc;     // Player: where to go back to from Winterland
    public final NamespacedKey santaDay;      // Player: last day Santa gave a gift
    public final NamespacedKey naughtyUntil;  // Player: epoch millis until they're off the naughty list
    public final NamespacedKey giftOpened;    // Chunk: gift chests already filled
    public final NamespacedKey giftPlaced;    // Chunk: chests placed by players (never gifts)
    public final NamespacedKey giftSpawned;   // Chunk: overworld chests our decorations placed

    public Keys(Plugin plugin) {
        this.plugin = plugin;
        item = key("item");
        santa = key("santa");
        mobType = key("mob_type");
        returnLoc = key("return_loc");
        santaDay = key("santa_day");
        naughtyUntil = key("naughty_until");
        giftOpened = key("gift_opened");
        giftPlaced = key("gift_placed");
        giftSpawned = key("gift_spawned");
    }

    public NamespacedKey advent(int year) {
        return key("advent_" + year);
    }

    public NamespacedKey recipe(String name) {
        return key(name);
    }

    private NamespacedKey key(String k) {
        return new NamespacedKey(plugin, k);
    }
}
