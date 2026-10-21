package me.slemm.winterland;

import me.slemm.winterland.listeners.ItemListener;
import me.slemm.winterland.listeners.SantaListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** /xmas — travel, calendar, leaderboard and admin tools. */
final class XmasCommand implements TabExecutor {

    private final WinterlandPlugin plugin;

    XmasCommand(WinterlandPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "tp", "go" -> {
                Player p = player(sender);
                if (p != null && perm(p, "winterland.tp")) plugin.sendToWinterland(p);
            }
            case "leave", "home" -> {
                Player p = player(sender);
                if (p != null && perm(p, "winterland.tp")) plugin.leaveWinterland(p);
            }
            case "advent" -> {
                Player p = player(sender);
                if (p != null && perm(p, "winterland.advent")) plugin.advent().open(p);
            }
            case "top" -> {
                sender.sendMessage(Text.msg("Snowball fight champions:", Text.ICE));
                var top = plugin.stats().top(10);
                if (top.isEmpty()) sender.sendMessage(Component.text("  Nobody has hit anyone yet. Go throw some snow!", NamedTextColor.GRAY));
                for (int i = 0; i < top.size(); i++) {
                    var e = top.get(i);
                    sender.sendMessage(Component.text("  " + (i + 1) + ". ", Text.GOLD)
                            .append(Component.text(e.name(), NamedTextColor.WHITE))
                            .append(Component.text(" — " + e.hits() + " hits", NamedTextColor.GRAY)));
                }
            }
            case "give" -> give(sender, args);
            case "santa" -> santa(sender, args);
            case "reload" -> {
                if (!perm(sender, "winterland.admin")) return true;
                plugin.reloadSettings();
                sender.sendMessage(Text.msg("Config reloaded.", NamedTextColor.GREEN));
            }
            default -> help(sender);
        }
        return true;
    }

    private void help(CommandSender s) {
        s.sendMessage(Text.msg("Commands:", Text.GOLD));
        line(s, "/xmas tp", "travel to Winterland");
        line(s, "/xmas leave", "go back where you came from");
        line(s, "/advent", "open the advent calendar");
        line(s, "/xmas top", "snowball fight leaderboard");
        if (s.hasPermission("winterland.admin")) {
            line(s, "/xmas give <item> [player] [amount]", "items: " + String.join(", ", itemNames()));
            line(s, "/xmas santa", "place Santa where you stand");
            line(s, "/xmas santa remove", "remove Santa near you");
            line(s, "/xmas reload", "reload config.yml");
        }
    }

    private void line(CommandSender s, String cmd, String desc) {
        s.sendMessage(Component.text("  " + cmd, Text.ICE).append(Component.text(" — " + desc, NamedTextColor.GRAY)));
    }

    private void give(CommandSender sender, String[] args) {
        if (!perm(sender, "winterland.admin")) return;
        if (args.length < 2) {
            sender.sendMessage(Text.msg("Usage: /xmas give <item> [player] [amount]", NamedTextColor.RED));
            return;
        }
        XmasItems.Type type;
        try {
            type = XmasItems.Type.valueOf(args[1].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Text.msg("Unknown item. Try: " + String.join(", ", itemNames()), NamedTextColor.RED));
            return;
        }
        Player target = args.length >= 3 ? Bukkit.getPlayerExact(args[2]) : (sender instanceof Player p ? p : null);
        if (target == null) {
            sender.sendMessage(Text.msg("Player not found.", NamedTextColor.RED));
            return;
        }
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Math.max(1, Math.min(64, Integer.parseInt(args[3])));
            } catch (NumberFormatException ignored) { }
        }
        boolean single = type == XmasItems.Type.SNOW_GLOBE || type == XmasItems.Type.SANTA_HAT;
        if (single) {
            for (int i = 0; i < amount; i++) ItemListener.give(target, plugin.items().create(type));
        } else {
            ItemListener.give(target, plugin.items().create(type, amount));
        }
        sender.sendMessage(Text.msg("Gave " + amount + "x " + type.name().toLowerCase(Locale.ROOT) + " to " + target.getName() + ".", NamedTextColor.GREEN));
    }

    private void santa(CommandSender sender, String[] args) {
        if (!perm(sender, "winterland.admin")) return;
        Player p = player(sender);
        if (p == null) return;
        if (args.length >= 2 && args[1].equalsIgnoreCase("remove")) {
            int removed = 0;
            for (Entity e : p.getNearbyEntities(6, 4, 6)) {
                if (e.getPersistentDataContainer().has(plugin.keys().santa, PersistentDataType.BYTE)) {
                    e.remove();
                    removed++;
                }
            }
            p.sendMessage(Text.msg(removed == 0 ? "No Santa within 6 blocks." : "Santa went back to the North Pole.", NamedTextColor.GRAY));
            return;
        }
        SantaListener.spawnSanta(plugin, p.getLocation());
        p.sendMessage(Text.msg("Santa has arrived! Players can right-click him once a day for a present.", Text.XMAS_GREEN));
    }

    private Player player(CommandSender s) {
        if (s instanceof Player p) return p;
        s.sendMessage(Component.text("Only players can do that.", NamedTextColor.RED));
        return null;
    }

    private boolean perm(CommandSender s, String perm) {
        if (s.hasPermission(perm)) return true;
        s.sendMessage(Text.msg("You don't have permission.", NamedTextColor.RED));
        return false;
    }

    private static List<String> itemNames() {
        List<String> names = new ArrayList<>();
        for (XmasItems.Type t : XmasItems.Type.values()) names.add(t.name().toLowerCase(Locale.ROOT));
        return names;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.addAll(Arrays.asList("tp", "leave", "advent", "top", "help"));
            if (sender.hasPermission("winterland.admin")) options.addAll(Arrays.asList("give", "santa", "reload"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            options.addAll(itemNames());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("santa")) {
            options.add("remove");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            for (Player p : Bukkit.getOnlinePlayers()) options.add(p.getName());
        }
        String last = args[args.length - 1].toLowerCase(Locale.ROOT);
        options.removeIf(o -> !o.toLowerCase(Locale.ROOT).startsWith(last));
        return options;
    }
}
