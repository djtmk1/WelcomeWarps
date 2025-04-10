package com.wasteofplastic.wwarps.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.wasteofplastic.wwarps.Settings;
import com.wasteofplastic.wwarps.WWarps;

import java.util.UUID;

public class AdminCmd implements CommandExecutor {
	private final WWarps plugin;

	public AdminCmd(WWarps plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] split) {
		Player player = null;
		if (sender instanceof Player) {
			player = (Player) sender;
			if (!player.hasPermission(Settings.PERMPREFIX + "admin")) {
				player.sendMessage(ChatColor.RED + plugin.myLocale().errorNoPermission);
				return true;
			}
		}

		if (split.length == 0) {
			sender.sendMessage(plugin.myLocale().adminHelpHelp);
			sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload:" + ChatColor.WHITE + " " + plugin.myLocale().adminHelpreload);
			sender.sendMessage(ChatColor.YELLOW + "/" + label + " list:" + ChatColor.WHITE + " List all warps");
			sender.sendMessage(ChatColor.YELLOW + "/" + label + " remove <player>:" + ChatColor.WHITE + " Remove a player's warp");
			return true;
		}

		switch (split[0].toLowerCase()) {
			case "reload":
				if (player != null && !player.hasPermission(Settings.PERMPREFIX + "admin.reload")) {
					player.sendMessage(ChatColor.RED + plugin.myLocale().errorNoPermission);
					return true;
				}
				plugin.reloadConfig();
				plugin.loadPluginConfig();
				sender.sendMessage(ChatColor.YELLOW + plugin.myLocale().reloadconfigReloaded);
				return true;
			case "list":
				if (player != null && !player.hasPermission(Settings.PERMPREFIX + "admin.list")) {
					player.sendMessage(ChatColor.RED + plugin.myLocale().errorNoPermission);
					return true;
				}
				sender.sendMessage(ChatColor.YELLOW + "Active Warps:");
				plugin.getWarpSignsListener().listWarps().forEach(uuid -> {
					String name = plugin.getServer().getOfflinePlayer(uuid).getName();
					sender.sendMessage(ChatColor.WHITE + "- " + (name != null ? name : uuid.toString()));
				});
				return true;
			case "remove":
				if (player != null && !player.hasPermission(Settings.PERMPREFIX + "admin.remove")) {
					player.sendMessage(ChatColor.RED + plugin.myLocale().errorNoPermission);
					return true;
				}
				if (split.length != 2) {
					sender.sendMessage(ChatColor.RED + "Usage: /" + label + " remove <player>");
					return true;
				}
				String targetName = split[1];
				UUID targetUUID = plugin.getServer().getOfflinePlayer(targetName).getUniqueId();
				if (plugin.getWarpSignsListener().getWarp(targetUUID) != null) {
					plugin.getWarpSignsListener().removeWarp(targetUUID);
					sender.sendMessage(ChatColor.GREEN + "Removed warp for " + targetName);
				} else {
					sender.sendMessage(ChatColor.RED + "No warp found for " + targetName);
				}
				return true;
			default:
				sender.sendMessage(ChatColor.RED + plugin.myLocale().errorUnknownCommand);
				return true;
		}
	}
}