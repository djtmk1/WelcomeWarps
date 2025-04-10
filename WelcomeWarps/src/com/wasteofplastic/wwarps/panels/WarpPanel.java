package com.wasteofplastic.wwarps.panels;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

import com.wasteofplastic.wwarps.WWarps;

public class WarpPanel implements Listener {
    private final WWarps plugin;
    private final List<Inventory> warpPanel;
    private final Map<UUID, ItemStack> signpostCache;

    public WarpPanel(WWarps plugin) {
        this.plugin = plugin;
        this.warpPanel = new ArrayList<>();
        this.signpostCache = new HashMap<>();
        updatePanel();
    }

    public void updatePanel() {
        warpPanel.clear();
        int panelSize = 45;
        Collection<UUID> warps = plugin.getWarpSignsListener().listSortedWarps();
        int panelNumber = warps.size() / (panelSize - 2);
        int remainder = (warps.size() % (panelSize - 2)) + 8 + 2;
        remainder -= (remainder % 9);

        int i;
        for (i = 0; i < panelNumber; i++) {
            warpPanel.add(Bukkit.createInventory(null, panelSize, plugin.myLocale().warpsTitle + " #" + (i + 1)));
        }
        warpPanel.add(Bukkit.createInventory(null, remainder, plugin.myLocale().warpsTitle + " #" + (i + 1)));

        panelNumber = 0;
        int slot = 0;
        int count = 0;

        for (UUID playerUUID : warps) {
            count++;
            String playerName = plugin.getServer().getOfflinePlayer(playerUUID).getName();
            ItemStack playerSign = new ItemStack(Material.OAK_SIGN);

            if (playerName != null) {
                if (signpostCache.containsKey(playerUUID)) {
                    playerSign = signpostCache.get(playerUUID);
                } else {
                    ItemMeta meta = playerSign.getItemMeta();
                    meta.setDisplayName(playerName);
                    Location signLocation = plugin.getWarpSignsListener().getWarp(playerUUID);
                    if (signLocation != null && Tag.SIGNS.isTagged(signLocation.getBlock().getType())) {
                        Sign sign = (Sign) signLocation.getBlock().getState();
                        meta.setLore(Arrays.asList(sign.getLines()));
                    }
                    playerSign.setItemMeta(meta);
                    signpostCache.put(playerUUID, playerSign);
                }
                CPItem newButton = new CPItem(playerSign, "wwarp " + playerName);
                warpPanel.get(panelNumber).setItem(slot++, newButton.getItem());
            } else {
                ItemMeta meta = playerSign.getItemMeta();
                meta.setDisplayName("#" + count);
                playerSign.setItemMeta(meta);
                warpPanel.get(panelNumber).setItem(slot++, playerSign);
            }

            if (slot == panelSize - 2) {
                if (panelNumber > 0) {
                    warpPanel.get(panelNumber).setItem(slot++, new CPItem(Material.OAK_SIGN, plugin.myLocale().warpsPrevious, "warps " + (panelNumber - 1), "").getItem());
                }
                warpPanel.get(panelNumber).setItem(slot, new CPItem(Material.OAK_SIGN, plugin.myLocale().warpsNext, "warps " + (panelNumber + 1), "").getItem());
                panelNumber++;
                slot = 0;
            }
        }

        if (remainder != 0 && panelNumber > 0) {
            warpPanel.get(panelNumber).setItem(slot++, new CPItem(Material.OAK_SIGN, plugin.myLocale().warpsPrevious, "warps " + (panelNumber - 1), "").getItem());
        }
    }

    public void invalidateCache(UUID playerUUID) {
        signpostCache.remove(playerUUID);
        updatePanel();
    }

    public Inventory getWarpPanel(int panelNumber) {
        if (panelNumber < 0) panelNumber = 0;
        else if (panelNumber > warpPanel.size() - 1) panelNumber = warpPanel.size() - 1;
        return warpPanel.get(panelNumber);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.startsWith(plugin.myLocale().warpsTitle + " #")) return;

        Player player = (Player) event.getWhoClicked();
        event.setCancelled(true);

        if (event.getSlotType() == SlotType.OUTSIDE) {
            player.closeInventory();
            return;
        }

        Inventory inventory = event.getInventory();
        ItemStack clicked = event.getCurrentItem();

        if (event.getRawSlot() >= inventory.getSize() || clicked == null || clicked.getType() == Material.AIR) return;

        int panelNumber;
        try {
            panelNumber = Integer.parseInt(title.substring(title.indexOf('#') + 1)) - 1;
        } catch (Exception e) {
            panelNumber = 0;
        }

        if (!clicked.hasItemMeta()) return;
        ItemMeta meta = clicked.getItemMeta();
        if (!meta.hasDisplayName()) return;

        String command = meta.getDisplayName();
        if (command.equalsIgnoreCase(plugin.myLocale().warpsNext)) {
            player.closeInventory();
            player.performCommand("wwarps " + (panelNumber + 2));
        } else if (command.equalsIgnoreCase(plugin.myLocale().warpsPrevious)) {
            player.closeInventory();
            player.performCommand("wwarps " + panelNumber);
        } else {
            player.closeInventory();
            player.sendMessage(ChatColor.GREEN + plugin.myLocale().warpswarpToPlayersSign.replace("<player>", command));
            player.performCommand("wwarp " + command);
        }
    }
}