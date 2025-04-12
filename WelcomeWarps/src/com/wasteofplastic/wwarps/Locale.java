package com.wasteofplastic.wwarps;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class Locale {
	private final WWarps plugin;
	public String warpswelcomeLine;
	public String warpssuccess;
	public String warpsremoved;
	public String warpssignRemoved;
	public String warpswarpTip;
	public String warpserrorNoPerm;
	public String warpserrorNoRemove;
	public String warpserrorDuplicate;
	public String warpserrorNoWarpsYet;
	public String warpswarpsAvailable;
	public String warpserrorDoesNotExist;
	public String warpserrorNotReadyYet;
	public String warpserrorNotSafe;
	public String warpsdeactivate;
	public String warpsPlayerWarped;
	public String warpsTitle;
	public String warpsPlayer;
	public String warpsWorld;
	public String warpsX;
	public String warpsY;
	public String warpsZ;
	public String warpsNext;
	public String warpsPrevious;
	public String errorNoPermission;
	public String errorUnknownCommand;
	public String errorNoPlayer;
	public String errorDatabase;
	public String errorTimeout;
	public String adminHelpHelp;
	public String adminHelpreload;
	public String reloadconfigReloaded;

	public Locale(WWarps plugin, String localeName) {
		this.plugin = plugin;
		File localeFile = new File(plugin.getDataFolder(), "locale/" + localeName + ".yml");
		YamlConfiguration config = YamlConfiguration.loadConfiguration(localeFile);
		warpswelcomeLine = config.getString("warps.welcomeLine", "[WELCOME]");
		warpssuccess = config.getString("warps.success", "Welcome sign placed successfully!");
		warpsremoved = config.getString("warps.removed", "Warp removed!");
		warpssignRemoved = config.getString("warps.signRemoved", "Your welcome sign was removed!");
		warpswarpTip = config.getString("warps.warpTip", "Create a welcome sign with [WELCOME] on the first line!");
		warpserrorNoPerm = config.getString("warps.errorNoPerm", "You do not have permission to place Welcome Signs yet!");
		warpserrorNoRemove = config.getString("warps.errorNoRemove", "You cannot remove this sign!");
		warpserrorDuplicate = config.getString("warps.errorDuplicate", "A warp already exists at this location!");
		warpserrorNoWarpsYet = config.getString("warps.errorNoWarpsYet", "There are no warps available yet!");
		warpswarpsAvailable = config.getString("warps.warpsAvailable", "Warps available");
		warpserrorDoesNotExist = config.getString("warps.errorDoesNotExist", "That warp does not exist!");
		warpserrorNotReadyYet = config.getString("warps.errorNotReadyYet", "That warp is not ready yet!");
		warpserrorNotSafe = config.getString("warps.errorNotSafe", "That warp is not safe!");
		warpsdeactivate = config.getString("warps.deactivate", "Previous warp sign deactivated!");
		warpsPlayerWarped = config.getString("warps.playerWarped", "[name] warped to your sign!");
		warpsTitle = config.getString("warps.title", "Warp Panel");
		warpsPlayer = config.getString("warps.player", "Player");
		warpsWorld = config.getString("warps.world", "World");
		warpsX = config.getString("warps.x", "X");
		warpsY = config.getString("warps.y", "Y");
		warpsZ = config.getString("warps.z", "Z");
		warpsNext = config.getString("warps.next", "Next Page");
		warpsPrevious = config.getString("warps.previous", "Previous Page");
		errorNoPermission = config.getString("error.noPermission", "You do not have permission!");
		errorUnknownCommand = config.getString("error.unknownCommand", "Unknown command!");
		errorNoPlayer = config.getString("error.noPlayer", "That player does not exist!");
		errorDatabase = config.getString("error.database", "Error accessing database!");
		errorTimeout = config.getString("error.timeout", "Database operation timed out!");
		adminHelpHelp = config.getString("admin.help.help", "Admin commands for Welcome Warp Signs");
		adminHelpreload = config.getString("admin.help.reload", "Reload the configuration");
		reloadconfigReloaded = config.getString("reload.configReloaded", "Configuration reloaded!");
	}
}