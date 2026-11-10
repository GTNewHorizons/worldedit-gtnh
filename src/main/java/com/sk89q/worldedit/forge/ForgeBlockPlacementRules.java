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

package com.sk89q.worldedit.forge;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBasePressurePlate;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.BlockSign;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.material.Material;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockPlacementRules;

/**
 * Works out placement from what Forge knows about a block, the first time an edit touches it.
 */
final class ForgeBlockPlacementRules implements BlockPlacementRules {

    private static final String MULTIPART_BLOCK = "ForgeMultipart:block";
    private static final int UNKNOWN = -2;
    private static final int ABSENT = -1;

    private int multipartId = UNKNOWN;

    private final Set<String> placeLast;
    private final Set<String> placeNormally;

    ForgeBlockPlacementRules(Set<String> placeLast, Set<String> placeNormally) {
        this.placeLast = checkNotNull(placeLast);
        this.placeNormally = checkNotNull(placeNormally);
    }

    @Override
    public boolean isAttached(int id) {
        Block block = Block.getBlockById(id);
        if (block == null) {
            return false;
        }

        String name = Block.blockRegistry.getNameForObject(block);
        if (placeLast.contains(name)) {
            return true;
        }
        if (placeNormally.contains(name)) {
            return false;
        }

        return isAttached(block);
    }

    @Override
    public boolean dependsOnContents(int id) {
        if (multipartId == UNKNOWN) {
            Block multipart = Block.getBlockFromName(MULTIPART_BLOCK);
            multipartId = multipart != null ? Block.getIdFromBlock(multipart) : ABSENT;
        }
        return id == multipartId;
    }

    /**
     * A multipart holding only microblocks is a solid block, and is what ProjectRed wires and gates
     * usually sit on. Any other part, such as a wire, gate or torch, drops itself if a neighbour changes
     * while the block it sits on is missing, so that block goes in with the attached ones: after its
     * supports, and removed before them.
     */
    @Override
    public boolean isAttached(BaseBlock block) {
        CompoundTag nbt = block.getNbtData();
        if (nbt == null) {
            return false;
        }
        for (CompoundTag part : nbt.getList("parts", CompoundTag.class)) {
            if (!part.getString("id")
                .startsWith("mcr_")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAttached(Block block) {
        Material material = block.getMaterial();
        if (!material.isSolid() && !material.isLiquid() && material != Material.air) {
            return true;
        }

        return block instanceof BlockTorch || block instanceof BlockBush
            || block instanceof BlockLever
            || block instanceof BlockButton
            || block instanceof BlockRailBase
            || block instanceof BlockSign
            || block instanceof BlockDoor
            || block instanceof BlockTrapDoor
            || block instanceof BlockLadder
            || block instanceof BlockRedstoneWire
            || block instanceof BlockRedstoneDiode
            || block instanceof BlockBasePressurePlate;
    }
}
