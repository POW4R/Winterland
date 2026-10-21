package me.slemm.winterland;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

/** Small helpers for Adventure text. */
public final class Text {
    private Text() {}

    public static final TextColor XMAS_RED = TextColor.color(0xE53935);
    public static final TextColor XMAS_GREEN = TextColor.color(0x43A047);
    public static final TextColor GOLD = TextColor.color(0xFFC107);
    public static final TextColor ICE = TextColor.color(0x81D4FA);
    public static final TextColor GRINCH = TextColor.color(0x7CB342);
    public static final TextColor COCOA = TextColor.color(0xA1887F);

    /** Non-italic text (item names/lore are italic by default). */
    public static Component plain(String s, TextColor color) {
        return Component.text(s, color).decoration(TextDecoration.ITALIC, false);
    }

    public static Component lore(String s) {
        return plain(s, NamedTextColor.GRAY);
    }

    /** "[❄ Winterland]" chat prefix. */
    public static Component prefix() {
        return Component.text("[", NamedTextColor.DARK_GRAY)
                .append(Component.text("❄ ", ICE))
                .append(Component.text("Winter", XMAS_RED))
                .append(Component.text("land", XMAS_GREEN))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY));
    }

    public static Component msg(String s, TextColor color) {
        return prefix().append(Component.text(s, color));
    }
}
