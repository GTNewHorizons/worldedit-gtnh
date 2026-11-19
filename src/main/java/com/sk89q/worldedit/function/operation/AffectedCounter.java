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

package com.sk89q.worldedit.function.operation;

/**
 * Keeps a count of how much an operation affected.
 *
 * <p>
 * The count does not always live on the operation itself: for an overlay, for example, the
 * operation is a layer visitor but the count is kept by the ground function it drives.
 * {@link CountedOperation} pairs the two so a caller only has to hold one object.
 * </p>
 */
public interface AffectedCounter {

    /**
     * Get the number of affected objects.
     *
     * @return the number affected so far
     */
    int getAffected();
}
