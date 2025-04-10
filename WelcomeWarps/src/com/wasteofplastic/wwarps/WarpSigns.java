package com.wasteofplastic.wwarps;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import com.wasteofplastic.wwarps.util.Util;

public class WarpSigns implements Listener {
	private final WWarps plugin;
	private final HashMap<UUID, Location> warpList;
	private final HashMap<Location, UUID> locationToOwner; // New reverse mapping
	private YamlConfiguration welcomeWarps;

	public WarpSigns(WWarps plugin) {
		this.plugin = plugin;
		this.warpList = new HashMap<>();
		this.locationToOwner = new HashMap<>();
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void onSignBreak(BlockBreakEvent e) {
		Block b = e.getBlock();
		Player player = e.getPlayer();
		if (Settings.worldName.isEmpty() || Settings.worldName.contains(b.getWorld().getName())) {
			if (Tag.SIGNS.isTagged(b.getType())) {
				Sign s = (Sign) b.getState();
				if (s.getLines()[0].equalsIgnoreCase(ChatColor.GREEN + plugin.myLocale().warpswelcomeLine)) {
					if (warpList.containsValue(s.getLocation())) {
						if (warpList.containsKey(player.getUniqueId()) && warpList.get(player.getUniqueId()).equals(s.getLocation())) {
							removeWarp(s.getLocation());
						} else if (player.isOp() || player.hasPermission(Settings.PERMPREFIX + "admin")) {
							player.sendMessage(ChatColor.GREEN + plugin.myLocale().warpsremoved);
							removeWarp(s.getLocation());
						} else {
							player.sendMessage(ChatColor.RED + plugin.myLocale().warpserrorNoRemove);
							e.setCancelled(true);
						}
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void onSignWarpCreate(SignChangeEvent e) {
		String title = e.getLine(0);
		Player player = e.getPlayer();
		if (title.equalsIgnoreCase(plugin.myLocale().warpswelcomeLine)) {
			if (!Settings.worldName.isEmpty() && !Settings.worldName.contains(player.getWorld().getName())) {
				player.sendMessage(ChatColor.RED + plugin.myLocale().errorWrongWorld);
				return;
			}
			if (!player.hasPermission(Settings.PERMPREFIX + "add")) {
				player.sendMessage(ChatColor.RED + plugin.myLocale().warpserrorNoPerm);
				return;
			}
			final Location oldSignLoc = getWarp(player.getUniqueId());
			if (oldSignLoc == null) {
				if (addWarp(player.getUniqueId(), e.getBlock().getLocation())) {
					player.sendMessage(ChatColor.GREEN + plugin.myLocale().warpssuccess);
					e.setLine(0, ChatColor.GREEN + plugin.myLocale().warpswelcomeLine);
					for (int i = 1; i < 4; i++) {
						e.setLine(i, ChatColor.translateAlternateColorCodes('&', e.getLine(i)));
					}
				} else {
					player.sendMessage(ChatColor.RED + plugin.myLocale().warpserrorDuplicate);
					e.setLine(0, ChatColor.RED + plugin.myLocale().warpswelcomeLine);
					for (int i = 1; i < 4; i++) {
						e.setLine(i, ChatColor.translateAlternateColorCodes('&', e.getLine(i)));
					}
				}
			} else {
				Block oldSignBlock = oldSignLoc.getBlock();
				if (Tag.SIGNS.isTagged(oldSignBlock.getType())) {
					Sign oldSign = (Sign) oldSignBlock.getState();
					if (oldSign.getLines()[0].equalsIgnoreCase(ChatColor.GREEN + plugin.myLocale().warpswelcomeLine)) {
						oldSign.setLine(0, ChatColor.RED + plugin.myLocale().warpswelcomeLine);
						oldSign.update();
						player.sendMessage(ChatColor.RED + plugin.myLocale().warpsdeactivate);
						removeWarp(player.getUniqueId());
					}
				}
				if (addWarp(player.getUniqueId(), e.getBlock().getLocation())) {
					player.sendMessage(ChatColor.GREEN + plugin.myLocale().warpssuccess);
					e.setLine(0, ChatColor.GREEN + plugin.myLocale().warpswelcomeLine);
				} else {
					player.sendMessage(ChatColor.RED + plugin.myLocale().warpserrorDuplicate);
					e.setLine(0, ChatColor.RED + plugin.myLocale().warpswelcomeLine);
				}
			}
		}
	}

	public void saveWarpList(boolean reloadPanel) {
		if (warpList == null || welcomeWarps == null) return;
		final HashMap<String, Object> warps = new HashMap<>();
		for (UUID p : warpList.keySet()) {
			warps.put(p.toString(), Util.getStringLocation(warpList.get(p)));
		}
		welcomeWarps.set("warps", warps);
		Util.saveYamlFile(welcomeWarps, "warps.yml");
		if (reloadPanel && plugin.getWarpPanel() != null) {
			plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getWarpPanel().updatePanel());
		}
	}

	public void loadWarpList() {
		plugin.getLogger().info("Loading warps...");
		welcomeWarps = Util.loadYamlFile("warps.yml");
		if (welcomeWarps.getConfigurationSection("warps") == null) {
			welcomeWarps.createSection("warps");
		}
		HashMap<String, Object> temp = (HashMap<String, Object>) welcomeWarps.getConfigurationSection("warps").getValues(true);
		for (String s : temp.keySet()) {
			try {
				UUID playerUUID = UUID.fromString(s);
				Location l = Util.getLocationString((String) temp.get(s));
				Block b = l.getBlock();
				if (Tag.SIGNS.isTagged(b.getType())) {
					warpList.put(playerUUID, l);
					locationToOwner.put(l, playerUUID); // Populate reverse map
				} else {
					plugin.getLogger().warning("Warp at location " + temp.get(s) + " has no sign - removing.");
				}
			} catch (Exception e) {
				plugin.getLogger().severe("Problem loading warp at location " + temp.get(s) + " - removing.");
				e.printStackTrace();
			}
		}
	}

	public boolean addWarp(UUID player, Location loc) {
		if (warpList.containsValue(loc)) return false;
		Location oldLoc = warpList.remove(player);
		if (oldLoc != null) locationToOwner.remove(oldLoc); // Clean up old location
		warpList.put(player, loc);
		locationToOwner.put(loc, player); // Update reverse map
		saveWarpList(true);
		return true;
	}

	public void removeWarp(UUID uuid) {
		Location loc = warpList.get(uuid);
		if (loc != null) {
			popSign(loc);
			warpList.remove(uuid);
			locationToOwner.remove(loc); // Sync reverse map
			saveWarpList(true);
		}
	}

	private void popSign(Location loc) {
		Block b = loc.getBlock();
		if (Tag.SIGNS.isTagged(b.getType())) {
			Sign s = (Sign) b.getState();
			if (s.getLines()[0].equalsIgnoreCase(ChatColor.GREEN + plugin.myLocale().warpswelcomeLine)) {
				s.setLine(0, ChatColor.RED + plugin.myLocale().warpswelcomeLine);
				s.update();
			}
		}
	}

	public void removeWarp(Location loc) {
		UUID owner = locationToOwner.remove(loc); // O(1) lookup
		if (owner != null) {
			popSign(loc);
			warpList.remove(owner);
			Player p = plugin.getServer().getPlayer(owner);
			if (p != null) {
				p.sendMessage(ChatColor.RED + plugin.myLocale().warpssignRemoved);
			} else {
				plugin.getMessages().setMessage(owner, ChatColor.RED + plugin.myLocale().warpssignRemoved);
			}
			saveWarpList(true);
		}
	}

	public Set<UUID> listWarps() {
		return warpList.keySet();
	}

	public Collection<UUID> listSortedWarps() {
		TreeMap<Long, UUID> map = new TreeMap<>();
		for (UUID uuid : warpList.keySet()) {
			map.put(plugin.getServer().getOfflinePlayer(uuid).getLastPlayed(), uuid);
		}
		return map.descendingMap().values();
	}

	public Location getWarp(UUID player) {
		return warpList.getOrDefault(player, null);
	}

	public String getWarpOwner(Location location) {
		UUID owner = locationToOwner.get(location);
		return owner != null ? plugin.getServer().getOfflinePlayer(owner).getName() : "";
	}
}