package com.cavetale.buildmything;

import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec2i;
import com.cavetale.core.struct.Vec3i;
import java.util.UUID;
import lombok.Data;

@Data
public final class GameRegion {
    private final Game game;
    private final Vec2i allocatorVector;
    private final Vec2i min;
    private final Vec2i max;
    private final Vec2i center;
    private UUID currentPlayer;

    public GameRegion(final Game game, final Vec2i allocatorVector, final Vec2i min, final Vec2i max) {
        this.game = game;
        this.allocatorVector = allocatorVector;
        this.min = min;
        this.max = max;
        this.center = Vec2i.of((min.x + max.x) / 2,
                               (min.z + max.z) / 2);
    }

    public Vec3i getBaseVector() {
        return Vec3i.of(center.x, 65, center.z);
    }

    public Cuboid createCentralSelection(int sizeX, int sizeY, int sizeZ) {
        final Vec3i base = getBaseVector();
        final Vec3i a = base.add(-sizeX / 2, 0, -sizeZ / 2);
        final Vec3i b = a.add(sizeX, sizeY, sizeZ);
        return Cuboid.containing(a, b);
    }
}
