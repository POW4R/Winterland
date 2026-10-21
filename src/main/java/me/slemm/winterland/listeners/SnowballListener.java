package me.slemm.winterland.listeners;

import me.slemm.winterland.Text;
import me.slemm.winterland.WinterlandPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/** Snowball fights: knockback, a brief chill, and a hit counter for /xmas top. */
public final class SnowballListener implements Listener {

    private final WinterlandPlugin plugin;

    public SnowballListener(WinterlandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball ball)) return;
        if (!(ball.getShooter() instanceof Player shooter)) return;
        if (!(event.getHitEntity() instanceof Player target) || target.equals(shooter)) return;
        if (!plugin.settings().snowballEverywhere()
                && !(plugin.isWinterland(shooter.getWorld()) && plugin.isWinterland(target.getWorld()))) return;

        Vector push = ball.getVelocity().clone().setY(0);
        if (push.lengthSquared() > 0.0001) push.normalize().multiply(plugin.settings().snowballKnockback());
        push.setY(0.22);
        target.setVelocity(target.getVelocity().add(push));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 0, false, false, true));
        int freeze = Math.min(target.getMaxFreezeTicks() - 1, target.getFreezeTicks() + 50);
        target.setFreezeTicks(Math.max(0, freeze));
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 0.8f, 1.2f);
        shooter.playSound(shooter.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.6f);

        int hits = plugin.stats().addHit(shooter);
        shooter.sendActionBar(Component.text("❄ Hit " + target.getName() + "! (" + hits + " total)", Text.ICE));
    }
}
