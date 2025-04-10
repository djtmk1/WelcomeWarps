package com.wasteofplastic.wwarps;

import java.io.File;
import java.io.InputStream;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class Locale {
	private FileConfiguration locale = null;
	private File localeFile = null;
	private final WWarps plugin;

	public String errorUnknownPlayer;
	public String errorNoPermission;
	public String errorCommandNotReady;
	public String errorOfflinePlayer;
	public String errorUnknownCommand;
	public String warpswelcomeLine;
	public String warpswarpTip;
	public String warpssuccess;
	public String warpsremoved;
	public String warpssignRemoved;
	public String warpsdeactivate;
	public String warpserrorNoRemove;
	public String warpserrorNoPerm;
	public String warpserrorNoPlace;
	public String warpserrorDuplicate;
	public String warpserrorDoesNotExist;
	public String warpserrorNotReadyYet;
	public String warpserrorNotSafe;
	public String warpswarpToPlayersSign;
	public String warpserrorNoWarpsYet;
	public String warpswarpsAvailable;
	public String warpsPlayerWarped;
	public String errorerrorYouDoNotHavePermission;
	public String warphelpWarps;
	public String warphelpWarp;
	public String errorerrorInvalidPlayer;
	public String reloadconfigReloaded;
	public String errorWrongWorld;
	public String helpColor;
	public String warpsPrevious;
	public String warpsNext;
	public String warpsTitle;
	public String errorUseInGame;
	public String newsHeadline;
	public String adminHelpreload;
	public String adminHelpHelp;

	public Locale(WWarps plugin, String localeName) {
		this.plugin = plugin;
		getLocale(localeName);
		loadLocale();
	}

	public FileConfiguration getLocale(String localeName) {
		if (this.locale == null) {
			reloadLocale(localeName);
		}
		return locale;
	}

	public void reloadLocale(String localeName) {
		File localeDir = new File(plugin.getDataFolder() + File.separator + "locale");
		if (!localeDir.exists()) {
			localeDir.mkdir();
		}
		if (localeFile == null) {
			localeFile = new File(localeDir.getPath(), localeName + ".yml");
		}
		if (localeFile.exists()) {
			locale = YamlConfiguration.loadConfiguration(localeFile);
		} else {
			InputStream defLocaleStream = plugin.getResource("locale/" + localeName + ".yml");
			if (defLocaleStream != null) {
				plugin.saveResource("locale/" + localeName + ".yml", true);
				localeFile = new File(plugin.getDataFolder() + File.separator + "locale", localeName + ".yml");
				locale = YamlConfiguration.loadConfiguration(localeFile);
			} else {
				localeFile = new File(plugin.getDataFolder() + File.separator + "locale", "locale.yml");
				if (localeFile.exists()) {
					locale = YamlConfiguration.loadConfiguration(localeFile);
				} else {
					defLocaleStream = plugin.getResource("locale/locale.yml");
					if (defLocaleStream != null) {
						plugin.saveResource("locale/locale.yml", true);
						localeFile = new File(plugin.getDataFolder() + File.separator + "locale", "locale.yml");
						locale = YamlConfiguration.loadConfiguration(localeFile);
					} else {
						plugin.getLogger().severe("Could not find any locale file!");
					}
				}
			}
		}
	}

	public void loadLocale() {
		errorUnknownPlayer = ChatColor.translateAlternateColorCodes('&', locale.getString("error.unknownPlayer", "That player is unknown."));
		errorNoPermission = ChatColor.translateAlternateColorCodes('&', locale.getString("error.noPermission", "You don't have permission to use that command!"));
		errorCommandNotReady = ChatColor.translateAlternateColorCodes('&', locale.getString("error.commandNotReady", "You can't use that command right now."));
		errorOfflinePlayer = ChatColor.translateAlternateColorCodes('&', locale.getString("error.offlinePlayer", "That player is offline or doesn't exist."));
		errorUnknownCommand = ChatColor.translateAlternateColorCodes('&', locale.getString("error.unknownCommand", "Unknown command."));
		errorWrongWorld = ChatColor.translateAlternateColorCodes('&', locale.getString("error.wrongWorld", "You cannot do that in this world."));
		newsHeadline = ChatColor.translateAlternateColorCodes('&', locale.getString("newsheadline", "Welcome Warp News"));
		warpswelcomeLine = ChatColor.translateAlternateColorCodes('&', locale.getString("warps.welcomeLine", "[WELCOME]"));
		warpswarpTip = ChatColor.translateAlternateColorCodes('&', locale.getString("warps.warpTip", "Create a warp by placing a sign with [WELCOME] at the top."));
		warpssuccess = ChatColor.translateAlternateColorCodes('&', locale.getString("warps.success", "Welcome sign placed successfully!"));
		warpsremoved = ChatColor.translateAlternateColorCodes('&', locale.getString("warps.removed", "Welcome sign removed!"));
		warpssignRemoved = ChatColor.translateAlternateColorCodes('&', locale.getString("warps.signRemoved", "Your welcome sign was removed!"));
		warpsdeactivate = ChatColor.translateAlternateColorCodes('&', locale.getString("warps.deactivate", "Deactivating old sign!"));
		warpserrorNoRemove = ChatColor.translateAlternateColorCodes('&', locale.getString("warps.errorNoRemove", "You can only remove your own Welcome Sign!"));
		warpserrorNoPerm = ChatColor.translateAlternateColorCodes('&', locale.getString("warps.errorNoPerm", "You do not have permission to place Welcome Signs yet!"));
		warpserrorNoPlace = ChatColor.translateAlternateColorCodes('&', locale.getString("warps.errorNoPlace", "You must be on your island to place a Welcome Sign!"));
		warpserrorDuplicate = ChatColor.translateAlternateColorCodes('&', locale.getString("warps.errorDuplicate", "Sorry! There is a sign already in that location!"));
		warpserrorDoesNotExist = ChatColor.translateAlternateColorCodes('&', locale.getString("warps.errorDoesNotExist", "That warp doesn't exist!"));
		warpserrorNotReadyYet = ChatColor.translateAlternateColorCodes('&', locale.getString("warps.errorNotReadyYet", "That warp is not ready yet. Try again later."));
		warpserrorNotSafe = ChatColor.translateAlternateColorCodes('&', locale.getString("warps.errorNotSafe", "That warp is not safe right now. Try again later."));
		warpswarpToPlayersSign = ChatColor.translateAlternateColorCodes('&', locale.getString("warps.warpToPlayersSign", "Warping to <player>'s welcome sign."));
		warpserrorNoWarpsYet = ChatColor.translateAlternateColorCodes('&', locale.getString("warps.errorNoWarpsYet", "There are no warps available yet!"));
		warpswarpsAvailable = ChatColor.translateAlternateColorCodes('&', locale.getString("warps.warpsAvailable", "The following warps are available"));
		warpsPlayerWarped = ChatColor.translateAlternateColorCodes('&', locale.getString("warps.playerWarped", "[name] &2warped to your island!"));
		warpsPrevious = ChatColor.translateAlternateColorCodes('&', locale.getString("warps.previous", "Previous"));
		warpsNext = ChatColor.translateAlternateColorCodes('&', locale.getString("warps.next", "Next"));
		warpsTitle = ChatColor.translateAlternateColorCodes('&', locale.getString("warps.title", "Island warps"));
		errorerrorYouDoNotHavePermission = ChatColor.translateAlternateColorCodes('&', locale.getString("error.errorYouDoNotHavePermission", "You do not have permission to use that command!"));
		warphelpWarps = ChatColor.translateAlternateColorCodes('&', locale.getString("warp.helpWarps", "Lists all available welcome-sign warps."));
		warphelpWarp = ChatColor.translateAlternateColorCodes('&', locale.getString("warp.helpWarp", "Warp to <player>'s welcome sign."));
		errorerrorInvalidPlayer = ChatColor.translateAlternateColorCodes('&', locale.getString("island.errorInvalidPlayer", "That player is invalid or does not have an island!"));
		reloadconfigReloaded = ChatColor.translateAlternateColorCodes('&', locale.getString("reload.configReloaded", "Configuration reloaded from file."));
		helpColor = ChatColor.translateAlternateColorCodes('&', locale.getString("warp.helpColor", "&e"));
		errorUseInGame = ChatColor.translateAlternateColorCodes('&', locale.getString("error.useInGame", "This command must be used in-game."));
		adminHelpHelp = ChatColor.translateAlternateColorCodes('&', locale.getString("adminHelp.help", "Warp Sign Admin"));
		adminHelpreload = ChatColor.translateAlternateColorCodes('&', locale.getString("adminHelp.reload", "reload plugin settings"));
	}
}