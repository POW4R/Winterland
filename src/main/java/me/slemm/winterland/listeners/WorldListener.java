package me.slemm.winterland.listeners;

import me.slemm.winterland.Text;
import me.slemm.winterland.WinterlandPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import java.time.Duration;

/** Welcome title when someone arrives in Winterland. */
public final class WorldListener implements Listener {

    private final WinterlandPlugin plugin;

    public WorldListener(WinterlandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChangeWorld(PlayerChangedWorldEvent event) {
        Player p = event.getPlayer();
        if (plugin.isWinterland(p.getWorld())) {
            p.showTitle(Title.title(
                    Component.text("❄ ", Text.ICE)
                            .append(Component.text("Winter", Text.XMAS_RED))
                            .append(Component.text("land", Text.XMAS_GREEN))
                            .append(Component.text(" ❄", Text.ICE)),
                    Component.text("Merry Christmas, " + p.getName() + "!", NamedTextColor.WHITE),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofSeconds(1))));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.0f);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.5f);
            plugin.advancements().grant(p, me.slemm.winterland.Advancements.Adv.LET_IT_SNOW);
        } else if (plugin.isWinterland(event.getFrom())) {
            p.sendActionBar(Component.text("See you soon in Winterland! ❄", Text.ICE));
        }
    }
}
