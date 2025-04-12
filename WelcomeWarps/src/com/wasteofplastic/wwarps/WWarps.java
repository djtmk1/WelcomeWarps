package com.wasteofplastic.wwarps;

import com.wasteofplastic.wwarps.commands.AdminCmd;
import com.wasteofplastic.wwarps.commands.WarpCmd;
import com.wasteofplastic.wwarps.database.DataBase;
import com.wasteofplastic.wwarps.listeners.JoinLeaveEvents;
import com.wasteofplastic.wwarps.panels.WarpPanel;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public class WWarps extends JavaPlugin {
	public static boolean isSafeLocation(final Location l) {
		if (l == null) {
			return false;
		}
		final Block ground = l.getBlock().getRelative(BlockFace.DOWN);
		final Block space1 = l.getBlock();
		final Block space2 = l.getBlock().getRelative(BlockFace.UP);

		Material groundType = ground.getType();
		Material space1Type = space1.getType();
		Material space2Type = space2.getType();

		if (space1Type == Material.NETHER_PORTAL || groundType == Material.NETHER_PORTAL || space2Type == Material.NETHER_PORTAL ||
				space1Type == Material.END_PORTAL || groundType == Material.END_PORTAL || space2Type == Material.END_PORTAL) {
			return false;
		}

		if (groundType == Material.AIR) {
			return false;
		}

		if (groundType == Material.WATER || groundType == Material.LAVA ||
				space1Type == Material.WATER || space1Type == Material.LAVA ||
				space2Type == Material.WATER || space2Type == Material.LAVA) {
			return false;
		}

		if (groundType == Material.CACTUS ||
				groundType == Material.OAK_BOAT ||
				groundType == Material.OAK_FENCE ||
				Tag.SIGNS.isTagged(groundType)) {
			return false;
		}

		return (!space1Type.isSolid() || Tag.SIGNS.isTagged(space1Type)) &&
				(!space2Type.isSolid() || Tag.SIGNS.isTagged(space2Type));
	}

	private final HashMap<String, Locale> availableLocales = new HashMap<>();
	private WarpSigns warpSignsListener;
	private WarpPanel warpPanel;
	private Messages messages;
	private DataBase database;
	private static WWarps plugin;

	public Messages getMessages() {
		if (messages == null) {
			messages = new Messages(this);
		}
		return messages;
	}

	public WarpPanel getWarpPanel() {
		if (warpPanel == null) {
			warpPanel = new WarpPanel(this);
			getServer().getPluginManager().registerEvents(warpPanel, this);
		}
		return warpPanel;
	}

	public WarpSigns getWarpSignsListener() {
		return warpSignsListener;
	}

	public DataBase getDatabase() {
		if (database == null) {
			database = new DataBase(this);
		}
		return database;
	}

	public Locale myLocale() {
		return availableLocales.getOrDefault("locale", new Locale(this, "locale"));
	}

	public void loadPluginConfig() {
		try {
			getConfig();
		} catch (final Exception e) {
			e.printStackTrace();
		}
		availableLocales.put("locale", new Locale(this, "locale"));
		Settings.debug = getConfig().getInt("debug", 0);
		Settings.useWarpPanel = getConfig().getBoolean("usewarppanel", true);
	}

	@Override
	public void onDisable() {
		try {
			if (database != null) {
				database.closeConnection();
			}
		} catch (final Exception e) {
			getLogger().severe("Something went wrong closing database!");
			e.printStackTrace();
		}
	}

	@Override
	public void onEnable() {
		plugin = this;
		saveDefaultConfig();
		loadPluginConfig();
		WarpCmd warpCmd = new WarpCmd(this);
		AdminCmd adminCmd = new AdminCmd(this);
		getCommand("wwarp").setExecutor(warpCmd);
		getCommand("wwarp").setTabCompleter(warpCmd);
		getCommand("wwadmin").setExecutor(adminCmd);
		registerEvents();
		getServer().getScheduler().runTask(this, () -> {
			warpSignsListener = new WarpSigns(this);
			getServer().getPluginManager().registerEvents(warpSignsListener, this);
			warpPanel = new WarpPanel(this);
			getServer().getPluginManager().registerEvents(warpPanel, this);
		});
	}

	public void registerEvents() {
		final PluginManager manager = getServer().getPluginManager();
		manager.registerEvents(new JoinLeaveEvents(this), this);
	}

	public static WWarps getPlugin() {
		return plugin;
	}
}