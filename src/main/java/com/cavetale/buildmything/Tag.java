package com.cavetale.buildmything;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import org.bukkit.entity.Player;

@Data
public final class Tag implements Serializable {
    private boolean event;
    private boolean pause;
    private Map<UUID, Integer> scores = new HashMap<>();

    public void addScore(UUID uuid, int value) {
        scores.put(uuid, scores.getOrDefault(uuid, 0) + value);
    }

    public void addScore(Player player, int value) {
        addScore(player.getUniqueId(), value);
    }
}
