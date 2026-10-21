package me.slemm.winterland.listeners;

import me.slemm.winterland.Advancements.Adv;
import me.slemm.winterland.Text;
import me.slemm.winterland.WinterlandPlugin;
import me.slemm.winterland.XmasItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.concurrent.ThreadLocalRandom;

/** Naughty Elves, The Grinch, and Santa's naughty list. */
public final class MobListener implements Listener {

    private final WinterlandPlugin plugin;

    public MobListener(WinterlandPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------- spawning

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType() != EntityType.ZOMBIE) return;
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;
        if (!plugin.isWinterland(event.getLocation().getWorld())) return;
        Zombie z = (Zombie) event.getEntity();
        double r = ThreadLocalRandom.current().nextDouble();
        if (r < plugin.settings().grinchChance()) makeGrinch(z);
        else if (r < plugin.settings().grinchChance() + plugin.settings().elfChance()) makeElf(z);
    }

    /** "Do You Want to Build a Snowman?" goes to whoever is closest to a freshly built Snow Golem. */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBuildGolem(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.BUILD_SNOWMAN) return;
        Player builder = null;
        double best = 8 * 8;
        for (Player p : event.getLocation().getWorld().getPlayers()) {
            double d = p.getLocation().distanceSquared(event.getLocation());
            if (d < best) {
                best = d;
                builder = p;
            }
        }
        if (builder != null) plugin.advancements().grant(builder, Adv.SNOWMAN);
    }

    public void makeElf(Zombie z) {
        z.setBaby();
        z.setCanPickupItems(false);
        z.customName(Component.text("Naughty Elf", Text.XMAS_GREEN));
        z.setCustomNameVisible(false);
        EntityEquipment eq = z.getEquipment();
        eq.setHelmet(leather(Material.LEATHER_HELMET, 0x2E7D32));
        eq.setChestplate(leather(Material.LEATHER_CHESTPLATE, 0x43A047));
        eq.setLeggings(leather(Material.LEATHER_LEGGINGS, 0xC62828));
        eq.setBoots(leather(Material.LEATHER_BOOTS, 0x2E7D32));
        eq.setItemInMainHand(new ItemStack(Material.COOKIE));
        noDrops(eq);
        z.getPersistentDataContainer().set(plugin.keys().mobType, PersistentDataType.STRING, "elf");
    }

    public void makeGrinch(Zombie z) {
        z.setAdult();
        z.setCanPickupItems(false);
        z.customName(Component.text("The Grinch", Text.GRINCH).decorate(TextDecoration.BOLD));
        z.setCustomNameVisible(true);
        EntityEquipment eq = z.getEquipment();
        eq.setHelmet(leather(Material.LEATHER_HELMET, 0xD32F2F)); // stolen Santa hat
        eq.setChestplate(leather(Material.LEATHER_CHESTPLATE, 0x558B2F));
        eq.setLeggings(leather(Material.LEATHER_LEGGINGS, 0x558B2F));
        eq.setBoots(leather(Material.LEATHER_BOOTS, 0x33691E));
        eq.setItemInMainHand(plugin.items().create(XmasItems.Type.PRESENT)); // a stolen present
        noDrops(eq);
        setAttr(z, Attribute.MAX_HEALTH, 80);
        z.setHealth(80);
        setAttr(z, Attribute.MOVEMENT_SPEED, 0.3);
        setAttr(z, Attribute.ATTACK_DAMAGE, 7);
        z.getPersistentDataContainer().set(plugin.keys().mobType, PersistentDataType.STRING, "grinch");

        World w = z.getWorld();
        for (Player p : w.getPlayers()) {
            p.sendMessage(Text.msg("The Grinch has been spotted in Winterland! Get your presents back!", Text.GRINCH));
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.4f, 1.6f);
        }
    }

    private static void setAttr(LivingEntity e, Attribute attr, double value) {
        AttributeInstance inst = e.getAttribute(attr);
        if (inst != null) inst.setBaseValue(value);
    }

    private static ItemStack leather(Material m, int rgb) {
        ItemStack it = new ItemStack(m);
        it.editMeta(LeatherArmorMeta.class, meta -> meta.setColor(Color.fromRGB(rgb)));
        return it;
    }

    private static void noDrops(EntityEquipment eq) {
        eq.setHelmetDropChance(0f);
        eq.setChestplateDropChance(0f);
        eq.setLeggingsDropChance(0f);
        eq.setBootsDropChance(0f);
        eq.setItemInMainHandDropChance(0f);
    }

    // --------------------------------------------------------------- deaths

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        LivingEntity dead = event.getEntity();
        Player killer = dead.getKiller();

        String type = dead.getPersistentDataContainer().get(plugin.keys().mobType, PersistentDataType.STRING);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        if ("elf".equals(type)) {
            if (killer != null) plugin.advancements().grant(killer, Adv.ELF);
            if (r.nextDouble() < 0.3) event.getDrops().add(plugin.items().create(XmasItems.Type.PRESENT));
            if (r.nextDouble() < 0.4) event.getDrops().add(plugin.items().create(XmasItems.Type.GINGERBREAD, 1 + r.nextInt(2)));
        } else if ("grinch".equals(type)) {
            event.getDrops().add(plugin.items().create(XmasItems.Type.PRESENT, 3 + r.nextInt(3)));
            event.getDrops().add(plugin.items().create(XmasItems.Type.GRINCH_HEART));
            event.setDroppedExp(60);
            if (killer != null) plugin.advancements().grant(killer, Adv.GRINCH);
            String who = killer != null ? killer.getName() : "Someone";
            for (Player p : dead.getWorld().getPlayers()) {
                p.sendMessage(Text.msg(who + " stopped the Grinch and saved Christmas! 🎄", Text.GOLD));
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.2f);
            }
        }

        if (killer != null && isNiceCreature(dead) && killer != dead) {
            long until = System.currentTimeMillis() + plugin.settings().naughtyHours() * 3_600_000L;
            killer.getPersistentDataContainer().set(plugin.keys().naughtyUntil, PersistentDataType.LONG, until);
            killer.sendActionBar(Component.text("Santa saw that... you're on the naughty list! 📜", NamedTextColor.DARK_RED));
            plugin.advancements().grant(killer, Adv.NAUGHTY);
        }
    }

    private boolean isNiceCreature(LivingEntity e) {
        if (e instanceof Player) return true;
        if (e instanceof Villager || e instanceof WanderingTrader) return true;
        if (e instanceof Snowman || e instanceof IronGolem || e instanceof Allay || e instanceof Axolotl) return true;
        return e instanceof Tameable t && t.isTamed();
    }
}
