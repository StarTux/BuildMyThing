package com.cavetale.buildmything;

import com.cavetale.core.event.block.PlayerBlockAbilityQuery;
import com.cavetale.core.event.block.PlayerBreakBlockEvent;
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.player.PlayerTPAEvent;
import com.destroystokyo.paper.MaterialTags;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;

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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    private void onEntityPlace(EntityPlaceEvent event) {
        if (event.getEntity() instanceof CommandMinecart) {
            plugin.getLogger().warning(event.getPlayer().getName() + " tried placing Command Minecart");
            event.setCancelled(true);
            return;
        }
        checkBlockEdit(event.getPlayer(), event.getEntity().getLocation().getBlock(), event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    private void onPlayerBucketFill(PlayerBucketFillEvent event) {
        checkBlockEdit(event.getPlayer(), event.getBlock(), event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    private void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        checkBlockEdit(event.getPlayer(), event.getBlock(), event);
    }

    /**
     * Control spawn eggs.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    private void onPlayerInteractSpawnEgg(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || !event.hasBlock() || !event.hasItem()) {
            return;
        }
        if (!MaterialTags.SPAWN_EGGS.isTagged(event.getItem().getType())) {
            return;
        }
        checkBlockEdit(event.getPlayer(), event.getClickedBlock().getRelative(event.getBlockFace()), event);
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

    @EventHandler
    private void onPlayerHud(PlayerHudEvent event) {
        final Game game = Game.in(event.getPlayer().getWorld());
        if (game == null || game.getMode() == null) return;
        game.onPlayerHud(event);
    }

    /**
     * Avoid falling blocks.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
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
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
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
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    private void onBlockFromTo(BlockFromToEvent event) {
        final Game game = Game.in(event.getBlock().getWorld());
        if (game == null) return;
        if (!game.isBuildingAllowed()) {
            event.setCancelled(true);
        }
        final BuildArea buildArea = game.findBuildAreaAt(event.getBlock());
        if (buildArea == null || !buildArea.getArea().contains(event.getToBlock())) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    private void onCreatureSpawn(CreatureSpawnEvent event) {
        final Game game = Game.in(event.getEntity().getWorld());
        if (game == null) return;
        switch (event.getSpawnReason()) {
        case SPAWNER_EGG:
            if (!game.isBuildingAllowed()) {
                event.setCancelled(true);
                return;
            }
            event.getEntity().setAI(false);
            event.getEntity().setGravity(false);
            event.getEntity().setSilent(true);
        default: break;
        }
    }

    /**
     * If a player spawns late during an event, we bring them to the
     * current game spawn.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPlayerJoinDuringEvent(PlayerJoinEvent event) {
        if (!plugin.getTag().isEvent()) return;
        final Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isValid()) return;
                if (Game.in(player.getWorld()) != null) return;
                if (plugin.getGames().isEmpty()) return;
                plugin.getGames().get(0).bringPlayer(player);
                player.setGameMode(GameMode.SPECTATOR);
            }, 20L);
    }

    /**
     * Deny TPA if player is in a game.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPlayerTPA(PlayerTPAEvent event) {
        if (Game.in(event.getTarget().getWorld()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        final Game game = Game.in(player.getWorld());
        if (game == null) return;
        event.setCancelled(true);
        if (event.getCause() != EntityDamageEvent.DamageCause.VOID) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> game.onPlayerVoidDamage(player));
    }
}
