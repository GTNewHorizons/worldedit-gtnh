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
        int[] sides = sideMap(affine);
        if (isIdentity(sides)) {
            return block;
        }

        CompoundTagBuilder builder = nbt.createBuilder();

        List<CompoundTag> parts = new ArrayList<>(nbt.getList("parts", CompoundTag.class));
        for (int i = 0; i < parts.size(); i++) {
            CompoundTag part = parts.get(i);
            String id = part.getString("id");
            parts.set(i, id.startsWith("mcr_") ? transformMicroblock(part, sides) : transformFacePart(part, sides));
        }
        builder.put("parts", new ListTag(CompoundTag.class, parts));

        return new BaseBlock(block.getId(), block.getData(), builder.build());
    }

    /**
     * Move a microblock to the slot the transform sends it to. ForgeMultipart keeps the slot in the low
     * bits of {@code shape} and the size in the high bits, and every slot kind is a direction: a face
     * or hollow is a side, a corner is a sign per axis, an edge is a sign on each of its two free axes,
     * and a post is an axis.
     */
    static CompoundTag transformMicroblock(CompoundTag part, int[] sides) {
        int shape = part.getByte("shape") & 0xFF;
        int size = shape >> 4;
        int slot = shape & 0xF;
        switch (part.getString("id")) {
            case "mcr_face":
            case "mcr_hllw":
                if (slot > 5) {
                    return part;
                }
                slot = sides[slot];
                break;
            case "mcr_cnr":
                slot = transformCorner(slot & 7, sides);
                break;
            case "mcr_edge":
                if (slot > 11) {
                    return part;
                }
                slot = transformEdge(slot, sides);
                break;
            case "mcr_post":
                if (slot > 2) {
                    return part;
                }
                // Post axes follow the side numbering: 0 is Y, 1 is Z, 2 is X.
                slot = sides[slot << 1] >> 1;
                break;
            default:
                return part;
        }
        return part.createBuilder()
            .putByte("shape", (byte) (size << 4 | slot))
            .build();
    }

    /**
     * Axis bits as ForgeMultipart uses them: Y is 1, Z is 2 and X is 4, which is 1 shifted by the
     * axis of a side ({@code side >> 1}). A set bit means the positive end of that axis.
     */
    private static int transformCorner(int corner, int[] sides) {
        int moved = 0;
        for (int axis = 0; axis < 3; axis++) {
            int face = sides[axis << 1 | (corner >> axis & 1)];
            moved |= (face & 1) << (face >> 1);
        }
        return moved;
    }

    /**
     * The two sides each edge touches, in ForgeMultipart's edge order (PartMap EDGE_NYN to EDGE_XPP):
     * four along Y, four along Z, then four along X.
     */
    private static final int[][] EDGE_SIDES = { { 4, 2 }, { 4, 3 }, { 5, 2 }, { 5, 3 }, { 4, 0 }, { 5, 0 }, { 4, 1 },
        { 5, 1 }, { 0, 2 }, { 1, 2 }, { 0, 3 }, { 1, 3 } };

    private static int transformEdge(int edge, int[] sides) {
        int a = sides[EDGE_SIDES[edge][0]];
        int b = sides[EDGE_SIDES[edge][1]];
        for (int candidate = 0; candidate < EDGE_SIDES.length; candidate++) {
            int c = EDGE_SIDES[candidate][0];
            int d = EDGE_SIDES[candidate][1];
            if ((c == a && d == b) || (c == b && d == a)) {
                return candidate;
            }
        }
        return edge;
    }

    static int[] sideMap(AffineTransform transform) {
        return SideMaps.sideMap(transform);
    }

    private static boolean isIdentity(int[] sides) {
        return SideMaps.isIdentity(sides);
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
