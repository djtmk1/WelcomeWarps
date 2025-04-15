package com.djtmk.wwarps.gui;

import com.djtmk.wwarps.Settings;
import com.djtmk.wwarps.WWarps;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeoutException;

public class GUI implements Listener {
    private final WWarps plugin;
    private final Map<UUID, Inventory> openGUIs;

    public GUI(WWarps plugin) {
        this.plugin = plugin;
        this.openGUIs = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openGUI(Player player) {
        try {
            Collection<UUID> warpUUIDs = plugin.getDatabase().listSortedWarps(5000);
            int size = Math.max(9, ((warpUUIDs.size() + 8) / 9) * 9); // Round up to nearest multiple of 9
            Inventory gui = Bukkit.createInventory(null, size, ChatColor.DARK_GREEN + "Warp List");

            int slot = 0;
            for (UUID uuid : warpUUIDs) {
                String playerName = plugin.getServer().getOfflinePlayer(uuid).getName();
                if (playerName == null) {
                    playerName = "Unknown";
                }
                Location loc = plugin.getDatabase().getWarp(uuid, 5000);
                if (loc == null) {
                    continue;
                }

                ItemStack sign = new ItemStack(Material.OAK_SIGN);
                ItemMeta meta = sign.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.GREEN + playerName + "'s Warp");
                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.GRAY + "World: " + loc.getWorld().getName());
                    lore.add(ChatColor.GRAY + "X: " + loc.getBlockX());
                    lore.add(ChatColor.GRAY + "Y: " + loc.getBlockY());
                    lore.add(ChatColor.GRAY + "Z: " + loc.getBlockZ());
                    lore.add(ChatColor.YELLOW + "Click to teleport!");
                    meta.setLore(lore);
                    sign.setItemMeta(meta);
                }
                gui.setItem(slot++, sign);
            }

            player.openInventory(gui);
            openGUIs.put(player.getUniqueId(), gui);
            if (Settings.debug >= 2) {
                plugin.getLogger().info("GUI opened for " + player.getName() + " with " + warpUUIDs.size() + " warps");
            }
        } catch (SQLException | TimeoutException e) {
            player.sendMessage(ChatColor.RED + "Error accessing database.");
            if (Settings.debug >= 2) {
                plugin.getLogger().severe("Error opening warp list GUI for " + player.getName() + ": " + e.getMessage());
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null || !openGUIs.containsKey(player.getUniqueId()) ||
                !openGUIs.get(player.getUniqueId()).equals(clickedInventory)) {
            return;
        }

        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() != Material.OAK_SIGN) {
            return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return;
        }

        String displayName = ChatColor.stripColor(meta.getDisplayName());
        String targetPlayer = displayName.replace("'s Warp", "");
        if (targetPlayer.equals("Unknown")) {
            player.sendMessage(ChatColor.RED + "Error accessing database.");
            return;
        }

        try {
            UUID targetUUID = plugin.getDatabase().getUUIDByName(targetPlayer);
            if (targetUUID == null) {
                player.sendMessage(ChatColor.RED + "No warp found for player " + targetPlayer + ".");
                return;
            }
            Location warpLoc = plugin.getDatabase().getWarp(targetUUID, 5000);
            if (warpLoc == null) {
                player.sendMessage(ChatColor.RED + "No warp found for player " + targetPlayer + ".");
                return;
            }
            player.teleport(warpLoc);
            player.sendMessage(ChatColor.GREEN + "Warped to " + targetPlayer + "'s warp!");
            player.closeInventory();
            if (Settings.debug >= 1) {
                plugin.getLogger().info("Player " + player.getName() + " warped to " + targetPlayer);
            }
        } catch (SQLException | TimeoutException e) {
            player.sendMessage(ChatColor.RED + "Error accessing database.");
            if (Settings.debug >= 2) {
                plugin.getLogger().severe("Error warping " + player.getName() + " to " + targetPlayer + ": " + e.getMessage());
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        openGUIs.remove(player.getUniqueId());
        if (Settings.debug >= 2) {
            plugin.getLogger().info("GUI closed for " + player.getName());
        }
    }
}