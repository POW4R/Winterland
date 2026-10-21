package me.slemm.winterland.listeners;

import me.slemm.winterland.Text;
import me.slemm.winterland.WinterlandPlugin;
import me.slemm.winterland.XmasItems;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Chests generated in Winterland (under trees, in gingerbread houses) are "gift chests":
 * the first time anyone opens one it fills with presents and goodies.
 * Chests placed by players are remembered so they never get free loot.
 * In the overworld, only chests placed by the Christmas decorations (under trees, in
 * gingerbread houses) are gifts.
 */
public final class GiftChestListener implements Listener {

    private final WinterlandPlugin plugin;

    public GiftChestListener(WinterlandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlace(BlockPlaceEvent event) {
        Block b = event.getBlockPlaced();
        if (b.getType() != Material.CHEST || !plugin.isWinterland(b.getWorld())) return;
        add(b.getChunk(), plugin.keys().giftPlaced, key(b));
    }

    @EventHandler(ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Inventory inv = event.getInventory();
        InventoryHolder holder = inv.getHolder();
        if (!(holder instanceof Chest chest)) return; // single chests only
        Block block = chest.getBlock();
        Chunk chunk = block.getChunk();
        String k = key(block);
        if (plugin.isWinterland(block.getWorld())) {
            // Every generated chest in Winterland is a gift, unless a player placed it
            if (has(chunk, plugin.keys().giftPlaced, k)) return;
        } else {
            // In other worlds only chests our Christmas decorations placed are gifts
            if (!has(chunk, plugin.keys().giftSpawned, k)) return;
        }
        if (has(chunk, plugin.keys().giftOpened, k)) return;
        add(chunk, plugin.keys().giftOpened, k);
        if (!inv.isEmpty()) return;

        ThreadLocalRandom r = ThreadLocalRandom.current();
        List<ItemStack> loot = new ArrayList<>();
        loot.add(plugin.items().create(XmasItems.Type.PRESENT, 1 + r.nextInt(2)));
        int extras = 2 + r.nextInt(4);
        for (int i = 0; i < extras; i++) loot.add(plugin.items().rollPresent());
        if (r.nextInt(4) == 0) loot.add(new ItemStack(Material.SNOWBALL, 16));

        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inv.getSize(); i++) slots.add(i);
        Collections.shuffle(slots);
        for (int i = 0; i < loot.size() && i < slots.size(); i++) inv.setItem(slots.get(i), loot.get(i));

        player.playSound(block.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.2f);
        player.playSound(block.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.6f);
        block.getWorld().spawnParticle(Particle.FIREWORK, block.getLocation().add(0.5, 1.2, 0.5), 25, 0.3, 0.3, 0.3, 0.05);
        player.sendActionBar(Text.plain("🎁 You found a gift chest!", Text.GOLD));
    }

    private static String key(Block b) {
        return b.getX() + ":" + b.getY() + ":" + b.getZ();
    }

    private static boolean has(Chunk chunk, org.bukkit.NamespacedKey key, String entry) {
        String s = chunk.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (s == null) return false;
        for (String part : s.split(";")) if (part.equals(entry)) return true;
        return false;
    }

    private static void add(Chunk chunk, org.bukkit.NamespacedKey key, String entry) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        String s = pdc.get(key, PersistentDataType.STRING);
        pdc.set(key, PersistentDataType.STRING, s == null || s.isEmpty() ? entry : s + ";" + entry);
    }
}
