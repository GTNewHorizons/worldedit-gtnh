package com.sk89q.worldedit.forge.compat;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.CompoundTagBuilder;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.transform.BlockTransformHook;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;

public class ArchitectureCraftBlockTransformHook implements BlockTransformHook {

    private byte[] y_rotations = { 2, // NORTH
        5, // EAST
        3, // SOUTH
        4, // WEST
    };

    private byte[] x_rotations = { 0, // DOWN
        3, // SOUTH
        1, // UP
        2, // NORTH
    };

    private byte[] z_rotations = { 0, // DOWN
        5, // EAST
        1, // UP
        4, // WEST
    };

    @Override
    public BaseBlock transformBlock(BaseBlock block, Transform transform) {
        CompoundTag nbt = block.getNbtData();
        if (nbt == null) {
            return block;
        }
        if (!"gcewing.shape".equals(nbt.getString("id"))) {
            return block;
        }
        if (!(transform instanceof AffineTransform affine)) {
            return block;
        }
        Vector rot = affine.getRotations();
        if (rot.lengthSq() == 0) {
            return block;
        }
        rot = rot.setX((360 - rot.getX()) % 360) // X rotation is opposite in architecture craft
            .setZ((360 - rot.getZ()) % 360); // Z rotation is opposite in architecture craft
        byte side = nbt.getByte("side");
        byte turn = nbt.getByte("turn");
        if (rot.getY() > 0) {
            int ticks = (int) Math.round(rot.getY() / 90);
            if (side == 0) { // DOWN
                // rotate positive (clockwise)
                turn = (byte) ((turn + ticks) % 4);
            } else if (side == 1) { // UP
                // rotate negative (anticlockwise)
                // add 4 in case turn < ticks
                turn = (byte) ((turn + 4 - ticks) % 4);
            } else {
                int idx = rotationIndex(y_rotations, side);
                side = y_rotations[(idx + 4 - ticks) % 4];
            }
        }
        if (rot.getX() > 0) {
            int ticks = (int) Math.round(rot.getX() / 90);
            if (side == 4) { // WEST
                // rotate positive (clockwise)
                turn = (byte) ((turn + ticks) % 4);
            } else if (side == 5) { // EAST
                // rotate negative (anticlockwise)
                // add 4 in case turn < ticks
                turn = (byte) ((turn + 4 - ticks) % 4);
            } else {
                int idx = rotationIndex(x_rotations, side);
                idx = (idx + ticks) % 4;
                side = x_rotations[idx];
                if (side == 3) {
                    turn = (byte) ((turn + 2) % 4);
                }
            }
        }
        if (rot.getZ() > 0) {
            int ticks = (int) Math.round(rot.getZ() / 90);
            if (side == 3) { // SOUTH
                // rotate positive (clockwise)
                turn = (byte) ((turn + ticks) % 4);
            } else if (side == 2) { // NORTH
                // rotate negative (anticlockwise)
                // add 4 in case turn < ticks
                turn = (byte) ((turn + 4 - ticks) % 4);
            } else {
                int idx = rotationIndex(z_rotations, side);
                side = z_rotations[(idx + 4 - ticks) % 4];
                turn = (byte) ((turn - ticks) % 4);
            }
        }
        CompoundTagBuilder newNbt = nbt.createBuilder();
        newNbt.putByte("side", side);
        newNbt.putByte("turn", turn);
        return new BaseBlock(block.getId(), block.getData(), newNbt.build());
    }

    private int rotationIndex(byte[] rotations, byte side) {
        for (int i = 0; i < rotations.length; i++) {
            if (rotations[i] == side) {
                return i;
            }
        }
        return -1;
    }
}
