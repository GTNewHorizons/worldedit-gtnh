package com.sk89q.worldedit.forge.compat;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;

import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.transform.BlockTransformHook;
import com.sk89q.worldedit.forge.compat.rotation.RotationMapping;
import com.sk89q.worldedit.forge.compat.rotation.RotationMappings;
import com.sk89q.worldedit.forge.compat.rotation.types.RotationBase;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;

import cpw.mods.fml.common.registry.GameData;

/**
 * Fallback rotation handler for modded stairs, pillars, fence gates and trap doors.
 */
public class ModRotationBlockTransformHook implements BlockTransformHook {

    private final Map<Integer, RotationMapping> cache = new HashMap<>();

    /** Clear the cached lookups. */
    public void clearCache() {
        cache.clear();
    }

    private RotationMapping lookup(int id) {
        if (cache.containsKey(id)) {
            return cache.get(id);
        }
        Block mcBlock = Block.getBlockById(id);
        RotationMapping result = null;
        if (mcBlock != null) {
            Object identifier = GameData.getBlockRegistry()
                .getNameForObject(mcBlock);
            String name = identifier == null ? null : identifier.toString();
            if (name != null) {
                RotationMappings cfg = RotationMappings.getInstance();
                if (cfg != null) {
                    result = cfg.get(name);
                }
            }
        }
        cache.put(id, result);
        return result;
    }

    @Override
    public BaseBlock transformBlock(BaseBlock block, Transform transform) {
        if (!(transform instanceof AffineTransform affine)) {
            return block;
        }
        RotationMapping mapping = lookup(block.getId());
        if (mapping == null) {
            return block;
        }
        RotationBase base = mapping.getBase();
        int data = base.transform(block.getData(), affine);
        block.setData(data);
        return block;
    }
}
