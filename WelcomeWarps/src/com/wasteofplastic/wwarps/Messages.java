package com.wasteofplastic.wwarps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.wasteofplastic.wwarps.util.Util;

public class Messages {
	private final WWarps plugin;
	private final HashMap<UUID, List<String>> messages = new HashMap<>();
	private YamlConfiguration messageStore;

	public Messages(WWarps plugin) {
		this.plugin = plugin;
	}

	public List<String> getMessages(UUID playerUUID) {
		return messages.get(playerUUID);
	}

	public void clearMessages(UUID playerUUID) {
		messages.remove(playerUUID);
	}

	public void saveMessages() {
		if (messageStore == null) {
			return;
		}
		plugin.getLogger().info("Saving offline messages...");
		try {
			final HashMap<String, Object> offlineMessages = new HashMap<>();
			for (UUID p : messages.keySet()) {
				offlineMessages.put(p.toString(), messages.get(p));
			}
			messageStore.set("messages", offlineMessages);
			Util.saveYamlFile(messageStore, "messages.yml");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean loadMessages() {
		plugin.getLogger().info("Loading offline messages...");
		try {
			messageStore = Util.loadYamlFile("messages.yml");
			if (messageStore.getConfigurationSection("messages") == null) {
				messageStore.createSection("messages");
			}
			HashMap<String, Object> temp = (HashMap<String, Object>) messageStore.getConfigurationSection("messages").getValues(true);
			for (String s : temp.keySet()) {
				List<String> messageList = messageStore.getStringList("messages." + s);
				if (!messageList.isEmpty()) {
					messages.put(UUID.fromString(s), messageList);
				}
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public List<String> get(UUID playerUUID) {
		return messages.get(playerUUID);
	}

	public void put(UUID playerUUID, List<String> playerMessages) {
		messages.put(playerUUID, playerMessages);
	}

	public boolean setMessage(UUID playerUUID, String message) {
		Player player = plugin.getServer().getPlayer(playerUUID);
		if (player != null && player.isOnline()) {
			return false;
		}
		List<String> playerMessages = get(playerUUID);
		if (playerMessages != null) {
			playerMessages.add(message);
		} else {
			playerMessages = new ArrayList<>(Collections.singletonList(message));
		}
		put(playerUUID, playerMessages);
		return true;
	}
}