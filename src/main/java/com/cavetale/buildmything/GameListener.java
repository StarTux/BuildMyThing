package com.cavetale.buildmything;

import com.cavetale.core.event.block.PlayerBlockAbilityQuery;
import com.cavetale.core.event.block.PlayerBreakBlockEvent;
import com.cavetale.core.event.hud.PlayerHudEvent;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

@RequiredArgsConstructor
public final class GameListener implements Listener {
    private final BuildMyThingPlugin plugin;

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    private void onBlockPlace(BlockPlaceEvent event) {
        checkBlockEdit(event.getPlayer(), event.getBlock(), event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    private void onBlockBreak(BlockBreakEvent event) {
        checkBlockEdit(event.getPlayer(), event.getBlock(), event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    private void onPlayerBlockAbility(PlayerBlockAbilityQuery query) {
        checkBlockEdit(query.getPlayer(), query.getBlock(), query);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    private void onPlayerBreakBlock(PlayerBreakBlockEvent event) {
        checkBlockEdit(event.getPlayer(), event.getBlock(), event);
    }

    @EventHandler
    private void onPlayerHud(PlayerHudEvent event) {
        final Game game = Game.in(event.getPlayer().getWorld());
        if (game == null || game.getMode() == null) return;
        game.onPlayerHud(event);
    }

    private static void checkBlockEdit(Player player, Block block, Cancellable event) {
        if (player.isOp()) return;
        final Game game = Game.in(block.getWorld());
        if (game == null) {
            event.setCancelled(true);
            return;
        }
        if (!game.playerCanBuild(player, block)) {
            event.setCancelled(true);
        }
    }
}
