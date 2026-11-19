package com.sk89q.worldedit.forge.compat.rotation.types;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.forge.compat.rotation.RotationUtils;
import com.sk89q.worldedit.math.transform.AffineTransform;

/**
 * Rotation data for stairs with separate top and bottom arrays.
 */
public class StairRotation implements RotationBase {

    private int[] top = new int[4];
    private int[] bottom = new int[4];

    public int[] getTop() {
        return top;
    }

    public void setTop(int[] t) {
        this.top = t;
    }

    public int[] getBottom() {
        return bottom;
    }

    public void setBottom(int[] b) {
        this.bottom = b;
    }

    private static final Vector[] DIRS = { new Vector(0, 0, -1), // north
        new Vector(1, 0, 0), // east
        new Vector(0, 0, 1), // south
        new Vector(-1, 0, 0) // west
    };

    private int find(int meta, int[] arr) {
        for (int i = 0; i < arr.length; i++) if (arr[i] == meta) return i;
        return -1;
    }

    @Override
    public int rotate(int meta, int steps) {
        boolean big = meta >= 8;
        int m = big ? meta - 8 : meta;
        int idx = find(m, bottom);
        int[] arr = bottom;
        if (idx == -1) {
            idx = find(m, top);
            if (idx == -1) return meta;
            arr = top;
        }
        int s = ((steps % 4) + 4) % 4;
        int newIdx = (idx + s) % 4;
        int result = arr[newIdx];
        return big ? result + 8 : result;
    }

    @Override
    public int transform(int meta, AffineTransform transform) {
        boolean big = meta >= 8;
        int m = big ? meta - 8 : meta;
        int idx = find(m, bottom);
        int[] arr = bottom;
        if (idx == -1) {
            idx = find(m, top);
            if (idx == -1) return meta;
            arr = top;
        }
        Vector out = transform.apply(DIRS[idx])
            .subtract(transform.apply(Vector.ZERO))
            .normalize();
        double best = -2;
        int bestIdx = idx;
        for (int i = 0; i < 4; i++) {
            double dot = DIRS[i].normalize()
                .dot(out);
            if (dot > best) {
                best = dot;
                bestIdx = i;
            }
        }
        // Vertical flips swap regular and upside-down stairs
        if (RotationUtils.flipsVertically(transform)) {
            arr = (arr == bottom) ? top : bottom;
        }
        int result = arr[bestIdx];
        return big ? result + 8 : result;
    }
}
