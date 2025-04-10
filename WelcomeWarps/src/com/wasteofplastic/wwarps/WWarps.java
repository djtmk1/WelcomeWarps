package com.wasteofplastic.wwarps;

import com.wasteofplastic.wwarps.commands.AdminCmd;
import com.wasteofplastic.wwarps.commands.WarpCmd;
import com.wasteofplastic.wwarps.listeners.JoinLeaveEvents;
import com.wasteofplastic.wwarps.panels.WarpPanel;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag; // Import for Tag.SIGNS
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

		// Check for portals
		if (space1Type == Material.NETHER_PORTAL || groundType == Material.NETHER_PORTAL || space2Type == Material.NETHER_PORTAL ||
				space1Type == Material.END_PORTAL || groundType == Material.END_PORTAL || space2Type == Material.END_PORTAL) {
			return false;
		}

		// Check if ground is air
		if (groundType == Material.AIR) {
			return false;
		}

		// Check for liquids
		if (groundType == Material.WATER || groundType == Material.LAVA ||
				space1Type == Material.WATER || space1Type == Material.LAVA ||
				space2Type == Material.WATER || space2Type == Material.LAVA) {
			return false;
		}

		// Check for unsafe ground materials, using Tag.SIGNS
		if (groundType == Material.CACTUS ||
				groundType == Material.OAK_BOAT ||
				groundType == Material.OAK_FENCE ||
				Tag.SIGNS.isTagged(groundType)) { // Replaces SIGN and WALL_SIGN
			return false;
		}

		// Check that the space above is not solid, allowing signs
		return (!space1Type.isSolid() || Tag.SIGNS.isTagged(space1Type)) &&
				(!space2Type.isSolid() || Tag.SIGNS.isTagged(space2Type));
	}

	private final HashMap<String, Locale> availableLocales = new HashMap<>();
	private WarpSigns warpSignsListener;
	private WarpPanel warpPanel;
	private Messages messages;
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

	public void loadPluginConfig() {
		try {
			getConfig();
		} catch (final Exception e) {
			e.printStackTrace();
		}
		availableLocales.put("locale", new Locale(this, "locale"));
		Settings.debug = getConfig().getInt("debug", 0);
		Settings.useWarpPanel = getConfig().getBoolean("usewarppanel", true);
		Settings.worldName = getConfig().getStringList("signworlds");
	}

	public Locale myLocale() {
		return availableLocales.get("locale");
	}

	@Override
	public void onDisable() {
		try {
			if (warpSignsListener != null) {
				warpSignsListener.saveWarpList(false);
			}
		} catch (final Exception e) {
			getLogger().severe("Something went wrong saving files!");
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
			getWarpSignsListener().loadWarpList();
			warpPanel = new WarpPanel(this);
			getServer().getPluginManager().registerEvents(warpPanel, this);
		});
	}

	public void registerEvents() {
		final PluginManager manager = getServer().getPluginManager();
		manager.registerEvents(new JoinLeaveEvents(this), this);
		warpSignsListener = new WarpSigns(this);
		manager.registerEvents(warpSignsListener, this);
	}

	public static WWarps getPlugin() {
		return plugin;
	}
}