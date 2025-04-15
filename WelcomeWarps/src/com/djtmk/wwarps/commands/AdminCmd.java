package com.djtmk.wwarps.commands;

import com.djtmk.wwarps.Settings;
import com.djtmk.wwarps.WWarps;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class AdminCmd implements CommandExecutor {
	private final WWarps plugin;
	private final long DB_TIMEOUT_MS = 5000;

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

		try {
			switch (split[0].toLowerCase()) {
				case "reload":
					if (player != null && !player.hasPermission(Settings.PERMPREFIX + "admin.reload")) {
						player.sendMessage(ChatColor.RED + plugin.myLocale().errorNoPermission);
						return true;
					}
					plugin.reloadConfig();
					Settings.loadConfig();
					plugin.getLogger().info("Configuration reloaded by " + sender.getName());
					sender.sendMessage(ChatColor.YELLOW + plugin.myLocale().reloadconfigReloaded);
					return true;
				case "list":
					if (player != null && !player.hasPermission(Settings.PERMPREFIX + "admin.list")) {
						player.sendMessage(ChatColor.RED + plugin.myLocale().errorNoPermission);
						return true;
					}
					sender.sendMessage(ChatColor.YELLOW + "Active Warps:");
					plugin.getWarpSigns().listWarps().forEach(uuid -> {
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
					UUID targetUUID = plugin.getDatabase().getUUIDByName(targetName);
					if (targetUUID != null && plugin.getWarpSigns().getWarp(targetUUID) != null) {
						plugin.getWarpSigns().removeWarp(targetUUID);
						sender.sendMessage(ChatColor.GREEN + "Removed warp for " + targetName);
					} else {
						sender.sendMessage(ChatColor.RED + plugin.myLocale().errorNoPlayer);
					}
					return true;
				default:
					sender.sendMessage(ChatColor.RED + plugin.myLocale().errorUnknownCommand);
					return true;
			}
		} catch (Exception ex) {
			sender.sendMessage(ChatColor.RED + "Error processing command!");
			plugin.getLogger().severe("Error in admin command: " + ex.getMessage());
			return true;
		}
	}
}