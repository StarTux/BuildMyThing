package com.cavetale.buildmything.phase;

import com.cavetale.buildmything.Game;
import java.time.Duration;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@Data
@EqualsAndHashCode(callSuper = true)
public final class BuildPhase extends TimedPhase {
    private final Game game;
    private List<Material> initialInventory = List.of(Material.STONE_BRICKS, // gray
                                                      Material.IRON_BLOCK, // white
                                                      Material.DEEPSLATE_BRICKS, // black
                                                      Material.MOSS_BLOCK, // green
                                                      Material.GOLD_BLOCK, // yellow
                                                      Material.STRIPPED_OAK_WOOD, // brown
                                                      Material.REDSTONE_BLOCK, // red
                                                      Material.PURPUR_BLOCK, // purple
                                                      Material.LAPIS_BLOCK); // blue

    public BuildPhase(final Game game, final Duration duration) {
        super(duration);
        this.game = game;
    }

    @Override
    public void start() {
        super.start();
        game.setBuildingAllowed(true);
        for (Player player : game.getOnlinePlayers()) {
            for (int i = 0; i < initialInventory.size(); i += 1) {
                player.getInventory().setItem(i, new ItemStack(initialInventory.get(i)));
            }
            player.playSound(player, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1f, 1f);
        }
    }

    @Override
    public void onFinished() {
        game.setBuildingAllowed(false);
        for (Player player : game.getOnlinePlayers()) {
            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 0.5f);
        }
    }
}
