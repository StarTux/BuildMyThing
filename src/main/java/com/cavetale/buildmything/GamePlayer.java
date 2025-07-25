package com.cavetale.buildmything;

import java.util.List;
import java.util.UUID;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

@Data
public final class GamePlayer {
    private final UUID uuid;
    private String name;
    /**
     * This area belongs to the player and they can build there as
     * long as Game::isBuildingAllowed yields true.
     */
    private BuildArea buildArea;
    private BuildArea guessArea;
    private boolean playing = false;
    // RatePhase
    private List<BuildArea> rateAreaList;
    private int rateIndex;

    public GamePlayer(final Player player) {
        this.uuid = player.getUniqueId();
        this.name = player.getName();
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public boolean canBuildHere(Block block) {
        return buildArea != null && buildArea.getArea().contains(block);
    }
}
