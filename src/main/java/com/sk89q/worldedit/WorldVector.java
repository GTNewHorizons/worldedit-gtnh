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

package com.sk89q.worldedit;

import com.sk89q.worldedit.internal.LocalWorldAdapter;
import com.sk89q.worldedit.world.World;

/**
 * @deprecated Use {@link com.sk89q.worldedit.util.Location} wherever possible
 */
@SuppressWarnings("deprecation")
@Deprecated
public class WorldVector extends Vector {

    private LocalWorld world;

    /**
     * Construct the Vector object.
     *
     * @param world a world
     * @param x     the X coordinate
     * @param y     the Y coordinate
     * @param z     the Z coordinate
     */
    public WorldVector(LocalWorld world, double x, double y, double z) {
        super(x, y, z);
        this.world = world;
    }

    /**
     * Construct the Vector object.
     *
     * @param world a world
     * @param x     the X coordinate
     * @param y     the Y coordinate
     * @param z     the Z coordinate
     */
    public WorldVector(LocalWorld world, int x, int y, int z) {
        super(x, y, z);
        this.world = world;
    }

    /**
     * Construct the Vector object.
     *
     * @param world a world
     * @param x     the X coordinate
     * @param y     the Y coordinate
     * @param z     the Z coordinate
     */
    public WorldVector(LocalWorld world, float x, float y, float z) {
        super(x, y, z);
        this.world = world;
    }

    /**
     * Construct the Vector object.
     *
     * @param world a world
     * @param other the position to copy
     */
    public WorldVector(LocalWorld world, Vector other) {
        super(other);
        this.world = world;
    }

    /**
     * Construct the Vector object.
     *
     * @param world a world
     */
    public WorldVector(LocalWorld world) {
        super();
        this.world = world;
    }

    /**
     * Construct the Vector object.
     *
     * @param location the location
     */
    public WorldVector(com.sk89q.worldedit.util.Location location) {
        this(LocalWorldAdapter.adapt((World) location.getExtent()), location.getX(), location.getY(), location.getZ());
    }

    /**
     * Get the world.
     * 
     * @return the world
     */
    public LocalWorld getWorld() {
        return world;
    }

    /**
     * Get a block point from a point.
     *
     * @param world a world
     * @param x     the X coordinate
     * @param y     the Y coordinate
     * @param z     the Z coordinate
     * @return point
     */
    public static WorldVector toBlockPoint(LocalWorld world, double x, double y, double z) {
        return new WorldVector(world, (int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    /**
     * Gets a BlockVector version.
     * 
     * @return BlockWorldVector
     */
    public BlockWorldVector toWorldBlockVector() {
        return new BlockWorldVector(this);
    }

    /**
     * Return this object as a new preferred {@code Location}
     * object.
     *
     * @return a new location object
     */
    public com.sk89q.worldedit.util.Location toLocation() {
        return new com.sk89q.worldedit.util.Location(getWorld(), this);
    }

}
