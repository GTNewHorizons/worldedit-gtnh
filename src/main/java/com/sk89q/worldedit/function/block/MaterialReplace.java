package com.sk89q.worldedit.function.block;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.transform.MaterialTransformHook;
import com.sk89q.worldedit.forge.ForgeWorldData;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.world.registry.LegacyWorldData;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Replaces the blocks material with a given pattern.
 */
public class MaterialReplace implements RegionFunction {

    private final Extent extent;
    private Pattern pattern;

    /**
     * Create a new instance.
     *
     * @param extent  an extent
     * @param pattern a pattern
     */
    public MaterialReplace(Extent extent, Pattern pattern) {
        checkNotNull(extent);
        checkNotNull(pattern);
        this.extent = extent;
        this.pattern = pattern;
    }

    @Override
    public boolean apply(Vector position) throws WorldEditException {
        MaterialTransformHook hook = ForgeWorldData.getInstance().getMaterialTransformHook();
        BaseBlock transformedBaseBlock = hook.transformMaterial(extent.getBlock(position),pattern.apply(position));
        extent.setBlock(position, transformedBaseBlock);
        return true;
    }
}
