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

package com.sk89q.worldedit.function.mask;

import javax.annotation.Nullable;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.extent.Extent;

public class SolidBlockMask extends AbstractExtentMask {

    public SolidBlockMask(Extent extent) {
        super(extent);
    }

    @Override
    public boolean test(Vector vector) {
        Extent extent = getExtent();
        BaseBlock lazyBlock = extent.getLazyBlock(vector);
        return !BlockType.canPassThrough(lazyBlock.getType(), lazyBlock.getData());
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        return null;
    }

}
