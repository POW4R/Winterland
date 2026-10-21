package me.slemm.winterland;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/** Creates and recognises all custom Christmas items, plus the present loot table. */
public final class XmasItems {

    public enum Type { PRESENT, SNOW_GLOBE, GINGERBREAD, HOT_COCOA, LUMP_OF_COAL, GRINCH_HEART, SANTA_HAT }

    private static final String[] PRESENT_MODELS = {"red_shulker_box", "lime_shulker_box", "light_blue_shulker_box", "yellow_shulker_box"};

    private final WinterlandPlugin plugin;
    private final List<NamespacedKey> recipeKeys = new ArrayList<>();
    private final List<Weighted> presentLoot = new ArrayList<>();
    private double totalWeight;

    public XmasItems(WinterlandPlugin plugin) {
        this.plugin = plugin;
        buildLootTable();
    }

    // ------------------------------------------------------------------ items

    public ItemStack create(Type type) {
        return create(type, 1);
    }

    public ItemStack create(Type type, int amount) {
        ItemStack item;
        switch (type) {
            case PRESENT -> {
                item = new ItemStack(Material.PAPER, amount);
                String model = PRESENT_MODELS[ThreadLocalRandom.current().nextInt(PRESENT_MODELS.length)];
                item.editMeta(m -> {
                    m.setItemModel(NamespacedKey.minecraft(model));
                    m.displayName(Text.plain("🎁 Christmas Present", Text.XMAS_RED).decorate(TextDecoration.BOLD));
                    m.lore(List.of(
                            Text.lore("Wrapped with love (and tape)."),
                            Text.plain("Right-click to unwrap!", Text.GOLD)));
                });
            }
            case SNOW_GLOBE -> {
                item = new ItemStack(Material.PAPER, 1);
                item.editMeta(m -> {
                    m.setItemModel(NamespacedKey.minecraft("light_blue_stained_glass"));
                    m.setMaxStackSize(1);
                    m.setEnchantmentGlintOverride(true);
                    m.displayName(Text.plain("❄ Snow Globe", Text.ICE).decorate(TextDecoration.BOLD));
                    m.lore(List.of(
                            Text.lore("A tiny, swirling world of snow."),
                            Text.lore("Shake it to travel to Winterland,"),
                            Text.lore("shake it again to go home."),
                            Text.plain("Right-click to shake", Text.GOLD)));
                });
            }
            case GINGERBREAD -> {
                item = new ItemStack(Material.COOKIE, amount);
                item.editMeta(m -> {
                    m.displayName(Text.plain("Gingerbread Cookie", Text.GOLD));
                    m.lore(List.of(Text.lore("Run, run, as fast as you can!"),
                            Text.lore("Gives Jump Boost II and a full belly.")));
                });
            }
            case HOT_COCOA -> {
                item = new ItemStack(Material.HONEY_BOTTLE, amount);
                item.editMeta(m -> {
                    m.displayName(Text.plain("Hot Cocoa", Text.COCOA));
                    m.lore(List.of(Text.lore("Warm, chocolatey, marshmallowy."),
                            Text.lore("Thaws you out, gives Speed and Regeneration.")));
                });
            }
            case LUMP_OF_COAL -> {
                item = new ItemStack(Material.COAL, amount);
                item.editMeta(m -> {
                    m.displayName(Text.plain("Lump of Coal", NamedTextColor.DARK_GRAY));
                    m.lore(List.of(Text.lore("Santa says: you've been naughty."),
                            Text.lore("At least it still burns...")));
                });
            }
            case GRINCH_HEART -> {
                item = new ItemStack(Material.PAPER, amount);
                item.editMeta(m -> {
                    m.setItemModel(NamespacedKey.minecraft("heart_of_the_sea"));
                    m.setEnchantmentGlintOverride(true);
                    m.displayName(Text.plain("The Grinch's Heart", Text.XMAS_GREEN).decorate(TextDecoration.BOLD));
                    m.lore(List.of(Text.lore("It was two sizes too small..."),
                            Text.plain("Right-click: your heart grows three sizes!", Text.GOLD)));
                });
            }
            case SANTA_HAT -> {
                item = new ItemStack(Material.LEATHER_HELMET, 1);
                item.editMeta(LeatherArmorMeta.class, m -> {
                    m.setColor(Color.fromRGB(0xD32F2F));
                    m.displayName(Text.plain("Santa Hat", Text.XMAS_RED).decorate(TextDecoration.BOLD));
                    m.lore(List.of(Text.lore("Fluffy white bobble included."),
                            Text.lore("Ho ho ho!")));
                });
            }
            default -> throw new IllegalStateException();
        }
        item.editMeta(m -> m.getPersistentDataContainer().set(plugin.keys().item, PersistentDataType.STRING, type.name()));
        return item;
    }

    public @Nullable Type typeOf(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return null;
        String s = item.getItemMeta().getPersistentDataContainer().get(plugin.keys().item, PersistentDataType.STRING);
        if (s == null) return null;
        try {
            return Type.valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean isXmas(@Nullable ItemStack item) {
        return typeOf(item) != null;
    }

    // ---------------------------------------------------------------- recipes

    public void registerRecipes() {
        ShapedRecipe globe = new ShapedRecipe(plugin.keys().recipe("snow_globe"), create(Type.SNOW_GLOBE));
        globe.shape(" G ", "GDG", "SSS");
        globe.setIngredient('G', Material.GLASS);
        globe.setIngredient('D', Material.DIAMOND);
        globe.setIngredient('S', Material.SNOW_BLOCK);
        add(globe);

        ShapelessRecipe ginger = new ShapelessRecipe(plugin.keys().recipe("gingerbread"), create(Type.GINGERBREAD, 4));
        ginger.addIngredient(2, Material.WHEAT);
        ginger.addIngredient(Material.COCOA_BEANS);
        ginger.addIngredient(Material.SUGAR);
        add(ginger);

        ShapelessRecipe cocoa = new ShapelessRecipe(plugin.keys().recipe("hot_cocoa"), create(Type.HOT_COCOA));
        cocoa.addIngredient(Material.GLASS_BOTTLE);
        cocoa.addIngredient(Material.COCOA_BEANS);
        cocoa.addIngredient(Material.SUGAR);
        cocoa.addIngredient(Material.MILK_BUCKET);
        add(cocoa);

        ShapedRecipe hat = new ShapedRecipe(plugin.keys().recipe("santa_hat"), create(Type.SANTA_HAT));
        hat.shape("  W", "RRR", "R R");
        hat.setIngredient('W', Material.WHITE_WOOL);
        hat.setIngredient('R', Material.RED_WOOL);
        add(hat);
    }

    private void add(org.bukkit.inventory.Recipe recipe) {
        NamespacedKey key = ((org.bukkit.Keyed) recipe).getKey();
        Bukkit.removeRecipe(key);
        Bukkit.addRecipe(recipe);
        recipeKeys.add(key);
    }

    public void unregisterRecipes() {
        for (NamespacedKey key : recipeKeys) Bukkit.removeRecipe(key);
        recipeKeys.clear();
    }

    public boolean isOurRecipe(@Nullable NamespacedKey key) {
        return key != null && recipeKeys.contains(key);
    }

    // ------------------------------------------------------------- loot table

    private record Weighted(double weight, Supplier<ItemStack> item) {}

    private void loot(double weight, Supplier<ItemStack> item) {
        presentLoot.add(new Weighted(weight, item));
        totalWeight += weight;
    }

    private static int rnd(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private void buildLootTable() {
        loot(14, () -> create(Type.GINGERBREAD, rnd(3, 6)));
        loot(10, () -> create(Type.HOT_COCOA, rnd(1, 3)));
        loot(10, () -> new ItemStack(Material.SNOWBALL, 16));
        loot(8, () -> new ItemStack(Material.EMERALD, rnd(3, 8)));
        loot(6, () -> new ItemStack(Material.DIAMOND, rnd(1, 3)));
        loot(6, () -> new ItemStack(Material.GOLDEN_APPLE, 2));
        loot(8, () -> new ItemStack(Material.FIREWORK_ROCKET, rnd(4, 12)));
        loot(6, () -> new ItemStack(Material.SPRUCE_SAPLING, 4));
        loot(6, () -> {
            Material[] lights = {Material.OCHRE_FROGLIGHT, Material.VERDANT_FROGLIGHT, Material.PEARLESCENT_FROGLIGHT};
            return new ItemStack(lights[rnd(0, 2)], 8);
        });
        loot(6, () -> new ItemStack(Material.EXPERIENCE_BOTTLE, rnd(6, 12)));
        loot(5, () -> new ItemStack(Material.GOLDEN_CARROT, 8));
        loot(5, this::randomBook);
        loot(3, () -> create(Type.SANTA_HAT));
        loot(1.5, () -> new ItemStack(Material.TOTEM_OF_UNDYING));
        loot(1, () -> new ItemStack(Material.NETHERITE_SCRAP));
        loot(0.5, () -> new ItemStack(Material.ENCHANTED_GOLDEN_APPLE));
    }

    private ItemStack randomBook() {
        Enchantment[] pool = {Enchantment.MENDING, Enchantment.UNBREAKING, Enchantment.EFFICIENCY, Enchantment.FORTUNE,
                Enchantment.SHARPNESS, Enchantment.PROTECTION, Enchantment.FEATHER_FALLING, Enchantment.LOOTING};
        Enchantment e = pool[rnd(0, pool.length - 1)];
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        book.editMeta(EnchantmentStorageMeta.class, m -> m.addStoredEnchant(e, Math.min(e.getMaxLevel(), rnd(1, 3)), true));
        return book;
    }

    /** One random thing out of a present. */
    public ItemStack rollPresent() {
        double r = ThreadLocalRandom.current().nextDouble() * totalWeight;
        for (Weighted w : presentLoot) {
            r -= w.weight();
            if (r <= 0) return w.item().get();
        }
        return presentLoot.get(0).item().get();
    }
}
