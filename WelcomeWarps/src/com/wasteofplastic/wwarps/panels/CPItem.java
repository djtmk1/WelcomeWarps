package com.wasteofplastic.wwarps.panels;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

public class CPItem {
	private final ItemStack item;
	private final String action;
	private final UUID warp;

	public CPItem(ItemStack item, String name, String action) {
		this(item, name, action, null);
	}

	public CPItem(ItemStack item, String name, String action, UUID warp) {
		this.item = item.clone();
		this.action = action;
		this.warp = warp;
		if (name != null) {
			ItemMeta meta = item.getItemMeta();
			if (meta != null) {
				meta.setDisplayName(name);
				item.setItemMeta(meta);
			}
		}
	}

	public ItemStack getItem() {
		return item.clone();
	}

	public String getAction() {
		return action;
	}

	public UUID getWarp() {
		return warp;
	}
}