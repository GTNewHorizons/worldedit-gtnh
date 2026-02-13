package com.sk89q.worldedit.forge.compat;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.CompoundTagBuilder;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.transform.MaterialTransformHook;
import net.minecraft.block.Block;

public class ArchitectureCraftBlockMaterialTransformHook implements MaterialTransformHook {

    @Override
    public BaseBlock transformMaterial(BaseBlock block, BaseBlock newMaterial) {
        CompoundTag nbt = block.getNbtData();
        if (nbt == null) {
            return block;
        }
        if (!"gcewing.shape".equals(nbt.getString("id"))) {
            return block;
        }

        CompoundTagBuilder newNbt = nbt.createBuilder();
        newNbt.putString("BaseName", Block.blockRegistry.getNameForObject(Block.getBlockById(newMaterial.getId())));
        newNbt.putInt("BaseData", newMaterial.getData());
        return new BaseBlock(block.getId(), block.getData(), newNbt.build());
    }
}
