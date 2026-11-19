package com.sk89q.worldedit.forge.compat.rotation.types;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.math.transform.AffineTransform;

/**
 * Simple four-direction rotation mapping.
 */
public class FourRotation implements RotationBase {

    private int[] metas = new int[4];
    /**
     * Mask of bits used for orientation detection.
     */
    private int mask = 3;

    public int[] getMetas() {
        return metas;
    }

    public void setMetas(int[] m) {
        this.metas = m;
        // compute orientation mask from provided metas
        mask = 0;
        for (int v : m) {
            mask |= v;
        }
    }

    private static final Vector[] DIRS = { new Vector(0, 0, -1), new Vector(1, 0, 0), new Vector(0, 0, 1),
        new Vector(-1, 0, 0) };

    private int find(int meta) {
        int orient = meta & mask;
        for (int i = 0; i < metas.length; i++) {
            if ((metas[i] & mask) == orient) return i;
        }
        return -1;
    }

    @Override
    public int rotate(int meta, int steps) {
        int extras = meta & ~mask;
        int idx = find(meta);
        if (idx == -1) return meta;
        int s = ((steps % 4) + 4) % 4;
        int newIdx = (idx + s) % 4;
        return (metas[newIdx] & mask) | extras;
    }

    @Override
    public int transform(int meta, AffineTransform transform) {
        int extras = meta & ~mask;
        int idx = find(meta);
        if (idx == -1) return meta;
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
        return (metas[bestIdx] & mask) | extras;
    }
}
