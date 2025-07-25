package com.cavetale.buildmything;

import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec2i;
import com.cavetale.core.struct.Vec3i;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.Data;

@Data
public final class GameRegion {
    private final Game game;
    private final Vec2i allocatorVector;
    private final Vec2i min;
    private final Vec2i max;
    private final Vec2i center;

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

    public List<GameRegion> createSubregions(int sizeX, int sizeZ, int border, int count) {
        final List<GameRegion> result = new ArrayList<>();
        result.add(createSubRegion(0, 0, sizeX, sizeZ, border));
        for (int radius = 1; result.size() < count; radius += 1) { // ring
            for (int d = -radius; d < radius; d += 1) {
                result.add(createSubRegion(d, -radius, sizeX, sizeZ, border));
            }
            for (int d = -radius; d < radius; d += 1) {
                result.add(createSubRegion(radius, d, sizeX, sizeZ, border));
            }
            for (int d = -radius; d < radius; d += 1) {
                result.add(createSubRegion(-d, radius, sizeX, sizeZ, border));
            }
            for (int d = -radius; d < radius; d += 1) {
                result.add(createSubRegion(-radius, -d, sizeX, sizeZ, border));
            }
        }
        result.sort(Comparator.comparing(r -> Math.abs(r.allocatorVector.x) + Math.abs(r.allocatorVector.z)));
        return result;
    }

    private GameRegion createSubRegion(int x, int z, int sizeX, int sizeZ, int border) {
        final Vec2i newVector = Vec2i.of(x, z);
        final Vec2i newMin = center.add(x * (sizeX + border) - (sizeX / 2), z * (sizeZ + border) - sizeZ / 2);
        final Vec2i newMax = newMin.add(sizeX, sizeZ);
        final GameRegion result = new GameRegion(game, newVector, newMin, newMax);
        return result;
    }
}
