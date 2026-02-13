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

import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.sk89q.worldedit.Vector;

/**
 * Utility methods for setting tile entities in the world.
 */
final class TileEntityUtils {

    private TileEntityUtils() {}

    /**
     * Update the given tag compound with position information.
     *
     * @param tag      the tag
     * @param position the position
     * @return a tag compound
     */
    private static NBTTagCompound updateForSet(NBTTagCompound tag, Vector position) {
        tag.setTag("x", new NBTTagInt(position.getBlockX()));
        tag.setTag("y", new NBTTagInt(position.getBlockY()));
        tag.setTag("z", new NBTTagInt(position.getBlockZ()));

        return tag;
    }

    /**
     * Set a tile entity at the given location using the tile entity ID from
     * the tag.
     *
     * @param world    the world
     * @param position the position
     * @param tag      the tag for the tile entity (may be null to do nothing)
     */
    static void setTileEntity(World world, Vector position, @Nullable NBTTagCompound tag) {
        if (tag != null) {
            updateForSet(tag, position);
            TileEntity tileEntity = makeTileEntity(world, position, tag);
            if (tileEntity != null) {
                setTileEntity(world, position, tileEntity);
            }
        }
    }

    private static TileEntity makeTileEntity(World world, Vector position, NBTTagCompound tag) {
        TileEntity normal = TileEntity.createAndLoadEntity(tag);
        return ForgeWorldEdit.inst.getFMPCompat()
            .overrideTileEntity(world, tag, normal);
    }

    private static void setTileEntity(World world, Vector position, TileEntity tileEntity) {
        world.setTileEntity(position.getBlockX(), position.getBlockY(), position.getBlockZ(), tileEntity);
        ForgeWorldEdit.inst.getFMPCompat()
            .sendDescPacket(world, tileEntity);
        ForgeWorldEdit.inst.getLtCompat()
            .sendDescPacket(world, tileEntity);
    }
}
