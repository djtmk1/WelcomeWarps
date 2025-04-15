package com.djtmk.wwarps.listeners;

import com.djtmk.wwarps.WWarps;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinLeaveEvents implements Listener {
    private final WWarps plugin;

    public JoinLeaveEvents(WWarps plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent e) {
        String message = plugin.getMessages().getPlayerMessage(e.getPlayer().getUniqueId());
        if (message != null) {
            e.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }
}