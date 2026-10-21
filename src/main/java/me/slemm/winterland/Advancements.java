package me.slemm.winterland;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.UnsafeValues;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * A "Winterland" tab in the vanilla advancements screen (L key). Every advancement uses the
 * impossible trigger and is granted by the plugin when the player does the thing.
 */
public final class Advancements {

    public enum Adv {
        ROOT("root", null, "snow_block", "Winterland", "Craft a Snow Globe or visit Winterland", Frame.TASK, false),
        LET_IT_SNOW("let_it_snow", ROOT, "light_blue_stained_glass", "Let It Snow!", "Travel to Winterland", Frame.TASK, false),
        GIFT_CHEST("gift_chest", LET_IT_SNOW, "chest", "Under the Tree", "Find a gift chest", Frame.TASK, false),
        UNWRAPPED("unwrapped", ROOT, "red_shulker_box", "Unwrapped", "Open a Christmas present", Frame.TASK, false),
        SANTA("santa", UNWRAPPED, "cake", "Milk and Cookies", "Get a present from Santa Claus", Frame.TASK, false),
        NAUGHTY("naughty", ROOT, "coal", "Naughty!", "End up on Santa's naughty list", Frame.TASK, true),
        GINGERBREAD("gingerbread", ROOT, "cookie", "Run, Run, As Fast As You Can", "Eat a Gingerbread Cookie", Frame.TASK, false),
        HOT_COCOA("hot_cocoa", GINGERBREAD, "honey_bottle", "Warm and Cozy", "Drink a Hot Cocoa", Frame.TASK, false),
        SANTA_HAT("santa_hat", ROOT, "red_wool", "Fashionably Festive", "Wear a Santa Hat", Frame.TASK, false),
        SNOWMAN("snowman", ROOT, "carved_pumpkin", "Do You Want to Build a Snowman?", "Build a Snow Golem", Frame.TASK, false),
        SNOWBALL("snowball", SNOWMAN, "snowball", "Snowball Fight!", "Hit another player with a snowball", Frame.TASK, false),
        SNOWBALL_CHAMPION("snowball_champion", SNOWBALL, "powder_snow_bucket", "Snowball Champion", "Land 100 snowball hits on other players", Frame.CHALLENGE, false),
        ELF("elf", LET_IT_SNOW, "leather_helmet", "Elf on the Shelf", "Defeat a Naughty Elf", Frame.TASK, false),
        GRINCH("grinch", ELF, "zombie_head", "You Saved Christmas!", "Defeat The Grinch", Frame.CHALLENGE, false),
        THREE_SIZES("three_sizes", GRINCH, "heart_of_the_sea", "Three Sizes That Day", "Use the Grinch's Heart", Frame.GOAL, false),
        ADVENT("advent", ROOT, "book", "Christmas Countdown", "Open a door of the Advent Calendar", Frame.TASK, false),
        ADVENT_24("advent_24", ADVENT, "yellow_shulker_box", "It's Christmas Eve!", "Open door 24 of the Advent Calendar", Frame.CHALLENGE, false);

        final String id;
        final Adv parent;
        final String icon, title, description;
        final Frame frame;
        final boolean hidden;

        Adv(String id, Adv parent, String icon, String title, String description, Frame frame, boolean hidden) {
            this.id = id;
            this.parent = parent;
            this.icon = icon;
            this.title = title;
            this.description = description;
            this.frame = frame;
            this.hidden = hidden;
        }
    }

    enum Frame { TASK, GOAL, CHALLENGE }

    private final WinterlandPlugin plugin;

    public Advancements(WinterlandPlugin plugin) {
        this.plugin = plugin;
    }

    /** Registers the tab. Enum order puts every parent before its children. */
    public void register() {
        Method load = loadMethod();
        int missing = 0;
        for (Adv a : Adv.values()) {
            NamespacedKey key = key(a);
            if (Bukkit.getAdvancement(key) != null) continue;
            boolean ok = false;
            if (load != null) {
                try {
                    ok = load.invoke(Bukkit.getUnsafe(), key, json(a)) != null;
                } catch (ReflectiveOperationException | RuntimeException e) {
                    plugin.getLogger().warning("Could not load advancement " + key + ": " + e.getMessage());
                }
            }
            if (!ok) missing++;
        }
        if (missing > 0) {
            // Fallback: a datapack in the main world, picked up on the next start.
            if (writeDatapack()) {
                plugin.getLogger().warning(missing + " Winterland advancement(s) could not be loaded live. "
                        + "They were saved as a datapack and will show up after the next server restart.");
            }
        }
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::checkHats, 100L, 100L);
    }

    private static Method loadMethod() {
        try {
            return UnsafeValues.class.getMethod("loadAdvancement", NamespacedKey.class, String.class);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private boolean writeDatapack() {
        try {
            Path pack = Bukkit.getWorlds().get(0).getWorldFolder().toPath().resolve("datapacks").resolve("winterland");
            Files.createDirectories(pack);
            Files.writeString(pack.resolve("pack.mcmeta"),
                    "{\"pack\":{\"description\":\"Winterland advancements\",\"min_format\":82,\"max_format\":999}}",
                    StandardCharsets.UTF_8);
            Path dir = pack.resolve("data").resolve(plugin.keys().item.getNamespace()).resolve("advancement");
            Files.createDirectories(dir);
            for (Adv a : Adv.values()) Files.writeString(dir.resolve(a.id + ".json"), json(a), StandardCharsets.UTF_8);
            return true;
        } catch (IOException | RuntimeException e) {
            plugin.getLogger().warning("Could not write the Winterland advancements datapack: " + e.getMessage());
            return false;
        }
    }

    private NamespacedKey key(Adv a) {
        return new NamespacedKey(plugin, a.id);
    }

    private String json(Adv a) {
        StringBuilder sb = new StringBuilder("{");
        if (a.parent != null) sb.append("\"parent\":").append(str(key(a.parent).toString())).append(',');
        sb.append("\"display\":{")
                .append("\"icon\":{\"id\":").append(str("minecraft:" + a.icon)).append("},")
                .append("\"title\":{\"text\":").append(str(a.title)).append(",\"color\":\"aqua\"},")
                .append("\"description\":{\"text\":").append(str(a.description)).append("},")
                .append("\"frame\":").append(str(a.frame.name().toLowerCase())).append(',')
                .append("\"show_toast\":true,\"announce_to_chat\":true,")
                .append("\"hidden\":").append(a.hidden);
        if (a.parent == null) sb.append(",\"background\":\"minecraft:block/snow\"");
        sb.append("},\"criteria\":{\"done\":{\"trigger\":\"minecraft:impossible\"}}}");
        return sb.toString();
    }

    private static String str(String s) {
        return '"' + s.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    // ------------------------------------------------------------- granting

    /** Grants an advancement (and the tab's root, so the tab always shows up). */
    public void grant(Player p, Adv a) {
        if (a != Adv.ROOT) grant(p, Adv.ROOT);
        Advancement adv = Bukkit.getAdvancement(key(a));
        if (adv == null) return;
        AdvancementProgress progress = p.getAdvancementProgress(adv);
        if (progress.isDone()) return;
        for (String criterion : List.copyOf(progress.getRemainingCriteria())) progress.awardCriteria(criterion);
    }

    private void checkHats() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            ItemStack helmet = p.getInventory().getHelmet();
            if (plugin.items().typeOf(helmet) == XmasItems.Type.SANTA_HAT) grant(p, Adv.SANTA_HAT);
        }
    }
}
