package com.sk89q.worldedit.extent.transform;

import java.util.ArrayList;
import java.util.List;

import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.math.transform.Transform;

public class BlockTransformHooks implements BlockTransformHook {

    private final List<BlockTransformHook> hooks = new ArrayList<>();

    public void addHook(BlockTransformHook hook) {
        hooks.add(hook);
    }

    @Override
    public BaseBlock transformBlock(BaseBlock block, Transform transform) {
        for (BlockTransformHook hook : hooks) {
            block = hook.transformBlock(block, transform);
        }
        return block;
    }
}
