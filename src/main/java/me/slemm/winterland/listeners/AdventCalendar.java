package me.slemm.winterland.listeners;

import me.slemm.winterland.Text;
import me.slemm.winterland.WinterlandPlugin;
import me.slemm.winterland.XmasItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** /advent — 24 doors, one per day of December. */
public final class AdventCalendar implements Listener {

    private static final int SIZE = 27;
    private final WinterlandPlugin plugin;

    public AdventCalendar(WinterlandPlugin plugin) {
        this.plugin = plugin;
    }

    /** Inventory holder that marks our GUI. */
    private static final class Holder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    // ------------------------------------------------------------ date logic

    /** Today, honouring advent.test-day. */
    private LocalDate today() {
        LocalDate real = LocalDate.now(plugin.zone());
        int test = plugin.settings().adventTestDay();
        if (test >= 1 && test <= 31) return LocalDate.of(real.getYear(), Month.DECEMBER, test);
        return real;
    }

    private boolean isAdventSeason(LocalDate d) {
        return d.getMonth() == Month.DECEMBER;
    }

    private NamespacedKey key(LocalDate d) {
        return plugin.keys().advent(d.getYear());
    }

    private Set<Integer> opened(Player p, LocalDate d) {
        Set<Integer> set = new HashSet<>();
        String s = p.getPersistentDataContainer().get(key(d), PersistentDataType.STRING);
        if (s == null || s.isBlank()) return set;
        for (String part : s.split(",")) {
            try {
                set.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignored) { }
        }
        return set;
    }

    private void markOpened(Player p, LocalDate d, int day) {
        Set<Integer> set = opened(p, d);
        set.add(day);
        StringBuilder sb = new StringBuilder();
        for (int i : set) {
            if (!sb.isEmpty()) sb.append(',');
            sb.append(i);
        }
        p.getPersistentDataContainer().set(key(d), PersistentDataType.STRING, sb.toString());
    }

    private boolean canOpen(int day, int todayDay, Set<Integer> opened) {
        if (opened.contains(day)) return false;
        if (day == todayDay) return true;
        return day < todayDay && plugin.settings().adventCatchUp();
    }

    // ------------------------------------------------------------------- GUI

    public void open(Player p) {
        LocalDate today = today();
        if (!isAdventSeason(today)) {
            LocalDate dec1 = LocalDate.of(today.getYear(), Month.DECEMBER, 1);
            long days = ChronoUnit.DAYS.between(today, dec1);
            p.sendMessage(Text.msg("The Advent Calendar opens on December 1st — " + days + " day"
                    + (days == 1 ? "" : "s") + " to go! 🎄", Text.GOLD));
            return;
        }
        Holder holder = new Holder();
        Inventory inv = Bukkit.createInventory(holder, SIZE,
                Component.text("🎄 Advent Calendar " + today.getYear(), Text.XMAS_GREEN));
        holder.inventory = inv;
        render(p, inv, today);
        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 1.2f);
    }

    private void render(Player p, Inventory inv, LocalDate today) {
        int todayDay = today.getDayOfMonth();
        Set<Integer> opened = opened(p, today);
        for (int day = 1; day <= 24; day++) inv.setItem(day - 1, door(day, todayDay, opened));

        inv.setItem(24, info(Material.BOOK, "How it works", Text.GOLD,
                "Open one door every day of December.",
                plugin.settings().adventCatchUp() ? "Missed a day? You can still catch up." : "Missed doors stay closed!",
                "Door 24 is extra special..."));
        inv.setItem(25, info(Material.NETHER_STAR, "Merry Christmas!", Text.XMAS_RED, "From everyone on the server ❤"));
        long toXmas = ChronoUnit.DAYS.between(today, LocalDate.of(today.getYear(), Month.DECEMBER, 25));
        inv.setItem(26, info(Material.CLOCK, toXmas > 0 ? toXmas + " sleep" + (toXmas == 1 ? "" : "s") + " until Christmas" : "It's Christmas!",
                Text.ICE, opened.size() + "/24 doors opened"));
    }

    private ItemStack door(int day, int todayDay, Set<Integer> opened) {
        ItemStack it;
        if (opened.contains(day)) {
            it = new ItemStack(Material.LIME_STAINED_GLASS_PANE, day);
            it.editMeta(m -> {
                m.displayName(Text.plain("Day " + day + " ✔", Text.XMAS_GREEN));
                m.lore(List.of(Text.lore("Already opened.")));
            });
        } else if (canOpen(day, todayDay, opened)) {
            it = new ItemStack(Material.PAPER, day);
            boolean isToday = day == todayDay;
            it.editMeta(m -> {
                m.setItemModel(NamespacedKey.minecraft(day == 24 ? "yellow_shulker_box" : "red_shulker_box"));
                m.setEnchantmentGlintOverride(isToday);
                m.displayName(Text.plain("Day " + day + (isToday ? " — open me!" : " — missed, but still here!"), Text.GOLD)
                        .decorate(TextDecoration.BOLD));
                m.lore(List.of(Text.plain("Click to open!", NamedTextColor.YELLOW)));
            });
        } else if (day < todayDay) {
            it = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, day);
            it.editMeta(m -> m.displayName(Text.plain("Day " + day + " — missed", NamedTextColor.GRAY)));
        } else {
            it = new ItemStack(Material.RED_STAINED_GLASS_PANE, day);
            it.editMeta(m -> {
                m.displayName(Text.plain("Day " + day + " 🔒", Text.XMAS_RED));
                m.lore(List.of(Text.lore("Opens on December " + day + ".")));
            });
        }
        return it;
    }

    private static ItemStack info(Material m, String name, net.kyori.adventure.text.format.TextColor color, String... lore) {
        ItemStack it = new ItemStack(m);
        it.editMeta(meta -> {
            meta.displayName(Text.plain(name, color).decorate(TextDecoration.BOLD));
            List<Component> l = new ArrayList<>();
            for (String s : lore) l.add(Text.lore(s));
            meta.lore(l);
        });
        return it;
    }

    // ---------------------------------------------------------------- clicks

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player p)) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 24) return;

        int day = slot + 1;
        LocalDate today = today();
        if (!isAdventSeason(today)) return;
        Set<Integer> opened = opened(p, today);
        if (!canOpen(day, today.getDayOfMonth(), opened)) {
            p.playSound(p.getLocation(), Sound.BLOCK_CHEST_LOCKED, 0.8f, 1f);
            return;
        }
        markOpened(p, today, day);
        for (ItemStack reward : rewards(day)) ItemListener.give(p, reward);

        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.2f);
        p.playSound(p.getLocation(), day == 24 ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.4f);
        p.sendMessage(Text.msg("You opened door " + day + " of the Advent Calendar! 🎁", Text.GOLD));
        render(p, event.getView().getTopInventory(), today);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Holder) event.setCancelled(true);
    }

    // --------------------------------------------------------------- rewards

    private List<ItemStack> rewards(int day) {
        XmasItems items = plugin.items();
        List<ItemStack> list = new ArrayList<>();
        list.add(items.create(XmasItems.Type.PRESENT));
        switch (day) {
            case 1 -> list.add(new ItemStack(Material.SNOWBALL, 16));
            case 2 -> list.add(items.create(XmasItems.Type.GINGERBREAD, 4));
            case 3 -> list.add(items.create(XmasItems.Type.HOT_COCOA, 2));
            case 4 -> list.add(new ItemStack(Material.OCHRE_FROGLIGHT, 8));
            case 5 -> list.add(new ItemStack(Material.SPRUCE_SAPLING, 8));
            case 6 -> list.add(items.create(XmasItems.Type.SANTA_HAT)); // St. Nicholas' Day
            case 7 -> list.add(new ItemStack(Material.EMERALD, 6));
            case 8 -> list.add(new ItemStack(Material.VERDANT_FROGLIGHT, 8));
            case 9 -> list.add(new ItemStack(Material.GOLDEN_CARROT, 12));
            case 10 -> list.add(new ItemStack(Material.EXPERIENCE_BOTTLE, 12));
            case 11 -> list.add(new ItemStack(Material.FIREWORK_ROCKET, 16));
            case 12 -> list.add(new ItemStack(Material.DIAMOND, 2));
            case 13 -> list.add(new ItemStack(Material.PEARLESCENT_FROGLIGHT, 8));
            case 14 -> list.add(items.create(XmasItems.Type.GINGERBREAD, 8));
            case 15 -> list.add(new ItemStack(Material.GOLDEN_APPLE, 2));
            case 16 -> list.add(new ItemStack(Material.SNOW_BLOCK, 32));
            case 17 -> list.add(items.create(XmasItems.Type.HOT_COCOA, 3));
            case 18 -> list.add(new ItemStack(Material.EMERALD_BLOCK, 1));
            case 19 -> list.add(items.rollPresent());
            case 20 -> list.add(new ItemStack(Material.DIAMOND, 3));
            case 21 -> list.add(new ItemStack(Material.LANTERN, 16)); // longest night
            case 22 -> list.add(items.create(XmasItems.Type.PRESENT, 2));
            case 23 -> list.add(new ItemStack(Material.TOTEM_OF_UNDYING));
            case 24 -> {
                list.add(items.create(XmasItems.Type.PRESENT, 4));
                list.add(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE));
                list.add(new ItemStack(Material.DIAMOND_BLOCK));
                list.add(new ItemStack(Material.FIREWORK_ROCKET, 32));
            }
            default -> { }
        }
        return list;
    }
}
