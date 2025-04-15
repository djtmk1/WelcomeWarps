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
    private final Map<UUID, Inventory> openGUIs = new HashMap<>();
    private final Map<UUID, Integer> playerPages = new HashMap<>();
    private static final int ITEMS_PER_PAGE = 45; // 5 rows, leaving 1 row for navigation
    private static final int PAGE_SIZE = 54; // 6 rows total

    public GUI(WWarps plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openGUI(Player player, int page) {
        try {
            List<UUID> warpUUIDs = new ArrayList<>(plugin.getDatabase().listSortedWarps(5000));
            if (warpUUIDs.isEmpty()) {
                player.sendMessage(ChatColor.RED + "No warps available.");
                return;
            }

            // Filter valid warps (where location is not null)
            List<UUID> validWarps = new ArrayList<>();
            for (UUID uuid : warpUUIDs) {
                Location loc = plugin.getDatabase().getWarp(uuid, 5000);
                if (loc != null) {
                    validWarps.add(uuid);
                }
            }

            if (validWarps.isEmpty()) {
                player.sendMessage(ChatColor.RED + "No valid warps found.");
                return;
            }

            // Calculate pagination based on valid warps
            int totalPages = (int) Math.ceil((double) validWarps.size() / ITEMS_PER_PAGE);
            page = Math.max(0, Math.min(page, totalPages - 1)); // Clamp page number
            playerPages.put(player.getUniqueId(), page);

            // Create inventory
            Inventory gui = Bukkit.createInventory(null, PAGE_SIZE, ChatColor.DARK_GREEN + "Warp List (Page " + (page + 1) + "/" + totalPages + ")");
            int startIndex = page * ITEMS_PER_PAGE;
            int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, validWarps.size());

            // Populate warps
            int slot = 0;
            for (int i = startIndex; i < endIndex; i++) {
                UUID uuid = validWarps.get(i);
                String playerName = plugin.getServer().getOfflinePlayer(uuid).getName();
                if (playerName == null) {
                    playerName = "Unknown";
                }
                Location loc = plugin.getDatabase().getWarp(uuid, 5000);
                if (loc == null) {
                    continue; // Shouldn't happen, but just in case
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

            // Add placeholder for empty slots
            ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta fillerMeta = filler.getItemMeta();
            if (fillerMeta != null) {
                fillerMeta.setDisplayName(ChatColor.GRAY + " ");
                filler.setItemMeta(fillerMeta);
            }
            for (int i = slot; i < ITEMS_PER_PAGE; i++) {
                gui.setItem(i, filler);
            }

            // Add navigation arrows
            if (page > 0) {
                ItemStack prevArrow = new ItemStack(Material.ARROW);
                ItemMeta prevMeta = prevArrow.getItemMeta();
                if (prevMeta != null) {
                    prevMeta.setDisplayName(ChatColor.YELLOW + "Previous Page");
                    prevArrow.setItemMeta(prevMeta);
                }
                gui.setItem(PAGE_SIZE - 2, prevArrow); // Slot 52
            }
            if (page < totalPages - 1) {
                ItemStack nextArrow = new ItemStack(Material.ARROW);
                ItemMeta nextMeta = nextArrow.getItemMeta();
                if (nextMeta != null) {
                    nextMeta.setDisplayName(ChatColor.YELLOW + "Next Page");
                    nextArrow.setItemMeta(nextMeta);
                }
                gui.setItem(PAGE_SIZE - 1, nextArrow); // Slot 53
            }

            player.openInventory(gui);
            openGUIs.put(player.getUniqueId(), gui);
            if (Settings.debug >= 2) {
                plugin.getLogger().info("GUI opened for " + player.getName() + " with " + validWarps.size() + " valid warps on page " + (page + 1) + "/" + totalPages);
            }
        } catch (SQLException | TimeoutException e) {
            player.sendMessage(ChatColor.RED + "Error accessing database.");
            if (Settings.debug >= 2) {
                plugin.getLogger().severe("Error opening warp list GUI for " + player.getName() + ": " + e.getMessage());
            }
        }
    }

    public void openGUI(Player player) {
        openGUI(player, 0); // Default to first page
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        Inventory openGUI = openGUIs.get(player.getUniqueId());

        if (clickedInventory == null || openGUI == null || !clickedInventory.equals(openGUI)) {
            return;
        }

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        int slot = event.getSlot();
        int currentPage = playerPages.getOrDefault(player.getUniqueId(), 0);

        // Handle navigation arrows
        if (slot == PAGE_SIZE - 2 && clickedItem.getType() == Material.ARROW && clickedItem.getItemMeta() != null && clickedItem.getItemMeta().getDisplayName().contains("Previous Page")) {
            openGUI(player, currentPage - 1);
            return;
        }
        if (slot == PAGE_SIZE - 1 && clickedItem.getType() == Material.ARROW && clickedItem.getItemMeta() != null && clickedItem.getItemMeta().getDisplayName().contains("Next Page")) {
            openGUI(player, currentPage + 1);
            return;
        }

        // Handle warp teleportation
        if (clickedItem.getType() == Material.OAK_SIGN) {
            String displayName = clickedItem.getItemMeta() != null ? clickedItem.getItemMeta().getDisplayName() : "";
            String playerName = ChatColor.stripColor(displayName).replace("'s Warp", "");
            try {
                UUID targetUUID = plugin.getDatabase().getUUIDByName(playerName);
                if (targetUUID == null) {
                    player.sendMessage(ChatColor.RED + "Player not found.");
                    return;
                }
                Location loc = plugin.getDatabase().getWarp(targetUUID, 5000);
                if (loc == null) {
                    player.sendMessage(ChatColor.RED + plugin.myLocale().warpserrorDoesNotExist);
                    return;
                }
                player.teleport(loc);
                plugin.getDatabase().updateLastUsed(targetUUID, 5000);
                player.sendMessage(ChatColor.GREEN + "Warped to " + playerName + "'s warp!");
                player.closeInventory();
                if (Settings.debug >= 1) {
                    plugin.getLogger().info("Player " + player.getName() + " warped to " + playerName + " via GUI");
                }
            } catch (SQLException | TimeoutException e) {
                player.sendMessage(ChatColor.RED + "Error accessing database.");
                if (Settings.debug >= 2) {
                    plugin.getLogger().severe("Error warping " + player.getName() + " to " + playerName + ": " + e.getMessage());
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        openGUIs.remove(player.getUniqueId());
        playerPages.remove(player.getUniqueId());
    }
}