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

package com.sk89q.worldedit.regions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.visitor.RegionVisitor;

/** Builds a resumable scan of a selection for a block count. */
public final class RegionCount {

    private RegionCount() {}

    public static RegionVisitor create(Region region, RegionFunction matches) {
        List<Iterable<BlockVector>> scans = new ArrayList<>();
        snapshot(region, scans);
        // A null candidate is a budget checkpoint for a position or column outside the shape.
        // Returning it lets the visitor yield even if no selected block is nearby.
        return new RegionVisitor(Iterables.concat(scans), point -> point != null && matches.apply(point));
    }

    private static void snapshot(Region region, List<Iterable<BlockVector>> scans) {
        if (region.getClass() == RegionIntersection.class) {
            // Its iterator concatenates its children, including any repeated positions.
            for (Region child : ((RegionIntersection) region).children()) {
                snapshot(child, scans);
            }
            return;
        }

        Region copy = region.clone();
        if (copy.getClass() == CuboidRegion.class) {
            scans.add(((CuboidRegion) copy)::chunkOrderedIterator);
        } else if (copy.getClass() == EllipsoidRegion.class || copy.getClass() == ConvexPolyhedralRegion.class) {
            scans.add(
                () -> Iterators.transform(
                    CuboidRegion.chunkOrderedIterator(copy.getMinimumPoint(), copy.getMaximumPoint()),
                    point -> copy.contains(point) ? point : null));
        } else if (copy.getClass() == Polygonal2DRegion.class || copy.getClass() == CylinderRegion.class) {
            scans.add(() -> columns(copy));
        } else {
            // Third-party shapes can define membership, order and repeats through their iterator.
            scans.add(copy);
        }
    }

    private static Iterator<BlockVector> columns(Region region) {
        Vector min = region.getMinimumPoint();
        Vector max = region.getMaximumPoint();
        Iterator<BlockVector> floor = CuboidRegion.chunkOrderedIterator(min, max.setY(min.getY()));
        return Iterators.concat(Iterators.transform(floor, column -> {
            if (!region.contains(column)) {
                return Iterators.<BlockVector>singletonIterator(null);
            }
            return CuboidRegion.chunkOrderedIterator(column, column.setY(max.getY()));
        }));
    }
}
