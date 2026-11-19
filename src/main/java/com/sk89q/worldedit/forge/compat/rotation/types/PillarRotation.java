package com.sk89q.worldedit.forge.compat.rotation.types;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.forge.compat.rotation.RotationUtils;
import com.sk89q.worldedit.math.transform.AffineTransform;

/**
 * Rotation mapping for pillar blocks.
 */
public class PillarRotation implements RotationBase {

    /**
     * Rotation groups in order [y,x,z].
     */
    private int[][] groups = { { 0, 4, 8 } };
    /**
     * Orientation mask calculated from groups.
     */
    private int mask = 0xC;

    /**
     * Get the rotation groups.
     */
    public int[][] getGroups() {
        return groups;
    }

    /**
     * Replace the rotation groups and recompute orientation mask.
     */
    public void setGroups(int[][] g) {
        this.groups = g;
        computeMask();
    }

    // Legacy accessors for single-group configuration
    public int getX() {
        return groups[0][1];
    }

    public void setX(int x) {
        groups[0][1] = x;
        computeMask();
    }

    public int getY() {
        return groups[0][0];
    }

    public void setY(int y) {
        groups[0][0] = y;
        computeMask();
    }

    public int getZ() {
        return groups[0][2];
    }

    public void setZ(int z) {
        groups[0][2] = z;
        computeMask();
    }

    private void computeMask() {
        int m = -1;
        boolean first = true;
        for (int[] g : groups) {
            int bits = g[0] | g[1] | g[2];
            if (first) {
                m = bits;
                first = false;
            } else {
                m &= bits;
            }
        }
        mask = m;
    }

    @Override
    public int rotate(int meta, int steps) {
        int s = Math.abs(steps) % 4;
        if (s == 0) return meta;
        int extras = meta & ~mask;
        int orientation = meta & mask;
        for (int[] g : groups) {
            if (orientation == g[0]) {
                return g[0] | extras; // vertical unaffected
            } else if (orientation == g[1]) {
                int out = (s % 2 == 1) ? g[2] : g[1];
                return (out & mask) | extras;
            } else if (orientation == g[2]) {
                int out = (s % 2 == 1) ? g[1] : g[2];
                return (out & mask) | extras;
            }
        }
        return meta;
    }

    /**
     * Axis directions in group order [y, x, z].
     */
    private static final Vector[] AXES = { new Vector(0, 1, 0), new Vector(1, 0, 0), new Vector(0, 0, 1) };

    @Override
    public int transform(int meta, AffineTransform transform) {
        int extras = meta & ~mask;
        int orientation = meta & mask;
        for (int[] g : groups) {
            int axis = -1;
            for (int i = 0; i < 3; i++) {
                if ((g[i] & mask) == orientation) {
                    axis = i;
                    break;
                }
            }
            if (axis == -1) continue;

            // Project the pillar axis through the transform and pick the nearest
            // axis by absolute dot product, which handles rotations and flips
            Vector out = RotationUtils.transformDirection(transform, AXES[axis]);
            int bestAxis = axis;
            double best = -1;
            for (int i = 0; i < 3; i++) {
                double dot = Math.abs(
                    AXES[i].normalize()
                        .dot(out));
                if (dot > best) {
                    best = dot;
                    bestAxis = i;
                }
            }
            return (g[bestAxis] & mask) | extras;
        }
        return meta;
    }
}
