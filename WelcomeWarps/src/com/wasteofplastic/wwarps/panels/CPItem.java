package com.wasteofplastic.wwarps.panels;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CPItem {
	private final ItemStack item;
	private final String command;
	private String nextSection;

	public CPItem(Material material, String name, String command, String nextSection) {
		this.command = command;
		this.nextSection = nextSection;
		item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		List<String> desc = new ArrayList<>(Arrays.asList(name.split("\\|")));
		meta.setDisplayName(desc.get(0));
		if (desc.size() > 1) {
			desc.remove(0);
			meta.setLore(desc);
		}
		item.setItemMeta(meta);
	}

	public CPItem(ItemStack itemStack, String name, String command, String nextSection) {
		this.command = command;
		this.nextSection = nextSection;
		this.item = itemStack;
		ItemMeta meta = item.getItemMeta();
		List<String> desc = new ArrayList<>(Arrays.asList(name.split("\\|")));
		meta.setDisplayName(desc.get(0));
		if (desc.size() > 1) {
			desc.remove(0);
			meta.setLore(desc);
		}
		item.setItemMeta(meta);
	}

	public CPItem(ItemStack itemStack, String command) {
		this.command = command;
		this.nextSection = "";
		this.item = itemStack;
	}

	public void setLore(List<String> lore) {
		ItemMeta meta = item.getItemMeta();
		meta.setLore(lore);
		item.setItemMeta(meta);
	}

	public String getCommand() {
		return command;
	}

	public String getNextSection() {
		return nextSection;
	}

	public void setNextSection(String nextSection) {
		this.nextSection = nextSection;
	}

	public ItemStack getItem() {
		return item;
	}
}