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

package com.sk89q.worldedit.function;

import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.regions.FlatRegion;

/**
 * Performs a function on the columns in a {@link FlatRegion}, or also
 * known as vectors with only X and Z components (where Y is height).
 */
public interface FlatRegionFunction {

    /**
     * Apply the function to the given position.
     *
     * @param position the position
     * @return true if something was changed
     * @throws WorldEditException thrown on an error
     */
    public boolean apply(Vector2D position) throws WorldEditException;

}
