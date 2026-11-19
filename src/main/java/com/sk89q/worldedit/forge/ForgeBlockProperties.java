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
import net.minecraft.block.material.Material;
import net.minecraftforge.fluids.IFluidBlock;

import com.sk89q.worldedit.blocks.ModdedBlockProperties;

/**
 * Tells modded blocks apart by their material, which every block declares.
 */
final class ForgeBlockProperties implements ModdedBlockProperties {

    private final Set<String> naturalTerrain;
    private final Set<String> notNaturalTerrain;

    ForgeBlockProperties(Set<String> naturalTerrain, Set<String> notNaturalTerrain) {
        this.naturalTerrain = checkNotNull(naturalTerrain);
        this.notNaturalTerrain = checkNotNull(notNaturalTerrain);
    }

    @Override
    public boolean canPassThrough(int id) {
        return !Block.getBlockById(id)
            .getMaterial()
            .blocksMovement();
    }

    @Override
    public boolean isLiquid(int id) {
        Block block = Block.getBlockById(id);
        return block.getMaterial()
            .isLiquid() || block instanceof IFluidBlock;
    }

    @Override
    public boolean isTranslucent(int id) {
        return !Block.getBlockById(id)
            .isOpaqueCube();
    }

    @Override
    public String getName(int id) {
        return Block.blockRegistry.getNameForObject(Block.getBlockById(id));
    }

    @Override
    public boolean isNaturalTerrain(int id) {
        Block block = Block.getBlockById(id);
        String name = Block.blockRegistry.getNameForObject(block);
        if (naturalTerrain.contains(name)) {
            return true;
        }
        if (notNaturalTerrain.contains(name)) {
            return false;
        }

        // Modded stone, dirt, sand and ores. Rock also covers some building blocks, which is the price of
        // catching every mod's stone types without a list.
        Material material = block.getMaterial();
        boolean ground = material == Material.rock || material == Material.ground
            || material == Material.grass
            || material == Material.sand
            || material == Material.clay
            || material == Material.packedIce;
        return ground && block.isOpaqueCube();
    }
}
