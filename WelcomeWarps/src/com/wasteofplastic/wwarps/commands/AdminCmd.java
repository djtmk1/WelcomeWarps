package com.wasteofplastic.wwarps.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.wasteofplastic.wwarps.Settings;
import com.wasteofplastic.wwarps.WWarps;

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
			return true;
		}

		if (split.length == 1 && split[0].equalsIgnoreCase("reload")) {
			plugin.reloadConfig();
			plugin.loadPluginConfig();
			sender.sendMessage(ChatColor.YELLOW + plugin.myLocale().reloadconfigReloaded);
			return true;
		}

		sender.sendMessage(ChatColor.RED + plugin.myLocale().errorUnknownCommand);
		return true;
	}
}