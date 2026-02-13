package com.sk89q.worldedit.extent.transform;

import com.sk89q.worldedit.blocks.BaseBlock;

public interface MaterialTransformHook {

    BaseBlock transformMaterial(BaseBlock block, BaseBlock newMaterial);

}
