package me.slemm.winterland.listeners;

import me.slemm.winterland.Text;
import me.slemm.winterland.WinterlandPlugin;
import me.slemm.winterland.XmasItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

/** Santa Claus: right-click him once a day for a present (or coal, if you've been naughty). */
public final class SantaListener implements Listener {

    private static final String[] NICE_LINES = {
            "Ho ho ho! Merry Christmas, %s!",
            "You've been very good this year, %s!",
            "Don't eat all the gingerbread at once, %s!",
            "The elves wrapped this one specially for you, %s!",
            "Rudolph says hi, %s!"
    };

    private final WinterlandPlugin plugin;

    public SantaListener(WinterlandPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isSanta(Entity e) {
        return e.getPersistentDataContainer().has(plugin.keys().santa, PersistentDataType.BYTE);
    }

    /** Spawns Santa at a location (used by /xmas santa). */
    public static Villager spawnSanta(WinterlandPlugin plugin, Location loc) {
        return loc.getWorld().spawn(loc, Villager.class, v -> {
            v.customName(Component.text("Santa Claus", Text.XMAS_RED).decorate(TextDecoration.BOLD));
            v.setCustomNameVisible(true);
            v.setAI(false);
            v.setInvulnerable(true);
            v.setPersistent(true);
            v.setRemoveWhenFarAway(false);
            v.setSilent(false);
            v.setCollidable(false);
            v.getPersistentDataContainer().set(plugin.keys().santa, PersistentDataType.BYTE, (byte) 1);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!isSanta(event.getRightClicked())) return;
        event.setCancelled(true); // no trading menu
        if (event.getHand() != EquipmentSlot.HAND) return;
        visit(event.getPlayer(), event.getRightClicked());
    }

    private void visit(Player p, Entity santa) {
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        String today = LocalDate.now(plugin.zone()).toString();
        Location fx = santa.getLocation().add(0, 2.2, 0);

        if (today.equals(pdc.get(plugin.keys().santaDay, PersistentDataType.STRING))) {
            say(p, "Ho ho ho! You already got your present today, " + p.getName() + ". Come back tomorrow!");
            p.playSound(fx, Sound.ENTITY_VILLAGER_NO, 1f, 0.7f);
            return;
        }
        pdc.set(plugin.keys().santaDay, PersistentDataType.STRING, today);

        Long naughty = pdc.get(plugin.keys().naughtyUntil, PersistentDataType.LONG);
        if (naughty != null && naughty > System.currentTimeMillis()) {
            ItemListener.give(p, plugin.items().create(XmasItems.Type.LUMP_OF_COAL));
            say(p, "Hmm... my list says you've been NAUGHTY, " + p.getName() + ". Coal for you!");
            plugin.advancements().grant(p, me.slemm.winterland.Advancements.Adv.NAUGHTY);
            p.playSound(fx, Sound.ENTITY_VILLAGER_NO, 1f, 0.6f);
            santa.getWorld().spawnParticle(Particle.LARGE_SMOKE, fx, 15, 0.3, 0.3, 0.3, 0.01);
            return;
        }

        ThreadLocalRandom r = ThreadLocalRandom.current();
        ItemListener.give(p, plugin.items().create(XmasItems.Type.PRESENT));
        if (r.nextInt(4) == 0) ItemListener.give(p, plugin.items().create(XmasItems.Type.GINGERBREAD, 3));
        if (r.nextInt(10) == 0) ItemListener.give(p, plugin.items().create(XmasItems.Type.HOT_COCOA));
        say(p, String.format(NICE_LINES[r.nextInt(NICE_LINES.length)], p.getName()));
        plugin.advancements().grant(p, me.slemm.winterland.Advancements.Adv.SANTA);
        p.playSound(fx, Sound.ENTITY_VILLAGER_CELEBRATE, 1f, 0.7f);
        p.playSound(fx, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.0f);
        santa.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, fx, 20, 0.5, 0.5, 0.5);
        santa.getWorld().spawnParticle(Particle.SNOWFLAKE, fx, 30, 0.6, 0.6, 0.6, 0.02);
    }

    private void say(Player p, String line) {
        p.sendMessage(Component.text("Santa", Text.XMAS_RED).decorate(TextDecoration.BOLD)
                .append(Component.text(" » ", NamedTextColor.DARK_GRAY))
                .append(Component.text(line, NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false)));
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (isSanta(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTransform(EntityTransformEvent event) {
        if (isSanta(event.getEntity())) event.setCancelled(true); // no witch/zombie Santa
    }
}
