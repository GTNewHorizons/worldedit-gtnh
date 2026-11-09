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
 * Answers the questions {@link BlockType} has hand-written vanilla tables for, for the blocks those
 * tables don't cover. {@link BlockType} asks once per ID and remembers the answers.
 */
public interface ModdedBlockProperties {

    /**
     * Whether players and entities can move through the block, as through plants, torches or liquids.
     *
     * @param id the block ID
     * @return true if it doesn't block movement
     */
    boolean canPassThrough(int id);

    /**
     * Whether the block is a liquid.
     *
     * @param id the block ID
     * @return true if it is a liquid
     */
    boolean isLiquid(int id);

    /**
     * Whether light or snow shouldn't treat the block as a full solid cube.
     *
     * @param id the block ID
     * @return true if it isn't a full opaque cube
     */
    boolean isTranslucent(int id);

    /**
     * Whether the block is natural ground, like stone, dirt or sand.
     *
     * @param id the block ID
     * @return true if it is natural terrain
     */
    boolean isNaturalTerrain(int id);
}
