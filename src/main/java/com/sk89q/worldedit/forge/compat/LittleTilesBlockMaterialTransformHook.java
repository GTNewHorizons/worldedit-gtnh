package com.sk89q.worldedit.forge.compat;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.CompoundTagBuilder;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.transform.MaterialTransformHook;
import net.minecraft.block.Block;

import java.util.ArrayList;
import java.util.List;

public class LittleTilesBlockMaterialTransformHook implements MaterialTransformHook {

    @Override
    public BaseBlock transformMaterial(BaseBlock block, BaseBlock newMaterial) {
        CompoundTag nbt = block.getNbtData();
        if (nbt == null) {
            return block;
        }
        if (!"LittleTilesTileEntity".equals(nbt.getString("id"))) {
            return block;
        }

        int tileCount = nbt.getInt("tilesCount");
        if (tileCount <= 0) {
            return block;
        }

        CompoundTagBuilder fullTileBuilder = nbt.createBuilder();
        for (int i = 0; i < tileCount; i++) {
            CompoundTag subTileNbt = nbt.getCompoundtag("t" + i);
            if (subTileNbt == null)
                continue;

            CompoundTagBuilder subTileBuilder = subTileNbt.createBuilder();
            subTileBuilder.putString("block", Block.blockRegistry.getNameForObject(Block.getBlockById(newMaterial.getId())));
            subTileBuilder.putInt("meta", newMaterial.getData());
            fullTileBuilder.put("t" + i, subTileBuilder.build());
        }

        return new BaseBlock(block.getId(), block.getData(), fullTileBuilder.build());
    }
}
