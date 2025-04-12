package com.wasteofplastic.wwarps;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Messages {
	private final WWarps plugin;
	private final Map<UUID, String> playerMessages;

	public Messages(WWarps plugin) {
		this.plugin = plugin;
		this.playerMessages = new HashMap<>();
	}

	public void setMessage(UUID playerUUID, String message) {
		playerMessages.put(playerUUID, message);
	}

	public String getPlayerMessage(UUID playerUUID) {
		return playerMessages.remove(playerUUID);
	}
}