package com.wasteofplastic.wwarps;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
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
	private final long DB_TIMEOUT_MS = 5000; // 5 seconds timeout

	public WarpSigns(WWarps plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void onSignBreak(BlockBreakEvent e) {
		Block b = e.getBlock();
		Player player = e.getPlayer();
		try {
			UUID owner = plugin.getDatabase().getWarpOwner(b.getLocation(), DB_TIMEOUT_MS);
			if (Tag.SIGNS.isTagged(b.getType())) {
				Sign s = (Sign) b.getState();
				if (s.getSide(Side.FRONT).getLines()[0].equalsIgnoreCase(ChatColor.GREEN + plugin.myLocale().warpswelcomeLine)) {
					if (owner != null) {
						if (owner.equals(player.getUniqueId()) || player.isOp() || player.hasPermission(Settings.PERMPREFIX + "admin")) {
							plugin.getDatabase().removeWarp(b.getLocation(), DB_TIMEOUT_MS);
							player.sendMessage(ChatColor.GREEN + plugin.myLocale().warpsremoved);
							if (!owner.equals(player.getUniqueId())) {
								Player ownerPlayer = plugin.getServer().getPlayer(owner);
								if (ownerPlayer != null) {
									ownerPlayer.sendMessage(ChatColor.RED + plugin.myLocale().warpssignRemoved);
								} else {
									plugin.getMessages().setMessage(owner, ChatColor.RED + plugin.myLocale().warpssignRemoved);
								}
							}
						} else {
							player.sendMessage(ChatColor.RED + plugin.myLocale().warpserrorNoRemove);
							e.setCancelled(true);
						}
					}
				}
			}
		} catch (SQLException | TimeoutException ex) {
			player.sendMessage(ChatColor.RED + plugin.myLocale().errorDatabase);
			plugin.getLogger().severe("Database error on sign break: " + ex.getMessage());
			e.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void onSignWarpCreate(SignChangeEvent e) {
		String title = e.getLine(0);
		Player player = e.getPlayer();
		if (title.equalsIgnoreCase(plugin.myLocale().warpswelcomeLine)) {
			try {
				if (!player.hasPermission(Settings.PERMPREFIX + "add")) {
					player.sendMessage(ChatColor.RED + plugin.myLocale().warpserrorNoPerm);
					e.setCancelled(true);
					return;
				}
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
						}
					}
				}
				plugin.getDatabase().addWarp(player.getUniqueId(), player.getName(), e.getBlock().getLocation(), DB_TIMEOUT_MS);
				player.sendMessage(ChatColor.GREEN + plugin.myLocale().warpssuccess);
				e.setLine(0, ChatColor.GREEN + plugin.myLocale().warpswelcomeLine);
				for (int i = 1; i < 4; i++) {
					e.setLine(i, ChatColor.translateAlternateColorCodes('&', e.getLine(i)));
				}
			} catch (SQLException ex) {
				player.sendMessage(ChatColor.RED + plugin.myLocale().errorDatabase);
				plugin.getLogger().severe("Database error on sign create: " + ex.getMessage());
				e.setCancelled(true);
			} catch (TimeoutException ex) {
				player.sendMessage(ChatColor.RED + plugin.myLocale().errorTimeout);
				plugin.getLogger().severe("Database timeout on sign create: " + ex.getMessage());
				e.setCancelled(true);
			}
		}
	}

	public void removeWarp(UUID uuid) {
		try {
			plugin.getDatabase().removeWarp(uuid, DB_TIMEOUT_MS);
			Player p = plugin.getServer().getPlayer(uuid);
			if (p != null) {
				p.sendMessage(ChatColor.RED + plugin.myLocale().warpssignRemoved);
			} else {
				plugin.getMessages().setMessage(uuid, ChatColor.RED + plugin.myLocale().warpssignRemoved);
			}
			plugin.getWarpPanel().invalidateCache(uuid);
		} catch (SQLException | TimeoutException ex) {
			plugin.getLogger().severe("Error removing warp for UUID " + uuid + ": " + ex.getMessage());
		}
	}

	public void removeWarp(Location loc) {
		try {
			UUID owner = plugin.getDatabase().getWarpOwner(loc, DB_TIMEOUT_MS);
			if (owner != null) {
				plugin.getDatabase().removeWarp(loc, DB_TIMEOUT_MS);
				Player p = plugin.getServer().getPlayer(owner);
				if (p != null) {
					p.sendMessage(ChatColor.RED + plugin.myLocale().warpssignRemoved);
				} else {
					plugin.getMessages().setMessage(owner, ChatColor.RED + plugin.myLocale().warpssignRemoved);
				}
				plugin.getWarpPanel().invalidateCache(owner);
			}
		} catch (SQLException | TimeoutException ex) {
			plugin.getLogger().severe("Error removing warp at location " + loc + ": " + ex.getMessage());
		}
	}

	public Set<UUID> listWarps() {
		try {
			return plugin.getDatabase().listWarps(DB_TIMEOUT_MS);
		} catch (SQLException | TimeoutException ex) {
			plugin.getLogger().severe("Error listing warps: " + ex.getMessage());
			return Collections.emptySet();
		}
	}

	public Collection<UUID> listSortedWarps() {
		try {
			return plugin.getDatabase().listSortedWarps(DB_TIMEOUT_MS);
		} catch (SQLException | TimeoutException ex) {
			plugin.getLogger().severe("Error listing sorted warps: " + ex.getMessage());
			return Collections.emptyList();
		}
	}

	public Location getWarp(UUID player) {
		try {
			return plugin.getDatabase().getWarp(player, DB_TIMEOUT_MS);
		} catch (SQLException | TimeoutException ex) {
			plugin.getLogger().severe("Error getting warp for UUID " + player + ": " + ex.getMessage());
			return null;
		}
	}

	public String getWarpOwner(Location location) {
		try {
			UUID owner = plugin.getDatabase().getWarpOwner(location, DB_TIMEOUT_MS);
			return owner != null ? plugin.getServer().getOfflinePlayer(owner).getName() : "";
		} catch (SQLException | TimeoutException ex) {
			plugin.getLogger().severe("Error getting warp owner at " + location + ": " + ex.getMessage());
			return "";
		}
	}
}