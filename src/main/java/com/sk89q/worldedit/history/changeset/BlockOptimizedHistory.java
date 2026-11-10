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

package com.sk89q.worldedit.history.changeset;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.google.common.collect.Iterators;
import com.sk89q.worldedit.history.change.BlockChange;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.util.collection.PackedBlockList;

/**
 * An extension of {@link ArrayListHistory} that stores {@link BlockChange}s in a
 * {@link PackedBlockList} instead of keeping an object for each one, and rebuilds them when iterated.
 *
 * <p>
 * Each iterator runs through other changes first and then block changes, and block changes are
 * returned in the order they were added, or the reverse when going backward.
 * </p>
 */
public class BlockOptimizedHistory extends ArrayListHistory {

    private final PackedBlockList blocks = new PackedBlockList(2);

    @Override
    public void add(Change change) {
        checkNotNull(change);

        if (change instanceof BlockChange && PackedBlockList.fits(((BlockChange) change).getPosition())) {
            BlockChange blockChange = (BlockChange) change;
            blocks.add(blockChange.getPosition(), blockChange.getPrevious(), blockChange.getCurrent());
        } else {
            super.add(change);
        }
    }

    @Override
    public Iterator<Change> forwardIterator() {
        return Iterators.concat(super.forwardIterator(), blockChanges(0, 1));
    }

    @Override
    public Iterator<Change> backwardIterator() {
        return Iterators.concat(super.backwardIterator(), blockChanges(blocks.size() - 1, -1));
    }

    @Override
    public int size() {
        return super.size() + blocks.size();
    }

    private Iterator<Change> blockChanges(final int start, final int step) {
        return new Iterator<Change>() {

            private int index = start;

            @Override
            public boolean hasNext() {
                return index >= 0 && index < blocks.size();
            }

            @Override
            public Change next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                Change change = new BlockChange(
                    blocks.positionAt(index),
                    blocks.blockAt(index, 0),
                    blocks.blockAt(index, 1));
                index += step;
                return change;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
