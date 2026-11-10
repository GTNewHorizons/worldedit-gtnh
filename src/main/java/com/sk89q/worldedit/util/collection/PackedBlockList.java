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

package com.sk89q.worldedit.util.collection;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;

/**
 * An append-only list of block positions with one or two blocks each, stored in primitive arrays so
 * that large edits don't need several objects per block.
 *
 * <p>
 * Positions are packed into a long, and blocks into an int that indexes a palette. Blocks without NBT
 * share one palette entry per ID and data value, so callers get back shared instances that must not
 * be modified. Blocks with NBT each keep their own entry.
 * </p>
 */
public final class PackedBlockList {

    private static final int SEGMENT_BITS = 15;
    private static final int SEGMENT_SIZE = 1 << SEGMENT_BITS;
    private static final int SEGMENT_MASK = SEGMENT_SIZE - 1;
    private static final int INITIAL_CAPACITY = 64;

    private static final int XZ_LIMIT = 1 << 25;
    private static final int Y_LIMIT = 1 << 11;

    private final int columns;
    private final List<long[]> positions = new ArrayList<long[]>();
    private final List<int[]> blocks = new ArrayList<int[]>();
    private final List<BaseBlock> palette = new ArrayList<BaseBlock>();
    private final Map<Long, Integer> paletteIndex = new HashMap<Long, Integer>();
    private long lastKey = -1;
    private int lastIndex;
    private int size;
    private int head;

    /**
     * Create a new instance.
     *
     * @param columns how many blocks each position has, either 1 or 2
     */
    public PackedBlockList(int columns) {
        checkArgument(columns == 1 || columns == 2, "columns must be 1 or 2");
        this.columns = columns;
    }

    /**
     * Whether a position can be stored. This covers every coordinate a Minecraft world can hold.
     *
     * @param position the position
     * @return true if it can be stored
     */
    public static boolean fits(Vector position) {
        int x = position.getBlockX();
        int y = position.getBlockY();
        int z = position.getBlockZ();
        return x >= -XZ_LIMIT && x < XZ_LIMIT && z >= -XZ_LIMIT && z < XZ_LIMIT && y >= -Y_LIMIT && y < Y_LIMIT;
    }

    /**
     * Add a position with one block.
     *
     * @param position the position, which must {@link #fits(Vector) fit}
     * @param block    the block
     */
    public void add(Vector position, BaseBlock block) {
        checkArgument(columns == 1, "this list has two blocks per position");
        int index = append(position);
        blocks.get(index >>> SEGMENT_BITS)[index & SEGMENT_MASK] = paletteIndexOf(block);
    }

    /**
     * Add a position with two blocks.
     *
     * @param position the position, which must {@link #fits(Vector) fit}
     * @param first    the first block
     * @param second   the second block
     */
    public void add(Vector position, BaseBlock first, BaseBlock second) {
        checkArgument(columns == 2, "this list has one block per position");
        int index = append(position);
        int[] segment = blocks.get(index >>> SEGMENT_BITS);
        int offset = (index & SEGMENT_MASK) * 2;
        segment[offset] = paletteIndexOf(first);
        segment[offset + 1] = paletteIndexOf(second);
    }

    private int append(Vector position) {
        checkArgument(fits(position), "position out of range: %s", position);

        int segment = size >>> SEGMENT_BITS;
        int offset = size & SEGMENT_MASK;

        if (segment == positions.size()) {
            // Only the first segment starts small, so that small edits stay small.
            int capacity = segment == 0 ? INITIAL_CAPACITY : SEGMENT_SIZE;
            positions.add(new long[capacity]);
            blocks.add(new int[capacity * columns]);
        }

        long[] segmentPositions = positions.get(segment);
        if (offset == segmentPositions.length) {
            int capacity = Math.min(segmentPositions.length * 2, SEGMENT_SIZE);
            segmentPositions = Arrays.copyOf(segmentPositions, capacity);
            positions.set(segment, segmentPositions);
            blocks.set(segment, Arrays.copyOf(blocks.get(segment), capacity * columns));
        }

        segmentPositions[offset] = pack(position);
        return size++;
    }

    private int paletteIndexOf(BaseBlock block) {
        if (block == null) {
            return -1;
        }
        if (block.getNbtData() != null) {
            palette.add(block);
            return palette.size() - 1;
        }

        long key = ((long) block.getId() << 16) | (block.getData() & 0xFFFF);
        if (key == lastKey) {
            return lastIndex;
        }

        Integer index = paletteIndex.get(key);
        if (index == null) {
            index = palette.size();
            palette.add(new BaseBlock(block.getId(), block.getData()));
            paletteIndex.put(key, index);
        }
        lastKey = key;
        lastIndex = index;
        return index;
    }

    private static long pack(Vector position) {
        return ((long) position.getBlockX() << 38) | ((position.getBlockZ() & 0x3FFFFFFL) << 12)
            | (position.getBlockY() & 0xFFFL);
    }

    /**
     * Get how many positions have been added, including any that have been drained.
     *
     * @return the size
     */
    public int size() {
        return size;
    }

    /**
     * Get the position at an index.
     *
     * @param index the index
     * @return the position
     */
    public BlockVector positionAt(int index) {
        long packed = positions.get(index >>> SEGMENT_BITS)[index & SEGMENT_MASK];
        int x = (int) (packed >> 38);
        int z = (int) (packed << 26 >> 38);
        int y = (int) (packed << 52 >> 52);
        return new BlockVector(x, y, z);
    }

    /**
     * Get a block at an index.
     *
     * @param index  the index
     * @param column which of the position's blocks to get, starting at 0
     * @return the block, which must not be modified, or null if null was added
     */
    public BaseBlock blockAt(int index, int column) {
        int paletteIndex = blocks.get(index >>> SEGMENT_BITS)[(index & SEGMENT_MASK) * columns + column];
        return paletteIndex < 0 ? null : palette.get(paletteIndex);
    }

    /**
     * Get an iterator that frees storage behind it as it goes. It continues from wherever the last one
     * stopped, so a drain that gets interrupted never returns the same position twice.
     *
     * @return an iterator over the positions and their first blocks
     */
    public Iterator<Map.Entry<BlockVector, BaseBlock>> drain() {
        return new Iterator<Map.Entry<BlockVector, BaseBlock>>() {

            @Override
            public boolean hasNext() {
                return head < size;
            }

            @Override
            public Map.Entry<BlockVector, BaseBlock> next() {
                if (head >= size) {
                    throw new NoSuchElementException();
                }

                Map.Entry<BlockVector, BaseBlock> entry = new AbstractMap.SimpleImmutableEntry<BlockVector, BaseBlock>(
                    positionAt(head),
                    blockAt(head, 0));
                advance();
                return entry;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Like {@link #drain()}, but with both blocks of a two-column list.
     *
     * @return an iterator over the positions and their blocks
     */
    public Iterator<Entry> drainEntries() {
        checkArgument(columns == 2, "this list has one block per position");
        return new Iterator<Entry>() {

            @Override
            public boolean hasNext() {
                return head < size;
            }

            @Override
            public Entry next() {
                if (head >= size) {
                    throw new NoSuchElementException();
                }

                Entry entry = new Entry(positionAt(head), blockAt(head, 0), blockAt(head, 1));
                advance();
                return entry;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    private void advance() {
        head++;
        if ((head & SEGMENT_MASK) == 0) {
            int finished = (head - 1) >>> SEGMENT_BITS;
            positions.set(finished, null);
            blocks.set(finished, null);
        }
    }

    /**
     * A position and its two blocks.
     */
    public static final class Entry {

        public final BlockVector position;
        public final BaseBlock first;
        public final BaseBlock second;

        Entry(BlockVector position, BaseBlock first, BaseBlock second) {
            this.position = position;
            this.first = first;
            this.second = second;
        }
    }

    /**
     * Get how many positions have been added but not drained yet.
     *
     * @return the number left
     */
    public int remaining() {
        return size - head;
    }

    /**
     * Whether anything added has not been drained yet.
     *
     * @return true if there is something left
     */
    public boolean hasRemaining() {
        return head < size;
    }

    /**
     * Remove everything.
     */
    public void clear() {
        positions.clear();
        blocks.clear();
        palette.clear();
        paletteIndex.clear();
        lastKey = -1;
        size = 0;
        head = 0;
    }
}
