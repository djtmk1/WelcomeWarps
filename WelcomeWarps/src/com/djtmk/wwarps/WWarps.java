package com.djtmk.wwarps;

import com.djtmk.wwarps.commands.AdminCmd;
import com.djtmk.wwarps.database.DataBase;
import com.djtmk.wwarps.listeners.JoinLeaveEvents;
import org.bukkit.plugin.java.JavaPlugin;

public class WWarps extends JavaPlugin {
	private static WWarps instance;
	private DataBase database;
	private WarpSigns warpSigns;
	private Messages messages;
	private Locale locale;

	@Override
	public void onEnable() {
		instance = this;
		saveDefaultConfig();
		Settings.loadConfig();
		if (Settings.useWarpPanel) {
			getLogger().warning("config.yml 'usewarppanel' is ignored; using new GUI with sign icons.");
		}
		getLogger().info("WWarps config loaded.");

		locale = new Locale(this, "en_US");
		messages = new Messages(this);
		getLogger().info("WWarps locale and messages initialized.");

		database = new DataBase(this);
		getLogger().info("WWarps database initialized.");

		warpSigns = new WarpSigns(this);
		getServer().getPluginManager().registerEvents(warpSigns, this);
		getServer().getPluginManager().registerEvents(new JoinLeaveEvents(this), this);
		getLogger().info("WWarps listeners registered.");

		com.djtmk.wwarps.WarpCmd warpCmd = new com.djtmk.wwarps.WarpCmd(this);
		getCommand("wwarps").setExecutor(warpCmd);
		getCommand("wwarps").setTabCompleter(warpCmd);
		getCommand("wwarp").setExecutor(warpCmd);
		getCommand("wwarp").setTabCompleter(warpCmd);
		getCommand("wwadmin").setExecutor(new AdminCmd(this));
		getLogger().info("WWarps commands registered.");
	}

	@Override
	public void onDisable() {
		if (database != null) {
			database.closeConnection();
			getLogger().info("WWarps database connection closed.");
		}
	}

	public static WWarps getPlugin() {
		return instance;
	}

	public DataBase getDatabase() {
		return database;
	}

	public WarpSigns getWarpSigns() {
		return warpSigns;
	}

	public Messages getMessages() {
		return messages;
	}

	public Locale myLocale() {
		return locale;
	}
}