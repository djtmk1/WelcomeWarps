package com.wasteofplastic.wwarps.commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.wasteofplastic.wwarps.Settings;
import com.wasteofplastic.wwarps.WWarps;
import com.wasteofplastic.wwarps.util.Util;

public class WarpCmd implements CommandExecutor, TabCompleter {
    private final WWarps plugin;
    private Sound batTakeOff;

    public WarpCmd(WWarps plugin) {
        this.plugin = plugin;
        this.batTakeOff = Sound.ENTITY_BAT_TAKEOFF;
    }

    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (value.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] split) {
        if (!(sender instanceof Player)) return false;
        final Player player = (Player) sender;
        final UUID playerUUID = player.getUniqueId();

        if (label.equalsIgnoreCase("wwarps")) {
            if (!player.hasPermission(Settings.PERMPREFIX + "use")) {
                player.sendMessage(ChatColor.RED + plugin.myLocale().errorNoPermission);
                return true;
            }
            Set<UUID> warpList = plugin.getWarpSignsListener().listWarps();
            if (warpList.isEmpty()) {
                player.sendMessage(ChatColor.YELLOW + plugin.myLocale().warpserrorNoWarpsYet);
                player.sendMessage(ChatColor.YELLOW + plugin.myLocale().warpswarpTip);
                return true;
            }
            if (Settings.useWarpPanel) {
                int panelNum;
                try {
                    panelNum = Integer.parseInt(split[0]) - 1;
                } catch (Exception e) {
                    panelNum = 0;
                }
                player.openInventory(plugin.getWarpPanel().getWarpPanel(panelNum));
                return true;
            } else {
                StringBuilder wlist = new StringBuilder();
                boolean hasWarp = false;
                for (UUID w : warpList) {
                    String name = plugin.getServer().getOfflinePlayer(w).getName();
                    if (wlist.length() == 0) wlist.append(name);
                    else wlist.append(", ").append(name);
                    if (w.equals(playerUUID)) hasWarp = true;
                }
                player.sendMessage(ChatColor.YELLOW + plugin.myLocale().warpswarpsAvailable + ": " + ChatColor.WHITE + wlist);
                if (!hasWarp && player.hasPermission(Settings.PERMPREFIX + "addwarp")) {
                    player.sendMessage(ChatColor.YELLOW + plugin.myLocale().warpswarpTip);
                }
                return true;
            }
        }

        if (label.equalsIgnoreCase("wwarp")) {
            if (split.length != 1) {
                player.performCommand("wwarps");
                return true;
            }
            if (!player.hasPermission(Settings.PERMPREFIX + "use")) {
                player.sendMessage(ChatColor.RED + plugin.myLocale().errorNoPermission);
                return true;
            }
            Set<UUID> warpList = plugin.getWarpSignsListener().listWarps();
            if (warpList.isEmpty()) {
                player.sendMessage(ChatColor.YELLOW + plugin.myLocale().warpserrorNoWarpsYet);
                if (player.hasPermission(Settings.PERMPREFIX + "add")) {
                    player.sendMessage(ChatColor.YELLOW + plugin.myLocale().warpswarpTip);
                }
                return true;
            }
            UUID foundWarp = null;
            for (UUID warp : warpList) {
                String name = plugin.getServer().getOfflinePlayer(warp).getName();
                if (name != null && name.toLowerCase().startsWith(split[0].toLowerCase())) {
                    foundWarp = warp;
                    break;
                }
            }
            if (foundWarp == null) {
                player.sendMessage(ChatColor.RED + plugin.myLocale().warpserrorDoesNotExist);
                return true;
            }
            Location warpSpot = plugin.getWarpSignsListener().getWarp(foundWarp);
            if (warpSpot == null) {
                player.sendMessage(ChatColor.RED + plugin.myLocale().warpserrorNotReadyYet);
                plugin.getLogger().warning("Null warp found, owned by " + plugin.getServer().getOfflinePlayer(foundWarp).getName());
                return true;
            }
            Block b = warpSpot.getBlock();
            if (Tag.SIGNS.isTagged(b.getType())) {
                Sign sign = (Sign) b.getState();
                BlockFace directionFacing = ((org.bukkit.block.data.type.Sign) sign.getBlockData()).getRotation();
                Location inFront = b.getRelative(directionFacing).getLocation();
                Location oneDown = b.getRelative(directionFacing).getRelative(BlockFace.DOWN).getLocation();
                if (WWarps.isSafeLocation(inFront)) {
                    warpPlayer(player, inFront, foundWarp, directionFacing);
                    return true;
                } else if (Tag.SIGNS.isTagged(b.getType()) && WWarps.isSafeLocation(oneDown)) {
                    warpPlayer(player, oneDown, foundWarp, directionFacing);
                    return true;
                }
            } else {
                player.sendMessage(ChatColor.RED + plugin.myLocale().warpserrorDoesNotExist);
                plugin.getWarpSignsListener().removeWarp(warpSpot);
                return true;
            }
            if (!WWarps.isSafeLocation(warpSpot)) {
                player.sendMessage(ChatColor.RED + plugin.myLocale().warpserrorNotSafe);
                if (Tag.SIGNS.isTagged(b.getType())) {
                    plugin.getLogger().warning("Unsafe warp found at " + warpSpot.toString() + " owned by " + plugin.getServer().getOfflinePlayer(foundWarp).getName());
                }
                return true;
            }
            Location actualWarp = new Location(warpSpot.getWorld(), warpSpot.getBlockX() + 0.5, warpSpot.getBlockY(), warpSpot.getBlockZ() + 0.5);
            player.teleport(actualWarp);
            player.getWorld().playSound(player.getLocation(), batTakeOff, 1F, 1F);
            return true;
        }
        return false;
    }

    private void warpPlayer(Player player, Location inFront, UUID foundWarp, BlockFace directionFacing) {
        float yaw = Util.blockFaceToFloat(directionFacing);
        Location actualWarp = new Location(inFront.getWorld(), inFront.getBlockX() + 0.5, inFront.getBlockY(), inFront.getBlockZ() + 0.5, yaw, 30F);
        player.teleport(actualWarp);
        player.getWorld().playSound(player.getLocation(), batTakeOff, 1F, 1F);
        Player warpOwner = plugin.getServer().getPlayer(foundWarp);
        if (warpOwner != null && !warpOwner.equals(player)) {
            warpOwner.sendMessage(plugin.myLocale().warpsPlayerWarped.replace("[name]", player.getDisplayName()));
        }
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (!(sender instanceof Player) || !label.equalsIgnoreCase("wwarp") || !((Player) sender).hasPermission(Settings.PERMPREFIX + "use") || args.length != 1) {
            return new ArrayList<>();
        }
        String prefix = args[0].toLowerCase();
        return plugin.getWarpSignsListener().listWarps().stream()
                .map(uuid -> plugin.getServer().getOfflinePlayer(uuid).getName())
                .filter(name -> name != null && name.toLowerCase().startsWith(prefix))
                .collect(Collectors.toList());
    }
}