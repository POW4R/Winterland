package me.slemm.winterland.listeners;

import me.slemm.winterland.Advancements.Adv;
import me.slemm.winterland.Text;
import me.slemm.winterland.WinterlandPlugin;
import me.slemm.winterland.XmasItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Keyed;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Presents, the Snow Globe, the Grinch's Heart and the festive food. */
public final class ItemListener implements Listener {

    private final WinterlandPlugin plugin;
    private final Map<UUID, Long> globeCooldown = new HashMap<>();

    public ItemListener(WinterlandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent event) {
        Action a = event.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;
        EquipmentSlot hand = event.getHand();
        if (hand == null) return;
        ItemStack item = event.getItem();
        XmasItems.Type type = plugin.items().typeOf(item);
        if (type == null) return;

        Player p = event.getPlayer();
        switch (type) {
            case PRESENT -> {
                event.setCancelled(true);
                consumeOne(p, hand);
                openPresent(p);
            }
            case SNOW_GLOBE -> {
                event.setCancelled(true);
                useGlobe(p);
            }
            case GRINCH_HEART -> {
                event.setCancelled(true);
                consumeOne(p, hand);
                p.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 20 * 60 * 10, 2));
                AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
                if (max != null) p.setHealth(Math.min(max.getValue(), p.getHealth() + 12));
                p.getWorld().spawnParticle(Particle.HEART, p.getLocation().add(0, 2, 0), 15, 0.6, 0.4, 0.6);
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.8f);
                p.sendMessage(Text.msg("Your heart grew three sizes that day! (+6 hearts for 10 minutes)", Text.XMAS_GREEN));
                plugin.advancements().grant(p, Adv.THREE_SIZES);
            }
            default -> { /* food is handled on consume, hat is just worn */ }
        }
    }

    private void consumeOne(Player p, EquipmentSlot hand) {
        ItemStack inHand = p.getInventory().getItem(hand);
        if (inHand.getAmount() <= 1) p.getInventory().setItem(hand, null);
        else inHand.setAmount(inHand.getAmount() - 1);
    }

    public void openPresent(Player p) {
        ItemStack loot;
        Long naughty = p.getPersistentDataContainer().get(plugin.keys().naughtyUntil, PersistentDataType.LONG);
        boolean isNaughty = naughty != null && naughty > System.currentTimeMillis();
        if (isNaughty && ThreadLocalRandom.current().nextBoolean()) {
            loot = plugin.items().create(XmasItems.Type.LUMP_OF_COAL);
        } else {
            loot = plugin.items().rollPresent();
        }
        give(p, loot);
        plugin.advancements().grant(p, Adv.UNWRAPPED);

        Location l = p.getLocation().add(0, 1.2, 0);
        p.getWorld().spawnParticle(Particle.FIREWORK, l, 30, 0.4, 0.4, 0.4, 0.05);
        p.getWorld().spawnParticle(Particle.SNOWFLAKE, l, 20, 0.5, 0.5, 0.5, 0.02);
        p.playSound(l, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.8f, 1.2f);
        p.playSound(l, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.5f);
        p.sendMessage(Text.prefix()
                .append(Component.text("You unwrapped ", NamedTextColor.WHITE))
                .append(Component.text(loot.getAmount() + "x ", Text.GOLD))
                .append(loot.displayName())
                .append(Component.text("!", NamedTextColor.WHITE)));
    }

    private void useGlobe(Player p) {
        long now = System.currentTimeMillis();
        long ready = globeCooldown.getOrDefault(p.getUniqueId(), 0L);
        if (now < ready) {
            p.sendActionBar(Component.text("The snow is still settling... (" + ((ready - now) / 1000 + 1) + "s)", Text.ICE));
            return;
        }
        globeCooldown.put(p.getUniqueId(), now + plugin.settings().globeCooldownSeconds() * 1000L);
        p.getWorld().spawnParticle(Particle.SNOWFLAKE, p.getLocation().add(0, 1, 0), 60, 0.6, 1, 0.6, 0.05);
        p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.4f);
        if (plugin.isWinterland(p.getWorld())) plugin.leaveWinterland(p);
        else plugin.sendToWinterland(p);
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        XmasItems.Type type = plugin.items().typeOf(event.getItem());
        if (type == null) return;
        Player p = event.getPlayer();
        switch (type) {
            case GINGERBREAD -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 20 * 30, 1));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 4, 1));
                p.sendActionBar(Component.text("Run, run, as fast as you can! 🍪", Text.GOLD));
                plugin.advancements().grant(p, Adv.GINGERBREAD);
            }
            case HOT_COCOA -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 60, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 8, 0));
                p.setFreezeTicks(0);
                p.sendActionBar(Component.text("Warm and cozy! ☕", Text.COCOA));
                plugin.advancements().grant(p, Adv.HOT_COCOA);
            }
            default -> { }
        }
    }

    /** Stop Christmas items from being used as plain paper/cookies/coal in vanilla recipes. */
    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe() instanceof Keyed k && plugin.items().isOurRecipe(k.getKey())) return;
        CraftingInventory inv = event.getInventory();
        for (ItemStack it : inv.getMatrix()) {
            if (plugin.items().isXmas(it)) {
                inv.setResult(null);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCrafted(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player p
                && plugin.items().typeOf(event.getRecipe().getResult()) == XmasItems.Type.SNOW_GLOBE) {
            plugin.advancements().grant(p, Adv.ROOT);
        }
    }

    public static void give(Player p, ItemStack item) {
        Map<Integer, ItemStack> left = p.getInventory().addItem(item);
        for (ItemStack rest : left.values()) p.getWorld().dropItemNaturally(p.getLocation(), rest);
    }
}
