package com.cavetale.buildmything;

import com.cavetale.core.struct.Cuboid;
import java.util.ArrayList;
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
    private GameRegion currentRegion;
    private final List<Cuboid> buildAreas = new ArrayList<>();
    private boolean playing = false;

    public GamePlayer(final Player player) {
        this.uuid = player.getUniqueId();
        this.name = player.getName();
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public boolean buildAreasContain(Block block) {
        for (Cuboid buildArea : buildAreas) {
            if (buildArea.contains(block)) return true;
        }
        return false;
    }
}
