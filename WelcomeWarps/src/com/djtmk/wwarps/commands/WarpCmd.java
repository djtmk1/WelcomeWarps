package com.djtmk.wwarps;

import com.djtmk.wwarps.gui.GUI;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class WarpCmd implements CommandExecutor, TabCompleter {
    private final WWarps plugin;

    public WarpCmd(WWarps plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + plugin.myLocale().errorNoPermission);
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            if (!player.hasPermission(Settings.PERMPREFIX + "use")) {
                player.sendMessage(ChatColor.RED + plugin.myLocale().errorNoPermission);
                return true;
            }
            GUI gui = new GUI(plugin);
            gui.openGUI(player);
            if (Settings.debug >= 1) {
                plugin.getLogger().info("Player " + player.getName() + " opened warps GUI");
            }
            return true;
        }

        if (!player.hasPermission(Settings.PERMPREFIX + "use")) {
            player.sendMessage(ChatColor.RED + plugin.myLocale().errorNoPermission);
            return true;
        }

        String targetPlayer = args[0];
        if (targetPlayer.equalsIgnoreCase(player.getName())) {
            player.sendMessage(ChatColor.RED + "You cannot warp to yourself.");
            return true;
        }

        try {
            UUID targetUUID = plugin.getDatabase().getUUIDByName(targetPlayer);
            if (targetUUID == null) {
                player.sendMessage(ChatColor.RED + plugin.myLocale().errorNoPlayer);
                return true;
            }
            Location warpLoc = plugin.getDatabase().getWarp(targetUUID, 5000);
            if (warpLoc == null) {
                player.sendMessage(ChatColor.RED + plugin.myLocale().warpserrorDoesNotExist);
                return true;
            }
            player.teleport(warpLoc);
            plugin.getDatabase().updateLastUsed(targetUUID, 5000);
            player.sendMessage(ChatColor.GREEN + "Warped to " + targetPlayer + "'s warp!");
            if (Settings.debug >= 1) {
                plugin.getLogger().info("Player " + player.getName() + " warped to " + targetPlayer);
            }
        } catch (SQLException | TimeoutException e) {
            player.sendMessage(ChatColor.RED + plugin.myLocale().errorDatabase);
            if (Settings.debug >= 2) {
                plugin.getLogger().severe("Error warping " + player.getName() + " to " + targetPlayer + ": " + e.getMessage());
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("list");
            try {
                Collection<UUID> warpUUIDs = plugin.getDatabase().listWarps(5000);
                for (UUID uuid : warpUUIDs) {
                    String playerName = plugin.getServer().getOfflinePlayer(uuid).getName();
                    if (playerName != null) {
                        completions.add(playerName);
                    }
                }
            } catch (SQLException | TimeoutException e) {
                if (Settings.debug >= 2) {
                    plugin.getLogger().severe("Error fetching warps for tab completion: " + e.getMessage());
                }
            }
        }
        String input = args.length > 0 ? args[args.length - 1].toLowerCase() : "";
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(input))
                .collect(Collectors.toList());
    }
}