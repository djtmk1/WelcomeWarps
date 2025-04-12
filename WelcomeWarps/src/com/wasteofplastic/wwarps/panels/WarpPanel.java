package com.wasteofplastic.wwarps.panels;

import com.wasteofplastic.wwarps.WWarps;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WarpPanel implements Listener {
    private final WWarps plugin;
    private final Map<Integer, List<CPItem>> controlPanels = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> playerPanel = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> cache = new ConcurrentHashMap<>();

    public WarpPanel(WWarps plugin) {
        this.plugin = plugin;
    }

    public void invalidateCache(UUID uuid) {
        cache.remove(uuid);
        controlPanels.clear();
    }

    private ItemStack getPlayerHead(UUID owner, Location loc) {
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
        String name = plugin.getServer().getOfflinePlayer(owner).getName();
        if (name == null) {
            name = owner.toString();
        }
        meta.setDisplayName(ChatColor.YELLOW + name);
        List<String> lores = new ArrayList<>();
        lores.add(ChatColor.AQUA + plugin.myLocale().warpsPlayer + ": " + name);
        if (loc.getWorld() != null) {
            lores.add(ChatColor.AQUA + plugin.myLocale().warpsWorld + ": " + loc.getWorld().getName());
        }
        lores.add(ChatColor.AQUA + plugin.myLocale().warpsX + ": " + loc.getBlockX());
        lores.add(ChatColor.AQUA + plugin.myLocale().warpsY + ": " + loc.getBlockY());
        lores.add(ChatColor.AQUA + plugin.myLocale().warpsZ + ": " + loc.getBlockZ());
        Block block = loc.getBlock();
        if (block.getState() instanceof Sign) {
            Sign sign = (Sign) block.getState();
            String[] lines = sign.getSide(Side.FRONT).getLines();
            for (int i = 1; i < lines.length; i++) {
                if (!lines[i].isEmpty()) {
                    lores.add(ChatColor.RESET + lines[i]);
                }
            }
        }
        meta.setLore(lores);
        meta.setOwningPlayer(plugin.getServer().getOfflinePlayer(owner));
        playerHead.setItemMeta(meta);
        return playerHead;
    }

    private ItemStack getNextPage() {
        ItemStack forward = new ItemStack(Material.PAPER, 1);
        CPItem cp = new CPItem(forward, ChatColor.GREEN + plugin.myLocale().warpsNext, "next");
        return cp.getItem();
    }

    private ItemStack getPrevPage() {
        ItemStack back = new ItemStack(Material.PAPER, 1);
        CPItem cp = new CPItem(back, ChatColor.GREEN + plugin.myLocale().warpsPrevious, "previous");
        return cp.getItem();
    }

    private void populatePanel(int panelNum) {
        if (!controlPanels.containsKey(panelNum)) {
            List<CPItem> panel = new ArrayList<>();
            Collection<UUID> sortedWarps = plugin.getWarpSignsListener().listSortedWarps();
            int index = panelNum * 45;
            Iterator<UUID> it = sortedWarps.iterator();
            for (int i = 0; i < index && it.hasNext(); i++) {
                it.next();
            }
            for (int i = 0; i < 45 && it.hasNext(); i++) {
                UUID owner = it.next();
                Location loc = plugin.getWarpSignsListener().getWarp(owner);
                if (loc != null) {
                    ItemStack playerHead = getPlayerHead(owner, loc);
                    panel.add(new CPItem(playerHead, null, "warp", owner));
                    cache.put(owner, owner);
                }
            }
            if (panelNum > 0) {
                panel.add(new CPItem(getPrevPage(), null, "previous"));
            }
            if (it.hasNext()) {
                panel.add(new CPItem(getNextPage(), null, "next"));
            }
            controlPanels.put(panelNum, panel);
        }
    }

    public Inventory getWarpPanel(int panelNum) {
        populatePanel(panelNum);
        Inventory controlPanel = Bukkit.createInventory(null, 54, ChatColor.DARK_BLUE + plugin.myLocale().warpsTitle);
        List<CPItem> panel = controlPanels.get(panelNum);
        for (CPItem item : panel) {
            controlPanel.addItem(item.getItem());
        }
        return controlPanel;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) e.getWhoClicked();
        UUID playerUUID = player.getUniqueId();
        if (!playerPanel.containsKey(playerUUID)) {
            return;
        }
        e.setCancelled(true);
        if (e.getCurrentItem() == null || e.getCurrentItem().getItemMeta() == null) {
            return;
        }
        int panelNum = playerPanel.get(playerUUID);
        List<CPItem> panel = controlPanels.get(panelNum);
        if (panel == null) {
            return;
        }
        for (CPItem cp : panel) {
            if (cp.getItem().equals(e.getCurrentItem())) {
                if (cp.getAction().equals("warp") && cp.getWarp() != null) {
                    player.closeInventory();
                    player.performCommand("wwarp " + plugin.getServer().getOfflinePlayer(cp.getWarp()).getName());
                } else if (cp.getAction().equals("next")) {
                    playerPanel.put(playerUUID, panelNum + 1);
                    player.openInventory(getWarpPanel(panelNum + 1));
                } else if (cp.getAction().equals("previous")) {
                    playerPanel.put(playerUUID, panelNum - 1);
                    player.openInventory(getWarpPanel(panelNum - 1));
                }
                break;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player) {
            playerPanel.remove(e.getPlayer().getUniqueId());
        }
    }
}