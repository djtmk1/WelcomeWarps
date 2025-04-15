package com.djtmk.wwarps;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeoutException;

public class WarpSigns implements Listener {
	private final WWarps plugin;
	private final long DB_TIMEOUT_MS = 5000;
	private final Map<UUID, Location> warpCache = new HashMap<>();

	public WarpSigns(WWarps plugin) {
		this.plugin = plugin;
		loadWarpCache();
	}

	private void loadWarpCache() {
		try {
			warpCache.putAll(plugin.getDatabase().loadWarps(DB_TIMEOUT_MS));
			if (Settings.debug >= 1) {
				plugin.getLogger().info("Loaded " + warpCache.size() + " warps into cache");
			}
		} catch (SQLException | TimeoutException ex) {
			plugin.getLogger().severe("Error loading warp cache: " + ex.getMessage());
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void onSignBreak(BlockBreakEvent e) {
		Block b = e.getBlock();
		Player player = e.getPlayer();

		if (!Tag.SIGNS.isTagged(b.getType())) {
			return;
		}

		Sign s = (Sign) b.getState();
		String firstLine = s.getSide(Side.FRONT).getLines()[0];

		if (!firstLine.equalsIgnoreCase(ChatColor.GREEN + plugin.myLocale().warpswelcomeLine)) {
			if (Settings.debug >= 2) {
				plugin.getLogger().info("Skipping non-warp sign break by " + player.getName() + " at " + b.getLocation());
			}
			return;
		}

		try {
			UUID owner = plugin.getDatabase().getWarpOwner(b.getLocation(), DB_TIMEOUT_MS);
			if (owner != null) {
				if (owner.equals(player.getUniqueId()) || player.isOp() || player.hasPermission(Settings.PERMPREFIX + "admin")) {
					plugin.getDatabase().removeWarp(b.getLocation(), DB_TIMEOUT_MS);
					warpCache.remove(owner);
					player.sendMessage(ChatColor.GREEN + plugin.myLocale().warpsremoved);
					if (!owner.equals(player.getUniqueId())) {
						Player ownerPlayer = plugin.getServer().getPlayer(owner);
						if (ownerPlayer != null) {
							ownerPlayer.sendMessage(ChatColor.RED + plugin.myLocale().warpssignRemoved);
						} else {
							plugin.getMessages().setMessage(owner, ChatColor.RED + plugin.myLocale().warpssignRemoved);
						}
					}
					if (Settings.debug >= 1) {
						plugin.getLogger().info("Player " + player.getName() + " removed warp for UUID " + owner + " at " + b.getLocation());
					}
				} else {
					player.sendMessage(ChatColor.RED + plugin.myLocale().warpserrorNoRemove);
					e.setCancelled(true);
				}
			}
		} catch (SQLException | TimeoutException ex) {
			player.sendMessage(ChatColor.RED + plugin.myLocale().errorDatabase);
			if (Settings.debug >= 2) {
				plugin.getLogger().severe("Database error on sign break for " + player.getName() + ": " + ex.getMessage());
			}
			e.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void onSignWarpCreate(SignChangeEvent e) {
		String title = e.getLine(0);
		Player player = e.getPlayer();
		if (title.equalsIgnoreCase(plugin.myLocale().warpswelcomeLine)) {
			if (!player.hasPermission(Settings.PERMPREFIX + "add")) {
				player.sendMessage(ChatColor.RED + plugin.myLocale().warpserrorNoPerm);
				e.setCancelled(true);
				return;
			}
			if (Settings.debug >= 1) {
				plugin.getLogger().info("Player " + player.getName() + " attempting to create warp sign at " + e.getBlock().getLocation());
			}
			try {
				Location oldSignLoc = plugin.getDatabase().getWarp(player.getUniqueId(), DB_TIMEOUT_MS);
				if (oldSignLoc != null) {
					Block oldSignBlock = oldSignLoc.getBlock();
					if (Tag.SIGNS.isTagged(oldSignBlock.getType())) {
						Sign oldSign = (Sign) oldSignBlock.getState();
						if (oldSign.getSide(Side.FRONT).getLines()[0].equalsIgnoreCase(ChatColor.GREEN + plugin.myLocale().warpswelcomeLine)) {
							oldSign.getSide(Side.FRONT).setLine(0, ChatColor.RED + plugin.myLocale().warpswelcomeLine);
							oldSign.update();
							player.sendMessage(ChatColor.RED + plugin.myLocale().warpsdeactivate);
							plugin.getDatabase().removeWarp(player.getUniqueId(), DB_TIMEOUT_MS);
							warpCache.remove(player.getUniqueId());
							if (Settings.debug >= 1) {
								plugin.getLogger().info("Deactivated existing warp for " + player.getName() + " at " + oldSignLoc);
							}
						}
					}
				}

				plugin.getDatabase().addWarp(player.getUniqueId(), player.getName(), e.getBlock().getLocation(), DB_TIMEOUT_MS);
				warpCache.put(player.getUniqueId(), e.getBlock().getLocation());
				player.sendMessage(ChatColor.GREEN + plugin.myLocale().warpssuccess);
				e.setLine(0, ChatColor.GREEN + plugin.myLocale().warpswelcomeLine);
				for (int i = 1; i < 4; i++) {
					e.setLine(i, ChatColor.translateAlternateColorCodes('&', e.getLine(i)));
				}
				if (Settings.debug >= 1) {
					plugin.getLogger().info("Warp created successfully for " + player.getName() + " at " + e.getBlock().getLocation());
				}
			} catch (SQLException ex) {
				player.sendMessage(ChatColor.RED + plugin.myLocale().errorDatabase);
				if (Settings.debug >= 2) {
					plugin.getLogger().severe("SQL error on sign create for " + player.getName() + ": " + ex.getMessage());
				}
				e.setCancelled(true);
			} catch (TimeoutException ex) {
				player.sendMessage(ChatColor.RED + plugin.myLocale().errorTimeout);
				if (Settings.debug >= 2) {
					plugin.getLogger().severe("Timeout on sign create for " + player.getName() + ": " + ex.getMessage());
				}
				e.setCancelled(true);
			}
		}
	}

	public void removeWarp(UUID uuid) {
		try {
			plugin.getDatabase().removeWarp(uuid, DB_TIMEOUT_MS);
			warpCache.remove(uuid);
			Player p = plugin.getServer().getPlayer(uuid);
			if (p != null) {
				p.sendMessage(ChatColor.RED + plugin.myLocale().warpssignRemoved);
			} else {
				plugin.getMessages().setMessage(uuid, ChatColor.RED + plugin.myLocale().warpssignRemoved);
			}
			if (Settings.debug >= 1) {
				plugin.getLogger().info("Removed warp for UUID " + uuid + " via command");
			}
		} catch (SQLException | TimeoutException ex) {
			if (Settings.debug >= 2) {
				plugin.getLogger().severe("Error removing warp for UUID " + uuid + ": " + ex.getMessage());
			}
		}
	}

	public void removeWarp(Location loc) {
		try {
			UUID owner = plugin.getDatabase().getWarpOwner(loc, DB_TIMEOUT_MS);
			if (owner != null) {
				plugin.getDatabase().removeWarp(loc, DB_TIMEOUT_MS);
				warpCache.remove(owner);
				Player p = plugin.getServer().getPlayer(owner);
				if (p != null) {
					p.sendMessage(ChatColor.RED + plugin.myLocale().warpssignRemoved);
				} else {
					plugin.getMessages().setMessage(owner, ChatColor.RED + plugin.myLocale().warpssignRemoved);
				}
				if (Settings.debug >= 1) {
					plugin.getLogger().info("Removed warp at " + loc + " for UUID " + owner);
				}
			}
		} catch (SQLException | TimeoutException ex) {
			if (Settings.debug >= 2) {
				plugin.getLogger().severe("Error removing warp at location " + loc + ": " + ex.getMessage());
			}
		}
	}

	public Set<UUID> listWarps() {
		return new HashSet<>(warpCache.keySet());
	}

	public Collection<UUID> listSortedWarps() {
		try {
			return plugin.getDatabase().listSortedWarps(DB_TIMEOUT_MS);
		} catch (SQLException | TimeoutException ex) {
			if (Settings.debug >= 2) {
				plugin.getLogger().severe("Error listing sorted warps: " + ex.getMessage());
			}
			return Collections.emptyList();
		}
	}

	public Location getWarp(UUID player) {
		return warpCache.get(player);
	}

	public String getWarpOwner(Location location) {
		try {
			UUID owner = plugin.getDatabase().getWarpOwner(location, DB_TIMEOUT_MS);
			return owner != null ? plugin.getServer().getOfflinePlayer(owner).getName() : "";
		} catch (SQLException | TimeoutException ex) {
			if (Settings.debug >= 2) {
				plugin.getLogger().severe("Error getting warp owner at " + location + ": " + ex.getMessage());
			}
			return "";
		}
	}
}