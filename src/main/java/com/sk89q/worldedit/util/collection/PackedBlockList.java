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
 * Pages store X runs by their first position and uniform block columns by one palette index.
 * Irregular pages expand independently and retain constant-time indexed access for undo. Blocks without NBT
 * share one palette entry per ID and data value, so callers get back shared instances that must not
 * be modified. Blocks with NBT each keep their own entry.
 * </p>
 */
public final class PackedBlockList {

    private static final int PAGE_BITS = 8;
    private static final int PAGE_SIZE = 1 << PAGE_BITS;
    private static final int PAGE_MASK = PAGE_SIZE - 1;
    private static final long X_STEP = 1L << 38;

    private static final int XZ_LIMIT = 1 << 25;
    private static final int Y_LIMIT = 1 << 11;

    private final int columns;
    private final List<Page> pages = new ArrayList<Page>();
    private final List<BaseBlock> palette = new ArrayList<BaseBlock>();
    private final Map<Long, Integer> paletteIndex = new HashMap<Long, Integer>();
    private final long[] lastKeys = { -1, -1 };
    private final int[] lastIndices = new int[2];
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
        append(position, block, null);
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
        append(position, first, second);
    }

    private void append(Vector position, BaseBlock first, BaseBlock second) {
        checkArgument(fits(position), "position out of range: %s", position);

        int firstIndex = paletteIndexOf(first, 0);
        int secondIndex = paletteIndexOf(second, 1);
        int offset = size & PAGE_MASK;
        if (offset == 0) {
            pages.add(new Page(firstIndex, secondIndex));
        }
        pages.get(size >>> PAGE_BITS)
            .add(offset, pack(position), firstIndex, secondIndex, size < PAGE_SIZE);
        size++;
    }

    private int paletteIndexOf(BaseBlock block, int column) {
        if (block == null) {
            return -1;
        }
        if (block.getNbtData() != null) {
            palette.add(block);
            return palette.size() - 1;
        }

        long key = ((long) block.getId() << 32) | (block.getData() & 0xFFFFFFFFL);
        if (key == lastKeys[column]) {
            return lastIndices[column];
        }

        Integer index = paletteIndex.get(key);
        if (index == null) {
            index = palette.size();
            palette.add(new BaseBlock(block.getId(), block.getData()));
            paletteIndex.put(key, index);
        }
        lastKeys[column] = key;
        lastIndices[column] = index;
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
        long packed = pages.get(index >>> PAGE_BITS)
            .positionAt(index & PAGE_MASK);
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
        Page page = pages.get(index >>> PAGE_BITS);
        int offset = index & PAGE_MASK;
        int paletteIndex = column == 0 ? page.firstAt(offset) : page.secondAt(offset);
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
        if ((head & PAGE_MASK) == 0) {
            pages.set((head - 1) >>> PAGE_BITS, null);
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
        pages.clear();
        palette.clear();
        paletteIndex.clear();
        Arrays.fill(lastKeys, -1);
        size = 0;
        head = 0;
    }

    private static final class Page {

        private static final int RUNS_PER_PAGE = PAGE_SIZE / 16;

        // Each group of 16 consecutive X coordinates needs only its start. On the first
        // discontinuity inside a group, expand this page only. Packed addition also works
        // across X = 0 because X occupies the high bits.
        private long[] positions = new long[RUNS_PER_PAGE];
        private final int first;
        private final int second;
        private int[] firstValues;
        private int[] secondValues;

        Page(int first, int second) {
            this.first = first;
            this.second = second;
        }

        void add(int offset, long position, int firstIndex, int secondIndex, boolean firstPage) {
            if (positions.length != RUNS_PER_PAGE) {
                if (offset == positions.length) {
                    positions = Arrays.copyOf(positions, offset * 2);
                }
                positions[offset] = position;
            } else if ((offset & 15) == 0) {
                positions[offset >>> 4] = position;
            } else if (positionAt(offset) != position) {
                long[] expanded = new long[initialDenseCapacity(offset, firstPage)];
                for (int i = 0; i < offset; i++) {
                    expanded[i] = positionAt(i);
                }
                expanded[offset] = position;
                positions = expanded;
            }
            firstValues = setValue(firstValues, offset, first, firstIndex, firstPage);
            secondValues = setValue(secondValues, offset, second, secondIndex, firstPage);
        }

        // Most short histories never fill a page. Grow only their first page, leaving subsequent
        // dense pages at full capacity to avoid copying buffers on large edits. Dense position
        // arrays are always longer than the array of compressed run starts.
        private static int initialDenseCapacity(int offset, boolean firstPage) {
            return firstPage ? Math.max(2 * RUNS_PER_PAGE, Integer.highestOneBit(offset) * 2) : PAGE_SIZE;
        }

        private static int[] setValue(int[] values, int offset, int uniform, int value, boolean firstPage) {
            if (values == null) {
                if (uniform == value) {
                    return null;
                }
                values = new int[initialDenseCapacity(offset, firstPage)];
                Arrays.fill(values, 0, offset, uniform);
            } else if (offset == values.length) {
                values = Arrays.copyOf(values, offset * 2);
            }
            values[offset] = value;
            return values;
        }

        long positionAt(int offset) {
            return positions.length == RUNS_PER_PAGE ? positions[offset >>> 4] + (offset & 15) * X_STEP
                : positions[offset];
        }

        int firstAt(int offset) {
            return firstValues == null ? first : firstValues[offset];
        }

        int secondAt(int offset) {
            return secondValues == null ? second : secondValues[offset];
        }
    }
}
