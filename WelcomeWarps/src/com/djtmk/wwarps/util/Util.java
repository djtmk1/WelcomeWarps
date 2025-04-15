package com.djtmk.wwarps.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;

import com.djtmk.wwarps.WWarps;

import java.util.ArrayList;
import java.util.List;

public class Util {
	private static final WWarps plugin = WWarps.getPlugin();

	public static float blockFaceToFloat(BlockFace face) {
		switch (face) {
			case EAST: return 90F;
			case EAST_NORTH_EAST: return 67.5F;
			case EAST_SOUTH_EAST: return 0F;
			case NORTH: return 0F;
			case NORTH_EAST: return 45F;
			case NORTH_NORTH_EAST: return 22.5F;
			case NORTH_NORTH_WEST: return 337.5F;
			case NORTH_WEST: return 315F;
			case SOUTH: return 180F;
			case SOUTH_EAST: return 135F;
			case SOUTH_SOUTH_EAST: return 157.5F;
			case SOUTH_SOUTH_WEST: return 202.5F;
			case SOUTH_WEST: return 225F;
			case WEST: return 270F;
			case WEST_NORTH_WEST: return 292.5F;
			case WEST_SOUTH_WEST: return 247.5F;
			default: return 0F;
		}
	}

	public static String prettifyText(String ugly) {
		if (!ugly.contains("_") && !ugly.equals(ugly.toUpperCase())) {
			return ugly;
		}
		StringBuilder fin = new StringBuilder();
		ugly = ugly.toLowerCase();
		if (ugly.contains("_")) {
			String[] splt = ugly.split("_");
			int i = 0;
			for (String s : splt) {
				i++;
				fin.append(Character.toUpperCase(s.charAt(0))).append(s.substring(1));
				if (i < splt.length) fin.append(" ");
			}
		} else {
			fin.append(Character.toUpperCase(ugly.charAt(0))).append(ugly.substring(1));
		}
		return fin.toString();
	}

	public static Location getLocationString(final String s) {
		if (s == null || s.trim().isEmpty()) {
			return null;
		}
		final String[] parts = s.split(":");
		if (parts.length == 4) {
			final World w = Bukkit.getServer().getWorld(parts[0]);
			if (w == null) return null;
			final int x = Integer.parseInt(parts[1]);
			final int y = Integer.parseInt(parts[2]);
			final int z = Integer.parseInt(parts[3]);
			return new Location(w, x, y, z);
		} else if (parts.length == 6) {
			final World w = Bukkit.getServer().getWorld(parts[0]);
			if (w == null) return null;
			final int x = Integer.parseInt(parts[1]);
			final int y = Integer.parseInt(parts[2]);
			final int z = Integer.parseInt(parts[3]);
			final float yaw = Float.intBitsToFloat(Integer.parseInt(parts[4]));
			final float pitch = Float.intBitsToFloat(Integer.parseInt(parts[5]));
			return new Location(w, x, y, z, yaw, pitch);
		}
		return null;
	}

	public static String getStringLocation(final Location l) {
		if (l == null || l.getWorld() == null) {
			return "";
		}
		return l.getWorld().getName() + ":" + l.getBlockX() + ":" + l.getBlockY() + ":" + l.getBlockZ() + ":" +
				Float.floatToIntBits(l.getYaw()) + ":" + Float.floatToIntBits(l.getPitch());
	}

	public static List<String> tabLimit(final List<String> list, final String start) {
		final List<String> returned = new ArrayList<>();
		for (String s : list) {
			if (s.toLowerCase().startsWith(start.toLowerCase())) {
				returned.add(s);
			}
		}
		return returned;
	}

	public static List<String> chop(ChatColor color, String longLine, int length) {
		List<String> result = new ArrayList<>();
		for (int i = 0; i < longLine.length(); i += length) {
			int endIndex = Math.min(i + length, longLine.length());
			String line = longLine.substring(i, endIndex);
			if (endIndex < longLine.length()) {
				if (!line.endsWith(" ") && !longLine.substring(endIndex, endIndex + 1).equals(" ")) {
					int lastSpace = line.lastIndexOf(" ");
					if (lastSpace != -1 && lastSpace < line.length()) {
						line = line.substring(0, lastSpace);
						i -= (length - lastSpace - 1);
					}
				}
			}
			result.add(color + line);
		}
		return result;
	}
}