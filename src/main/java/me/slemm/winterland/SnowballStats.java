package me.slemm.winterland;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Snowball fight hit counter, saved to plugins/Winterland/stats.yml. */
public final class SnowballStats {

    public record Entry(String name, int hits) {}

    private final WinterlandPlugin plugin;
    private final File file;
    private final YamlConfiguration yaml;
    private boolean dirty;

    public SnowballStats(WinterlandPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> { if (dirty) save(); }, 20L * 300, 20L * 300);
    }

    public int addHit(Player p) {
        String base = "snowball." + p.getUniqueId();
        int hits = yaml.getInt(base + ".hits") + 1;
        yaml.set(base + ".hits", hits);
        yaml.set(base + ".name", p.getName());
        dirty = true;
        return hits;
    }

    public List<Entry> top(int n) {
        List<Entry> list = new ArrayList<>();
        ConfigurationSection sec = yaml.getConfigurationSection("snowball");
        if (sec == null) return list;
        for (String id : sec.getKeys(false)) {
            list.add(new Entry(sec.getString(id + ".name", "?"), sec.getInt(id + ".hits")));
        }
        list.sort(Comparator.comparingInt(Entry::hits).reversed());
        return list.size() > n ? list.subList(0, n) : list;
    }

    public void save() {
        try {
            yaml.save(file);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save stats.yml: " + e.getMessage());
        }
    }
}
