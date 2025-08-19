package com.cavetale.buildmything;

import com.cavetale.core.event.hud.PlayerHudEvent;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;

@RequiredArgsConstructor
public final class LobbyListener implements Listener {
    private final BuildMyThingPlugin plugin;

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    private void onPlayerHud(PlayerHudEvent event) {
        if (!event.getPlayer().getWorld().equals(Bukkit.getWorlds().get(0))) {
            return;
        }
        plugin.onPlayerHud(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPlayerJoinDuringVote(PlayerJoinEvent event) {
        if (plugin.getGameVote() == null) return;
        final Player player = event.getPlayer();
        if (!player.getWorld().equals(Bukkit.getWorlds().get(0))) return;
        plugin.getGameVote().remind(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPlayerChangedWorldDuringVote(PlayerChangedWorldEvent event) {
        if (plugin.getGameVote() == null) return;
        final Player player = event.getPlayer();
        if (!player.equals(Bukkit.getWorlds().get(0))) return;
        plugin.getGameVote().remind(player);
    }
}
