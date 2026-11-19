package com.sk89q.worldedit.forge.compat.rotation.types;

import com.sk89q.worldedit.math.transform.AffineTransform;

/**
 * Base interface for rotation mappings.
 */
public interface RotationBase {

    /**
     * Rotate the given metadata by the specified number of 90-degree steps.
     */
    int rotate(int meta, int steps);

    /**
     * Apply an arbitrary transform to the metadata.
     */
    default int transform(int meta, AffineTransform transform) {
        // Fallback uses rotate only
        double yRot = -transform.getRotations()
            .getY();
        int ticks = Math.round((float) (yRot / 90));
        return rotate(meta, ticks);
    }
}
