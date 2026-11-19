package com.sk89q.worldedit.forge.compat.rotation;

import com.sk89q.worldedit.forge.compat.rotation.types.RotationBase;

/**
 * Holds the rotation type and mapping data for a block.
 */
public class RotationMapping {

    private RotationType type;
    private RotationBase base;

    public RotationMapping(RotationType type, RotationBase base) {
        this.type = type;
        this.base = base;
    }

    public RotationType getType() {
        return type;
    }

    public RotationBase getBase() {
        return base;
    }
}
