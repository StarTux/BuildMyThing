package com.cavetale.buildmything;

import com.cavetale.core.event.block.PlayerBlockAbilityQuery;
import com.cavetale.core.event.block.PlayerBreakBlockEvent;
import com.cavetale.core.event.hud.PlayerHudEvent;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntitySpawnEvent;

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
        if (event.getPlayer().getWorld().equals(Bukkit.getWorlds().get(0))) {
            plugin.onPlayerHud(event);
            return;
        }
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

    /**
     * Avoid falling blocks.
     */
    @EventHandler
    private void onEntityChangeBlock(EntityChangeBlockEvent event) {
        final Game game = Game.in(event.getEntity().getWorld());
        if (game == null) return;
        if (event.getEntity() instanceof FallingBlock && event.getTo().isAir()) {
            event.setCancelled(true);
        }
    }

    /**
     * Avoid falling blocks.
     */
    @EventHandler
    private void onEntitySpawn(EntitySpawnEvent event) {
        final Game game = Game.in(event.getEntity().getWorld());
        if (game == null) return;
        switch (event.getEntity().getType()) {
        case FALLING_BLOCK:
            event.setCancelled(true);
        default: break;
        }
    }

    /**
     * Avoid spill.
     */
    @EventHandler
    private void onBlockFromTo(BlockFromToEvent event) {
        final Game game = Game.in(event.getBlock().getWorld());
        if (game == null) return;
        final BuildArea buildArea = game.findBuildAreaAt(event.getBlock());
        if (buildArea == null || !buildArea.getArea().contains(event.getToBlock())) {
            event.setCancelled(true);
            return;
        }
    }
}
