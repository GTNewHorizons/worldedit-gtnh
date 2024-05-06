package com.sk89q.worldedit.forge.compat;

import static com.carpentersblocks.data.Slope.ID_OBL_EXT_NEG_NE;
import static com.carpentersblocks.data.Slope.ID_OBL_EXT_NEG_NW;
import static com.carpentersblocks.data.Slope.ID_OBL_EXT_NEG_SE;
import static com.carpentersblocks.data.Slope.ID_OBL_EXT_NEG_SW;
import static com.carpentersblocks.data.Slope.ID_OBL_EXT_POS_NE;
import static com.carpentersblocks.data.Slope.ID_OBL_EXT_POS_NW;
import static com.carpentersblocks.data.Slope.ID_OBL_EXT_POS_SE;
import static com.carpentersblocks.data.Slope.ID_OBL_EXT_POS_SW;
import static com.carpentersblocks.data.Slope.ID_OBL_INT_NEG_NE;
import static com.carpentersblocks.data.Slope.ID_OBL_INT_NEG_NW;
import static com.carpentersblocks.data.Slope.ID_OBL_INT_NEG_SE;
import static com.carpentersblocks.data.Slope.ID_OBL_INT_NEG_SW;
import static com.carpentersblocks.data.Slope.ID_OBL_INT_POS_NE;
import static com.carpentersblocks.data.Slope.ID_OBL_INT_POS_NW;
import static com.carpentersblocks.data.Slope.ID_OBL_INT_POS_SE;
import static com.carpentersblocks.data.Slope.ID_OBL_INT_POS_SW;
import static com.carpentersblocks.data.Slope.ID_PRISM_1P_POS_E;
import static com.carpentersblocks.data.Slope.ID_PRISM_1P_POS_N;
import static com.carpentersblocks.data.Slope.ID_PRISM_1P_POS_S;
import static com.carpentersblocks.data.Slope.ID_PRISM_1P_POS_W;
import static com.carpentersblocks.data.Slope.ID_PRISM_2P_POS_NE;
import static com.carpentersblocks.data.Slope.ID_PRISM_2P_POS_NW;
import static com.carpentersblocks.data.Slope.ID_PRISM_2P_POS_SE;
import static com.carpentersblocks.data.Slope.ID_PRISM_2P_POS_SW;
import static com.carpentersblocks.data.Slope.ID_PRISM_3P_POS_NSE;
import static com.carpentersblocks.data.Slope.ID_PRISM_3P_POS_NSW;
import static com.carpentersblocks.data.Slope.ID_PRISM_3P_POS_NWE;
import static com.carpentersblocks.data.Slope.ID_PRISM_3P_POS_SWE;
import static com.carpentersblocks.data.Slope.ID_PRISM_NEG;
import static com.carpentersblocks.data.Slope.ID_PRISM_POS;
import static com.carpentersblocks.data.Slope.ID_PRISM_POS_4P;
import static com.carpentersblocks.data.Slope.ID_PRISM_WEDGE_POS_E;
import static com.carpentersblocks.data.Slope.ID_PRISM_WEDGE_POS_N;
import static com.carpentersblocks.data.Slope.ID_PRISM_WEDGE_POS_S;
import static com.carpentersblocks.data.Slope.ID_PRISM_WEDGE_POS_W;
import static com.carpentersblocks.data.Slope.ID_WEDGE_EXT_NEG_NE;
import static com.carpentersblocks.data.Slope.ID_WEDGE_EXT_NEG_NW;
import static com.carpentersblocks.data.Slope.ID_WEDGE_EXT_NEG_SE;
import static com.carpentersblocks.data.Slope.ID_WEDGE_EXT_NEG_SW;
import static com.carpentersblocks.data.Slope.ID_WEDGE_EXT_POS_NE;
import static com.carpentersblocks.data.Slope.ID_WEDGE_EXT_POS_NW;
import static com.carpentersblocks.data.Slope.ID_WEDGE_EXT_POS_SE;
import static com.carpentersblocks.data.Slope.ID_WEDGE_EXT_POS_SW;
import static com.carpentersblocks.data.Slope.ID_WEDGE_INT_NEG_NE;
import static com.carpentersblocks.data.Slope.ID_WEDGE_INT_NEG_NW;
import static com.carpentersblocks.data.Slope.ID_WEDGE_INT_NEG_SE;
import static com.carpentersblocks.data.Slope.ID_WEDGE_INT_NEG_SW;
import static com.carpentersblocks.data.Slope.ID_WEDGE_INT_POS_NE;
import static com.carpentersblocks.data.Slope.ID_WEDGE_INT_POS_NW;
import static com.carpentersblocks.data.Slope.ID_WEDGE_INT_POS_SE;
import static com.carpentersblocks.data.Slope.ID_WEDGE_INT_POS_SW;
import static com.carpentersblocks.data.Slope.ID_WEDGE_NE;
import static com.carpentersblocks.data.Slope.ID_WEDGE_NEG_E;
import static com.carpentersblocks.data.Slope.ID_WEDGE_NEG_N;
import static com.carpentersblocks.data.Slope.ID_WEDGE_NEG_S;
import static com.carpentersblocks.data.Slope.ID_WEDGE_NEG_W;
import static com.carpentersblocks.data.Slope.ID_WEDGE_NW;
import static com.carpentersblocks.data.Slope.ID_WEDGE_POS_E;
import static com.carpentersblocks.data.Slope.ID_WEDGE_POS_N;
import static com.carpentersblocks.data.Slope.ID_WEDGE_POS_S;
import static com.carpentersblocks.data.Slope.ID_WEDGE_POS_W;
import static com.carpentersblocks.data.Slope.ID_WEDGE_SE;
import static com.carpentersblocks.data.Slope.ID_WEDGE_SW;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.CompoundTagBuilder;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.transform.BlockTransformHook;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;

public class CarpentersBlocksBlockTransformHook implements BlockTransformHook {

    private int[][] rotationGroups = { { ID_WEDGE_SE, ID_WEDGE_NE, ID_WEDGE_NW, ID_WEDGE_SW },
        { ID_WEDGE_NEG_N, ID_WEDGE_NEG_W, ID_WEDGE_NEG_S, ID_WEDGE_NEG_E },
        { ID_WEDGE_POS_N, ID_WEDGE_POS_W, ID_WEDGE_POS_S, ID_WEDGE_POS_E },
        { ID_WEDGE_INT_NEG_SE, ID_WEDGE_INT_NEG_NE, ID_WEDGE_INT_NEG_NW, ID_WEDGE_INT_NEG_SW },
        { ID_WEDGE_INT_POS_SE, ID_WEDGE_INT_POS_NE, ID_WEDGE_INT_POS_NW, ID_WEDGE_INT_POS_SW },
        { ID_WEDGE_EXT_NEG_SE, ID_WEDGE_EXT_NEG_NE, ID_WEDGE_EXT_NEG_NW, ID_WEDGE_EXT_NEG_SW },
        { ID_WEDGE_EXT_POS_SE, ID_WEDGE_EXT_POS_NE, ID_WEDGE_EXT_POS_NW, ID_WEDGE_EXT_POS_SW },
        { ID_OBL_INT_NEG_SE, ID_OBL_INT_NEG_NE, ID_OBL_INT_NEG_NW, ID_OBL_INT_NEG_SW },
        { ID_OBL_INT_POS_SE, ID_OBL_INT_POS_NE, ID_OBL_INT_POS_NW, ID_OBL_INT_POS_SW },
        { ID_OBL_EXT_NEG_SE, ID_OBL_EXT_NEG_NE, ID_OBL_EXT_NEG_NW, ID_OBL_EXT_NEG_SW },
        { ID_OBL_EXT_POS_SE, ID_OBL_EXT_POS_NE, ID_OBL_EXT_POS_NW, ID_OBL_EXT_POS_SW }, { ID_PRISM_NEG, ID_PRISM_POS },
        { ID_PRISM_1P_POS_N, ID_PRISM_1P_POS_W, ID_PRISM_1P_POS_S, ID_PRISM_1P_POS_E },
        { ID_PRISM_2P_POS_SE, ID_PRISM_2P_POS_NE, ID_PRISM_2P_POS_NW, ID_PRISM_2P_POS_SW },
        { ID_PRISM_3P_POS_NWE, ID_PRISM_3P_POS_NSW, ID_PRISM_3P_POS_SWE, ID_PRISM_3P_POS_NSE }, { ID_PRISM_POS_4P },
        { ID_PRISM_WEDGE_POS_N, ID_PRISM_WEDGE_POS_W, ID_PRISM_WEDGE_POS_S, ID_PRISM_WEDGE_POS_E }, };

    private int rotateSlot(int slot, int ticks) {
        for (int type = 0; type < rotationGroups.length; type++) {
            int[] rotations = rotationGroups[type];
            for (int i = 0; i < rotations.length; i++) {
                if (rotations[i] == slot) {
                    // make sure ticks is positive
                    ticks = ticks % rotations.length + rotations.length;
                    slot = rotations[(i + ticks) % rotations.length];
                    return slot;
                }
            }
        }
        return -1;
    }

    @Override
    public BaseBlock transformBlock(BaseBlock block, Transform transform) {
        CompoundTag nbt = block.getNbtData();
        if (nbt == null) {
            return block;
        }
        if (!"TileEntityCarpentersBlock".equals(nbt.getString("id"))) {
            return block;
        }
        if (!(transform instanceof AffineTransform affine)) {
            return block;
        }
        Vector rot = affine.getRotations();
        if (rot.getX() > 0) {
            System.out.println("X rotation is currently unsupported");
            rot = rot.setX(0);
        }
        if (rot.getZ() > 0) {
            System.out.println("Z rotation is currently unsupported");
            rot = rot.setZ(0);
        }
        if (rot.lengthSq() == 0) {
            return block;
        }
        CompoundTagBuilder builder = nbt.createBuilder();
        int md = nbt.asInt("cbMetadata");

        if (rot.getY() > 0) {
            int ticks = (int) Math.round(rot.getY() / 90);
            md = rotateSlot(md, ticks);
        }
        builder.putInt("cbMetadata", md);
        return new BaseBlock(block.getId(), block.getData(), builder.build());
    }
}
