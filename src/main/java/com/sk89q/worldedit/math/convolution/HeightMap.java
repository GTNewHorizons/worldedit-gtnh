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

package com.sk89q.worldedit.math.convolution;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.function.operation.AffectedCounter;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.operation.RunContext;
import com.sk89q.worldedit.regions.Region;

/**
 * Allows applications of Kernels onto the region's height map.
 *
 * <p>
 * Currently only used for smoothing (with a GaussianKernel)
 * </p>
 * .
 */
public class HeightMap {

    private int[] data;
    private int width;
    private int height;

    private Region region;
    private EditSession session;

    /**
     * Constructs the HeightMap
     *
     * @param session an edit session
     * @param region  the region
     */
    public HeightMap(EditSession session, Region region) {
        this(session, region, false);
    }

    /**
     * Constructs the HeightMap
     *
     * @param session     an edit session
     * @param region      the region
     * @param naturalOnly ignore non-natural blocks
     */
    public HeightMap(EditSession session, Region region, boolean naturalOnly) {
        checkNotNull(session);
        checkNotNull(region);

        this.session = session;
        this.region = region;

        this.width = region.getWidth();
        this.height = region.getLength();

        int minX = region.getMinimumPoint()
            .getBlockX();
        int minY = region.getMinimumPoint()
            .getBlockY();
        int minZ = region.getMinimumPoint()
            .getBlockZ();
        int maxY = region.getMaximumPoint()
            .getBlockY();

        // Store current heightmap data
        data = new int[width * height];
        for (int z = 0; z < height; ++z) {
            for (int x = 0; x < width; ++x) {
                data[z * width + x] = session.getHighestTerrainBlock(x + minX, z + minZ, minY, maxY, naturalOnly);
            }
        }
    }

    /**
     * Apply the filter 'iterations' amount times.
     *
     * @param filter     the filter
     * @param iterations the number of iterations
     * @return number of blocks affected
     * @throws MaxChangedBlocksException
     */

    public int applyFilter(HeightMapFilter filter, int iterations) throws MaxChangedBlocksException {
        Applier applier = applyFilterOperation(filter, iterations);
        Operations.completeLegacy(applier);
        return applier.getAffected();
    }

    /**
     * Build the operation that {@link #applyFilter(HeightMapFilter, int)} runs, without running it,
     * so a caller can hand it to an
     * {@link com.sk89q.worldedit.util.operation.OperationScheduler OperationScheduler} instead.
     *
     * @param filter     the filter
     * @param iterations the number of iterations to run the filter for
     * @return the operation, which also carries the affected count once it has run
     */
    public Applier applyFilterOperation(HeightMapFilter filter, int iterations) {
        checkNotNull(filter);

        int[] newData = new int[data.length];
        System.arraycopy(data, 0, newData, 0, data.length);

        for (int i = 0; i < iterations; ++i) {
            newData = filter.filter(newData, width, height);
        }

        return new Applier(newData);
    }

    /**
     * Apply a raw heightmap to the region
     *
     * @param data the data
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int apply(int[] data) throws MaxChangedBlocksException {
        Applier applier = applyOperation(data);
        Operations.completeLegacy(applier);
        return applier.getAffected();
    }

    /**
     * Build the operation that {@link #apply(int[])} runs, without running it.
     *
     * @param data the data
     * @return the operation, which also carries the affected count once it has run
     */
    public Applier applyOperation(int[] data) {
        checkNotNull(data);

        return new Applier(data);
    }

    /**
     * Writes a heightmap to the world a column at a time, so a large smooth can be spread over
     * several server ticks instead of running in one go.
     */
    public class Applier implements Operation, AffectedCounter {

        private final int[] target;
        private final int originX;
        private final int originY;
        private final int originZ;
        private final int maxY;
        private final BaseBlock fillerAir = new BaseBlock(BlockID.AIR);

        private int x;
        private int z;
        private int blocksChanged;

        private Applier(int[] target) {
            this.target = target;

            Vector min = region.getMinimumPoint();
            this.originX = min.getBlockX();
            this.originY = min.getBlockY();
            this.originZ = min.getBlockZ();
            this.maxY = region.getMaximumPoint()
                .getBlockY();
        }

        @Override
        public int getAffected() {
            return blocksChanged;
        }

        @Override
        public Operation resume(RunContext run) throws WorldEditException {
            while (z < height) {
                if (x >= width) {
                    x = 0;
                    z++;
                    continue;
                }

                applyColumn(x, z);
                x++;

                if (!run.shouldContinue()) {
                    return this;
                }
            }

            // Drop trees to the floor -- TODO

            return null;
        }

        @Override
        public void cancel() {}

        @Override
        public void addStatusMessages(List<String> messages) {
            messages.add(getAffected() + " blocks affected");
        }

        private void applyColumn(int x, int z) throws WorldEditException {
            int index = z * width + x;
            int curHeight = HeightMap.this.data[index];

            // Clamp newHeight within the selection area
            int newHeight = Math.min(maxY, target[index]);

            // Offset x,z to be 'real' coordinates
            int xr = x + originX;
            int zr = z + originZ;

            // We are keeping the topmost blocks so take that in account for the scale
            double scale = (double) (curHeight - originY) / (double) (newHeight - originY);

            // Depending on growing or shrinking we need to start at the bottom or top
            if (newHeight > curHeight) {
                // Set the top block of the column to be the same type (this might go wrong with rounding)
                BaseBlock existing = session.getBlock(new Vector(xr, curHeight, zr));

                // Skip water/lava
                if (existing.getType() != BlockID.WATER && existing.getType() != BlockID.STATIONARY_WATER
                    && existing.getType() != BlockID.LAVA
                    && existing.getType() != BlockID.STATIONARY_LAVA) {
                    session.setBlock(new Vector(xr, newHeight, zr), existing);
                    ++blocksChanged;

                    // Grow -- start from 1 below top replacing airblocks
                    for (int y = newHeight - 1 - originY; y >= 0; --y) {
                        int copyFrom = (int) (y * scale);
                        session.setBlock(
                            new Vector(xr, originY + y, zr),
                            session.getBlock(new Vector(xr, originY + copyFrom, zr)));
                        ++blocksChanged;
                    }
                }
            } else if (curHeight > newHeight) {
                // Shrink -- start from bottom
                for (int y = 0; y < newHeight - originY; ++y) {
                    int copyFrom = (int) (y * scale);
                    session.setBlock(
                        new Vector(xr, originY + y, zr),
                        session.getBlock(new Vector(xr, originY + copyFrom, zr)));
                    ++blocksChanged;
                }

                // Set the top block of the column to be the same type
                // (this could otherwise go wrong with rounding)
                session.setBlock(new Vector(xr, newHeight, zr), session.getBlock(new Vector(xr, curHeight, zr)));
                ++blocksChanged;

                // Fill rest with air
                for (int y = newHeight + 1; y <= curHeight; ++y) {
                    session.setBlock(new Vector(xr, y, zr), fillerAir);
                    ++blocksChanged;
                }
            }
        }
    }

}
