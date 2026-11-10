/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.sk89q.worldedit.forge.compat;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.CompoundTagBuilder;
import com.sk89q.jnbt.ListTag;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.transform.BlockTransformHook;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;

import codechicken.lib.vec.Rotation;
import codechicken.multipart.MultipartHelper;
import codechicken.multipart.TileMultipart;

public class ForgeMultipartExistsCompat implements ForgeMultipartCompat, BlockTransformHook {

    @Override
    public TileEntity overrideTileEntity(World world, @Nullable NBTTagCompound tag, TileEntity normal) {
        if (tag == null) {
            return normal;
        }
        TileEntity tile = MultipartHelper.createTileFromNBT(world, tag);
        if (tile == null) {
            return normal;
        }
        return tile;
    }

    @Override
    public void sendDescPacket(World world, TileEntity entity) {
        if (entity instanceof TileMultipart) {
            TileMultipart multi = (TileMultipart) entity;
            // A tile loaded from NBT mid-game never gets the chunk load call, so its parts never join
            // the world (AE cables stay off their network until a restart, for example).
            if (!world.isRemote && world.getTileEntity(multi.xCoord, multi.yCoord, multi.zCoord) == multi) {
                multi.onChunkLoad();
            }
            MultipartHelper.sendDescPacket(world, multi);
        }
    }

    private int rotateSlot(int[][] edge_rotations, int slot, int ticks) {
        for (int type = 0; type < edge_rotations.length; type++) {
            int[] rotations = edge_rotations[type];
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

    private int[][] edge_rotations = { { 4, 10, 5, 8 }, // bottom edges
        { 6, 11, 7, 9 }, // top edges
        { 2, 0, 1, 3 }, // corner vertical
    };

    private int rotateEdgeSlot(Vector rot, int slot) {
        if (rot.getY() > 0) {
            int ticks = (int) Math.round(rot.getY() / 90);
            slot = rotateSlot(edge_rotations, slot, ticks);
        }
        if (rot.getX() < 0) {
            int ticks = (int) Math.round(rot.getX() / 90);
        }
        if (rot.getZ() > 0) {
            int ticks = (int) Math.round(rot.getZ() / 90);
        }
        return slot;
    }

    private int[][] post_rotations = { { 0 }, // Y
        { 1 }, // X
        { 2 }, // Z
    };

    private int rotatePostSlot(Vector rot, int slot) {
        if (rot.getY() > 0) {
            int ticks = (int) Math.round(rot.getY() / 90);
            slot = rotateSlot(post_rotations, slot, ticks);
        }
        if (rot.getX() < 0) {
            int ticks = (int) Math.round(rot.getX() / 90);
        }
        if (rot.getZ() > 0) {
            int ticks = (int) Math.round(rot.getZ() / 90);
        }
        return slot;
    }

    private int[][] cnr_rotations = { { 0, 2, 6, 4 }, // bottom
        { 1, 3, 7, 5 }, // top
    };

    private int rotateCnrSlot(Vector rot, int slot) {
        if (rot.getY() > 0) {
            int ticks = (int) Math.round(rot.getY() / 90);
            slot = rotateSlot(cnr_rotations, slot, ticks);
        }
        if (rot.getX() < 0) {
            int ticks = (int) Math.round(rot.getX() / 90);
        }
        if (rot.getZ() > 0) {
            int ticks = (int) Math.round(rot.getZ() / 90);
        }
        return slot;
    }

    private int[][] face_rotations = { { 0 }, // bottom
        { 2, 5, 3, 4 }, // edges
        { 1 }, // top
    };

    private int rotateFaceSlot(Vector rot, int slot) {
        if (rot.getY() > 0) {
            int ticks = (int) Math.round(rot.getY() / 90);
            slot = rotateSlot(face_rotations, slot, ticks);
        }
        if (rot.getX() < 0) {
            int ticks = (int) Math.round(rot.getX() / 90);
        }
        if (rot.getZ() > 0) {
            int ticks = (int) Math.round(rot.getZ() / 90);
        }
        return slot;
    }

    @Override
    public BaseBlock transformBlock(BaseBlock block, Transform transform) {
        CompoundTag nbt = block.getNbtData();
        if (nbt == null) {
            return block;
        }
        if (!"savedMultipart".equals(nbt.getString("id"))) {
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
        int[] sides = sideMap(affine);
        if (rot.lengthSq() == 0 && isIdentity(sides)) {
            return block;
        }

        CompoundTagBuilder builder = nbt.createBuilder();

        List<CompoundTag> parts = new ArrayList<>(nbt.getList("parts", CompoundTag.class));
        for (int i = 0; i < parts.size(); i++) {
            CompoundTag part = parts.get(i);
            String id = part.getString("id");
            if (!id.startsWith("mcr_")) {
                parts.set(i, transformFacePart(part, sides));
                continue;
            }
            byte shape = part.getByte("shape");
            int size = shape >> 4;
            int slot = shape & 0xF;
            switch (id) {
                case "mcr_edge":
                    slot = rotateEdgeSlot(rot, slot);
                    break;

                case "mcr_post":
                    slot = rotatePostSlot(rot, slot);
                    break;

                case "mcr_cnr":
                    slot = rotateCnrSlot(rot, slot);
                    break;

                case "mcr_face", "mcr_hllw":
                    slot = rotateFaceSlot(rot, slot);
                    break;

                default:
                    break;
            }
            shape = (byte) ((size << 4) | slot);

            CompoundTagBuilder partBuilder = part.createBuilder();
            partBuilder.putByte("shape", shape);
            parts.set(i, partBuilder.build());
        }
        builder.put("parts", new ListTag(CompoundTag.class, parts));

        return new BaseBlock(block.getId(), block.getData(), builder.build());
    }

    private static final Vector[] SIDE_VECTORS = { new Vector(0, -1, 0), new Vector(0, 1, 0), new Vector(0, 0, -1),
        new Vector(0, 0, 1), new Vector(-1, 0, 0), new Vector(1, 0, 0) };

    /**
     * Where the transform sends each side, worked out the way block directions are elsewhere, by
     * transforming the side's direction.
     */
    static int[] sideMap(AffineTransform transform) {
        int[] sides = new int[6];
        Vector zero = transform.apply(Vector.ZERO);
        for (int side = 0; side < 6; side++) {
            Vector moved = transform.apply(SIDE_VECTORS[side])
                .subtract(zero);
            int best = side;
            double bestDot = Double.NEGATIVE_INFINITY;
            for (int candidate = 0; candidate < 6; candidate++) {
                double dot = moved.dot(SIDE_VECTORS[candidate]);
                if (dot > bestDot) {
                    bestDot = dot;
                    best = candidate;
                }
            }
            sides[side] = best;
        }
        return sides;
    }

    private static boolean isIdentity(int[] sides) {
        for (int side = 0; side < 6; side++) {
            if (sides[side] != side) {
                return false;
            }
        }
        return true;
    }

    /**
     * Turn a part that sits on one face of the block, such as a ProjectRed wire ({@code side}) or gate
     * ({@code orient}: the side, and which way the gate faces on it).
     */
    static CompoundTag transformFacePart(CompoundTag part, int[] sides) {
        if (part.containsKey("side")) {
            int side = part.getByte("side");
            if (side < 0 || side > 5) {
                return part;
            }
            CompoundTagBuilder rotated = part.createBuilder()
                .putByte("side", (byte) sides[side]);
            if (part.containsKey("connMap")) {
                // Connections are stored relative to the old side, so ask the wire to work them out
                // again when it joins the world, as ProjectRed does for converted wires.
                rotated.putInt("connMap", part.getInt("connMap") | 0x80000000);
            }
            return rotated.build();
        }

        if (part.containsKey("orient")) {
            int orient = part.getByte("orient") & 0xFF;
            int side = orient >> 2;
            if (side > 5) {
                return part;
            }
            // ProjectRed's TFaceOrient: the gate's front is rotateSide(side, (rotation + 2) % 4).
            int front = Rotation.rotateSide(side, ((orient & 3) + 2) % 4);
            int newSide = sides[side];
            int newFront = sides[front];
            int rotation = (Rotation.rotationTo(newSide, newFront) + 2) % 4;
            return part.createBuilder()
                .putByte("orient", (byte) (newSide << 2 | rotation))
                .build();
        }

        if (part.containsKey("connMap")) {
            // A framed wire sits in the middle, but its connections are kept by direction too.
            return part.createBuilder()
                .putInt("connMap", part.getInt("connMap") | 0x80000000)
                .build();
        }

        return part;
    }

}
