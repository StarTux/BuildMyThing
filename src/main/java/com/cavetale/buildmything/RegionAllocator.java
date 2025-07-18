package com.cavetale.buildmything;

import com.cavetale.core.struct.Vec2i;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class RegionAllocator {
    private final Game game;
    private final int regionSize;
    private final Map<Vec2i, GameRegion> regionMap = new HashMap<>();

    public GameRegion allocateRegion() {
        GameRegion result;
        result = tryToAllocate(0, 0);
        if (result != null) return result;
        for (int radius = 1;; radius += 1) { // ring
            for (int d = -radius; d < radius; d += 1) {
                result = tryToAllocate(d, -radius);
                if (result != null) return result;
            }
            for (int d = -radius; d < radius; d += 1) {
                result = tryToAllocate(radius, d);
                if (result != null) return result;
            }
            for (int d = -radius; d < radius; d += 1) {
                result = tryToAllocate(-d, radius);
                if (result != null) return result;
            }
            for (int d = -radius; d < radius; d += 1) {
                result = tryToAllocate(-radius, -d);
                if (result != null) return result;
            }
        }
    }

    private GameRegion tryToAllocate(int x, int y) {
        final Vec2i vec = Vec2i.of(x, y);
        if (regionMap.containsKey(vec)) return null;
        final Vec2i min = Vec2i.of(x * regionSize, y * regionSize);
        final Vec2i max = min.add(regionSize, regionSize);
        final GameRegion result = new GameRegion(game, vec, min, max);
        regionMap.put(vec, result);
        return result;
    }
}
