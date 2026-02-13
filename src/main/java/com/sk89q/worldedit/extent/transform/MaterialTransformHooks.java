package com.sk89q.worldedit.extent.transform;

import com.sk89q.worldedit.blocks.BaseBlock;

import java.util.ArrayList;
import java.util.List;

public class MaterialTransformHooks implements MaterialTransformHook {

    private final List<MaterialTransformHook> hooks = new ArrayList<>();

    public void addHook(MaterialTransformHook hook) {
        hooks.add(hook);
    }

    @Override
    public BaseBlock transformMaterial(BaseBlock block, BaseBlock newMaterial) {
        for (MaterialTransformHook hook : hooks) {
            block = hook.transformMaterial(block, newMaterial);
        }
        return block;
    }
}
