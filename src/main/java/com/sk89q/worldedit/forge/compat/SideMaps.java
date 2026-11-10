package com.sk89q.worldedit.forge.compat;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.math.transform.AffineTransform;

/**
 * Where a clipboard transform sends each side of a block, for mods that keep a facing as a side number
 * (0 down, 1 up, 2 north, 3 south, 4 west, 5 east).
 */
final class SideMaps {

    private static final Vector[] SIDE_VECTORS = { new Vector(0, -1, 0), new Vector(0, 1, 0), new Vector(0, 0, -1),
        new Vector(0, 0, 1), new Vector(-1, 0, 0), new Vector(1, 0, 0) };

    private SideMaps() {}

    /**
     * Worked out the way block directions are elsewhere, by transforming each side's direction.
     */
    static int[] sideMap(AffineTransform transform) {
        int[] sides = new int[6];
        Vector zero = transform.apply(Vector.ZERO);
        for (int side = 0; side < 6; side++) {
            Vector moved = transform.apply(SIDE_VECTORS[side])
                .subtract(zero);
            int best = side;
            double bestDot = Double.NEGATIVE_INFINITY;
            for (int candidate = 0; candidate < 6; candidate++) {
                double dot = moved.dot(SIDE_VECTORS[candidate]);
                if (dot > bestDot) {
                    bestDot = dot;
                    best = candidate;
                }
            }
            sides[side] = best;
        }
        return sides;
    }

    static boolean isIdentity(int[] sides) {
        for (int side = 0; side < 6; side++) {
            if (sides[side] != side) {
                return false;
            }
        }
        return true;
    }
}
