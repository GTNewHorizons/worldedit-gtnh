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

package com.sk89q.worldedit.extent.cache;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;

/**
 * Returns the same cached {@link BaseBlock} for repeated calls to
 * {@link #getLazyBlock(Vector)} with the same position.
 */
public class LastAccessExtentCache extends AbstractDelegateExtent {

    private BaseBlock lastBlock;
    private int lastX;
    private int lastY;
    private int lastZ;

    /**
     * Create a new instance.
     *
     * @param extent the extent
     */
    public LastAccessExtentCache(Extent extent) {
        super(extent);
    }

    @Override
    public BaseBlock getLazyBlock(Vector position) {
        int x = position.getBlockX();
        int y = position.getBlockY();
        int z = position.getBlockZ();
        if (lastBlock != null && x == lastX && y == lastY && z == lastZ) {
            return lastBlock;
        }

        BaseBlock block = super.getLazyBlock(position);
        lastBlock = block;
        lastX = x;
        lastY = y;
        lastZ = z;
        return block;
    }

    @Override
    public boolean setBlock(Vector location, BaseBlock block) throws WorldEditException {
        // The cached block would no longer be what is there.
        if (lastBlock != null && location.getBlockX() == lastX
            && location.getBlockY() == lastY
            && location.getBlockZ() == lastZ) {
            lastBlock = null;
        }
        return super.setBlock(location, block);
    }
}
