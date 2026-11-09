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

package com.sk89q.worldedit.extent.world;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.World;

/**
 * Handles various quirks when setting blocks, such as ice turning
 * into water or containers dropping their contents.
 */
public class BlockQuirkExtent extends AbstractDelegateExtent {

    private final World world;

    /**
     * Create a new instance.
     *
     * @param extent the extent
     * @param world  the world
     */
    public BlockQuirkExtent(Extent extent, World world) {
        super(extent);
        checkNotNull(world);
        this.world = world;
    }

    @Override
    public boolean setBlock(Vector position, BaseBlock block) throws WorldEditException {
        // Read straight from the world, which is what the extents below this one do too, without
        // creating a block to hold the result.
        int existing = world.getBlockType(position);

        if (existing == BlockID.ICE) {
            world.setBlock(position, new BaseBlock(BlockID.AIR)); // Ice turns until water so this has to be done first
        } else if (existing != BlockID.AIR
            && !(existing == block.getType() && world.getBlockData(position) == block.getData())) {
                // Otherwise the old block drops its inventory, on top of the copy kept for undo.
                // A write of the same block is ignored, so emptying it would only lose the contents.
                world.clearContainerBlockContents(position);
            }

        return super.setBlock(position, block);
    }

}
