package com.sk89q.worldedit.forge.compat;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.CompoundTagBuilder;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.transform.MaterialTransformHook;
import net.minecraft.block.Block;

import java.util.ArrayList;
import java.util.List;

public class ForgeMultipartBlockMaterialTransformHook implements MaterialTransformHook {

    @Override
    public BaseBlock transformMaterial(BaseBlock block, BaseBlock newMaterial) {
        CompoundTag nbt = block.getNbtData();
        if (nbt == null) {
            return block;
        }
        if (!"savedMultipart".equals(nbt.getString("id"))) {
            return block;
        }

        CompoundTagBuilder builder = nbt.createBuilder();

        List<CompoundTag> parts = new ArrayList<>(nbt.getList("parts", CompoundTag.class));
        for (int i = 0; i < parts.size(); i++) {
            CompoundTag part = parts.get(i);
            CompoundTagBuilder partBuilder = part.createBuilder();
            partBuilder.putString("material", Block.blockRegistry.getNameForObject(Block.getBlockById(newMaterial.getId())));
            parts.set(i, partBuilder.build());
        }
        builder.put("parts", new ListTag(CompoundTag.class, parts));

        return new BaseBlock(block.getId(), block.getData(), builder.build());
    }
}
