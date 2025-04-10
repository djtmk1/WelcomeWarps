package com.wasteofplastic.wwarps.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;
import java.util.UUID;

import com.wasteofplastic.wwarps.WWarps;

public class JoinLeaveEvents implements Listener {
    private final WWarps plugin;

    public JoinLeaveEvents(WWarps plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final UUID playerUUID = player.getUniqueId();
        final List<String> messages = plugin.getMessages().getMessages(playerUUID);
        if (messages != null) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage(ChatColor.AQUA + plugin.myLocale().newsHeadline);
                int i = 1;
                for (String message : messages) {
                    player.sendMessage(i++ + ": " + message);
                }
                plugin.getMessages().clearMessages(playerUUID);
            }, 40L);
        }
    }
}