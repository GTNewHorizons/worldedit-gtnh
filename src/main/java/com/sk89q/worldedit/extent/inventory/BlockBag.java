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

package com.sk89q.worldedit.extent.inventory;

import com.sk89q.worldedit.WorldVector;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.BlockType;

/**
 * Represents a source to get blocks from and store removed ones.
 */
public abstract class BlockBag {

    /**
     * Stores a block as if it was mined.
     *
     * @param id the type ID
     * @throws BlockBagException on error
     * @deprecated Use {@link BlockBag#storeDroppedBlock(int, int)} instead
     */
    @Deprecated
    public void storeDroppedBlock(int id) throws BlockBagException {
        storeDroppedBlock(id, 0);
    }

    /**
     * Stores a block as if it was mined.
     *
     * @param id   the type ID
     * @param data the data value
     * @throws BlockBagException on error
     */
    public void storeDroppedBlock(int id, int data) throws BlockBagException {
        BaseItem dropped = BlockType.getBlockBagItem(id, data);
        if (dropped == null) return;
        if (dropped.getType() == BlockID.AIR) return;

        storeItem(dropped);
    }

    /**
     * Sets a block as if it was placed by hand.
     *
     * @param id the type ID
     * @throws BlockBagException on error
     * @deprecated Use {@link #fetchPlacedBlock(int,int)} instead
     */
    @Deprecated
    public void fetchPlacedBlock(int id) throws BlockBagException {
        fetchPlacedBlock(id, 0);
    }

    /**
     * Sets a block as if it was placed by hand.
     *
     * @param id   the type ID
     * @param data the data value
     * @throws BlockBagException on error
     */
    public void fetchPlacedBlock(int id, int data) throws BlockBagException {
        try {
            // Blocks that can't be fetched...
            switch (id) {
                case BlockID.BEDROCK:
                case BlockID.GOLD_ORE:
                case BlockID.IRON_ORE:
                case BlockID.COAL_ORE:
                case BlockID.DIAMOND_ORE:
                case BlockID.TNT:
                case BlockID.MOB_SPAWNER:
                case BlockID.CROPS:
                case BlockID.REDSTONE_ORE:
                case BlockID.GLOWING_REDSTONE_ORE:
                case BlockID.SNOW:
                case BlockID.LIGHTSTONE:
                case BlockID.PORTAL:
                    throw new UnplaceableBlockException();

                case BlockID.WATER:
                case BlockID.STATIONARY_WATER:
                case BlockID.LAVA:
                case BlockID.STATIONARY_LAVA:
                    // Override liquids
                    return;

                default:
                    fetchBlock(id);
                    break;
            }

        } catch (OutOfBlocksException e) {
            BaseItem placed = BlockType.getBlockBagItem(id, data);
            if (placed == null) throw e; // TODO: check
            if (placed.getType() == BlockID.AIR) throw e; // TODO: check

            fetchItem(placed);
        }
    }

    /**
     * Get a block.
     *
     * <p>
     * Either this method or fetchItem needs to be overridden.
     * </p>
     *
     * @param id the type ID
     * @throws BlockBagException on error
     */
    public void fetchBlock(int id) throws BlockBagException {
        fetchItem(new BaseItem(id));
    }

    /**
     * Get a block.
     *
     * <p>
     * Either this method or fetchItem needs to be overridden.
     * </p>
     *
     * @param item the item
     * @throws BlockBagException on error
     */
    public void fetchItem(BaseItem item) throws BlockBagException {
        fetchBlock(item.getType());
    }

    /**
     * Store a block.
     *
     * <p>
     * Either this method or fetchItem needs to be overridden.
     * </p>
     *
     * @param id the type ID
     * @throws BlockBagException on error
     */
    public void storeBlock(int id) throws BlockBagException {
        storeItem(new BaseItem(id));
    }

    /**
     * Store a block.
     *
     * <p>
     * Either this method or fetchItem needs to be overridden.
     * </p>
     *
     * @param item the item
     * @throws BlockBagException on error
     */
    public void storeItem(BaseItem item) throws BlockBagException {
        storeBlock(item.getType());
    }

    /**
     * Checks to see if a block exists without removing it.
     *
     * @param id the type ID
     * @return whether the block exists
     */
    public boolean peekBlock(int id) {
        try {
            fetchBlock(id);
            storeBlock(id);
            return true;
        } catch (BlockBagException e) {
            return false;
        }
    }

    /**
     * Flush any changes. This is called at the end.
     */
    public abstract void flushChanges();

    /**
     * Adds a position to be used a source.
     *
     * @param pos the position
     */
    public abstract void addSourcePosition(WorldVector pos);

    /**
     * Adds a position to be used a source.
     *
     * @param pos the position
     */
    public abstract void addSingleSourcePosition(WorldVector pos);
}
