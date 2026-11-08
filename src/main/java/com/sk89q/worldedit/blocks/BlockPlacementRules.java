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

package com.sk89q.worldedit.blocks;

/**
 * Decides which blocks outside the vanilla tables in {@link BlockType} have to be written after other
 * blocks in an edit, because a block written before the one it attaches to can fall off.
 */
public interface BlockPlacementRules {

    /**
     * Whether a block attaches to others. {@link BlockType} only asks once per ID and remembers the
     * answer.
     *
     * @param id the block ID
     * @return true if it should be written after ordinary blocks
     */
    boolean isAttached(int id);
}
