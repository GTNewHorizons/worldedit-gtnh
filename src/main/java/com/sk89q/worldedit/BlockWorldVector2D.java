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

/**
 * @deprecated Replace all uses of {@link WorldVector}s with {@link Location}s
 */
@SuppressWarnings("deprecation")
@Deprecated
public class BlockWorldVector2D extends WorldVector2D {

    /**
     * Construct a new instance.
     *
     * @param world the world
     * @param x     the X coordinate
     * @param z     the Z coordinate
     */
    public BlockWorldVector2D(LocalWorld world, double x, double z) {
        super(world, x, z);
    }

    /**
     * Construct a new instance.
     *
     * @param world the world
     * @param x     the X coordinate
     * @param z     the Z coordinate
     */
    public BlockWorldVector2D(LocalWorld world, float x, float z) {
        super(world, x, z);
    }

    /**
     * Construct a new instance.
     *
     * @param world the world
     * @param x     the X coordinate
     * @param z     the Z coordinate
     */
    public BlockWorldVector2D(LocalWorld world, int x, int z) {
        super(world, x, z);
    }

    /**
     * Construct a new instance.
     *
     * @param world    the world
     * @param position a position
     */
    public BlockWorldVector2D(LocalWorld world, Vector2D position) {
        super(world, position);
    }

    /**
     * Construct a new instance with X and Z set to (0, 0).
     *
     * @param world the world
     */
    public BlockWorldVector2D(LocalWorld world) {
        super(world);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof WorldVector2D)) {
            return false;
        }
        WorldVector2D other = (WorldVector2D) obj;
        return other.getWorld()
            .equals(world) && (int) other.getX() == (int) this.x
            && (int) other.getZ() == (int) this.z;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        long temp;
        result = 31 * result + world.hashCode();
        temp = Double.doubleToLongBits(x);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(z);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

}
