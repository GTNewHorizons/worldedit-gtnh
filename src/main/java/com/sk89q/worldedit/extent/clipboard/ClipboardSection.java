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

// SPDX-License-Identifier: LGPL-3.0-or-later
package com.sk89q.worldedit.extent.clipboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.sk89q.jnbt.ByteArrayTag;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntArrayTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.BlockVector;

/** A section of ID/data pairs. Tile entities retain their own tags even when they share a state. */
final class ClipboardSection {

    private static final int MAX_PALETTE = 256;

    final BlockVector key;
    // A section holds at most 4096 slots, with at most 4095 transitions between them.
    private final short volume;
    private long uniform;
    private long[] palette;
    private byte[] indices;
    private long[] direct;
    private int paletteSize;
    private int lastIndex;
    private Map<Long, Integer> paletteLookup;
    private Map<Integer, CompoundTag> tags;

    // Number of adjacent slots with different states. Updating the two edges of a write gives an
    // exact O(1) test for uniformity, including when a section is overwritten out of order.
    private short transitions;

    ClipboardSection(BlockVector key, int volume) {
        this.key = key;
        this.volume = (short) volume;
    }

    boolean isEmpty() {
        return transitions == 0 && uniform == 0 && tags == null;
    }

    long stateAt(int index) {
        if (direct != null) {
            return direct[index];
        }
        if (indices != null) {
            return palette[indices[index] & 255];
        }
        return uniform;
    }

    @Nullable
    CompoundTag tagAt(int index) {
        CompoundTag tag = tags == null ? null : tags.get(index);
        return tag == null ? null : copyCompound(tag);
    }

    void set(int index, long state, @Nullable CompoundTag tag) {
        if (tag != null) {
            if (tags == null) {
                tags = new HashMap<>();
            }
            tags.put(index, copyCompound(tag));
        } else if (tags != null) {
            tags.remove(index);
            if (tags.isEmpty()) {
                tags = null;
            }
        }

        long old = stateAt(index);
        if (old == state) {
            return;
        }
        if (index > 0) {
            transitions += edgeChange(stateAt(index - 1), old, state);
        }
        if (index < volume - 1) {
            transitions += edgeChange(stateAt(index + 1), old, state);
        }
        if (transitions == 0) {
            uniform = state;
            indices = null;
            palette = null;
            paletteLookup = null;
            direct = null;
        } else if (direct != null) {
            direct[index] = state;
        } else {
            setPaletted(index, state);
        }
    }

    private static int edgeChange(long neighbor, long old, long state) {
        if (neighbor == old) {
            return 1;
        }
        return neighbor == state ? -1 : 0;
    }

    private void setPaletted(int index, long state) {
        if (indices == null) {
            indices = new byte[volume];
            palette = new long[4];
            palette[0] = uniform;
            paletteSize = 1;
            lastIndex = 0;
        }
        int next = paletteIndex(state);
        if (next < MAX_PALETTE) {
            indices[index] = (byte) next;
            return;
        }
        direct = new long[volume];
        for (int i = 0; i < volume; i++) {
            direct[i] = palette[indices[i] & 255];
        }
        direct[index] = state;
        indices = null;
        palette = null;
        paletteLookup = null;
    }

    private int paletteIndex(long state) {
        if (state == palette[lastIndex]) {
            return lastIndex;
        }
        if (paletteLookup == null) {
            for (int i = 0; i < paletteSize; i++) {
                if (palette[i] == state) {
                    lastIndex = i;
                    return i;
                }
            }
        } else {
            Integer existing = paletteLookup.get(state);
            if (existing != null) {
                lastIndex = existing;
                return existing;
            }
        }
        if (paletteSize == MAX_PALETTE) {
            return MAX_PALETTE;
        }
        if (paletteSize == palette.length) {
            palette = Arrays.copyOf(palette, paletteSize * 2);
        }
        if (paletteSize == 16) {
            paletteLookup = new HashMap<>();
            for (int i = 0; i < paletteSize; i++) {
                paletteLookup.put(palette[i], i);
            }
        }
        if (paletteLookup != null) {
            paletteLookup.put(state, paletteSize);
        }
        lastIndex = paletteSize++;
        palette[lastIndex] = state;
        return lastIndex;
    }

    private static CompoundTag copyCompound(CompoundTag tag) {
        Map<String, Tag> copy = new HashMap<>();
        for (Map.Entry<String, Tag> entry : tag.getValue()
            .entrySet()) {
            copy.put(entry.getKey(), copyTag(entry.getValue()));
        }
        return new CompoundTag(copy);
    }

    // JNBT containers wrap their caller's collections, and array tags expose mutable arrays.
    private static Tag copyTag(Tag tag) {
        if (tag instanceof CompoundTag) {
            return copyCompound((CompoundTag) tag);
        }
        if (tag instanceof ListTag) {
            ListTag list = (ListTag) tag;
            List<Tag> copy = new ArrayList<>(
                list.getValue()
                    .size());
            for (Tag item : list.getValue()) {
                copy.add(copyTag(item));
            }
            return new ListTag(list.getType(), copy);
        }
        if (tag instanceof ByteArrayTag) {
            return new ByteArrayTag(
                ((ByteArrayTag) tag).getValue()
                    .clone());
        }
        if (tag instanceof IntArrayTag) {
            return new IntArrayTag(
                ((IntArrayTag) tag).getValue()
                    .clone());
        }
        return tag;
    }
}
