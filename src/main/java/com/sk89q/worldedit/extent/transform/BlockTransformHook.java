package com.sk89q.worldedit.extent.transform;

import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.math.transform.Transform;

public interface BlockTransformHook {

    BaseBlock transformBlock(BaseBlock block, Transform transform);
}
