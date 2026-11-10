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

package com.sk89q.worldedit.extent.reorder;

import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.Iterators;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.PlayerDirection;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.OperationQueue;
import com.sk89q.worldedit.function.operation.RunContext;
import com.sk89q.worldedit.history.change.BlockChange;
import com.sk89q.worldedit.history.changeset.ChangeSet;
import com.sk89q.worldedit.util.collection.PackedBlockList;

/**
 * Re-orders blocks into several stages.
 */
public class MultiStageReorder extends AbstractDelegateExtent implements ReorderingExtent {

    private static final BaseBlock AIR = new BaseBlock(BlockID.AIR);

    // Each entry holds the block to write and the block that was there when it was queued, which goes
    // into history once the write happens. Null for writes that aren't recorded.
    private final PackedBlockList detach = new PackedBlockList(2);
    private final PackedBlockList stage1 = new PackedBlockList(2);
    private final PackedBlockList stage2 = new PackedBlockList(2);
    private final PackedBlockList stage3 = new PackedBlockList(2);
    private boolean enabled;
    @Nullable
    private ChangeSet history;
    // Blocks cleared ahead of their real write, with what was there. Recorded only if the edit stops
    // before the real write, which records them otherwise.
    private final Map<BlockVector, BaseBlock> detached = new LinkedHashMap<BlockVector, BaseBlock>();

    /**
     * Create a new instance.
     *
     * @param extent  the extent
     * @param enabled true to enable
     */
    public MultiStageReorder(Extent extent, boolean enabled) {
        super(extent);
        this.enabled = enabled;
    }

    /**
     * Create a new instance when the re-ordering is enabled.
     *
     * @param extent the extent
     */
    public MultiStageReorder(Extent extent) {
        this(extent, true);
    }

    /**
     * Return whether re-ordering is enabled.
     *
     * @return true if re-ordering is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Set whether re-ordering is enabled.
     *
     * @param enabled true if re-ordering is enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Set where queued writes are recorded once they happen, for those queued with
     * {@link #setBlock(Vector, BaseBlock, BaseBlock)}.
     *
     * @param history the change set, or null to record nothing
     */
    public void setHistory(@Nullable ChangeSet history) {
        this.history = history;
    }

    /**
     * Whether a write to the given position would be queued rather than written straight away.
     *
     * @param location the position
     * @return true if it would be queued
     */
    public boolean queues(Vector location) {
        return enabled && PackedBlockList.fits(location);
    }

    @Override
    public boolean setBlock(Vector location, BaseBlock block) throws WorldEditException {
        return setBlock(location, block, null);
    }

    /**
     * Queue a block together with what history should record as being there before. The change is
     * only recorded once the block is actually written, so an edit cancelled partway leaves no history
     * for the blocks it never wrote.
     *
     * @param location the position
     * @param block    the block to write
     * @param previous the block that was there when this was queued, or null to record nothing
     * @return whether the block changes
     * @throws WorldEditException if writing straight away fails
     */
    public boolean setBlock(Vector location, BaseBlock block, @Nullable BaseBlock previous) throws WorldEditException {
        if (!queues(location)) {
            return super.setBlock(location, block);
        }

        BaseBlock lazyBlock = getLazyBlock(location);
        boolean changed = !(lazyBlock.getType() == block.getType() && lazyBlock.getData() == block.getData());
        boolean replacingAttached = changed
            && (BlockType.shouldPlaceLast(lazyBlock.getType()) || BlockType.shouldPlaceFinal(lazyBlock.getType())
                || BlockType.isAttachedByContents(lazyBlock));

        // Destroy torches, etc. first, whatever replaces them. They would fall off if what they're
        // attached to went away while they were still waiting for their turn. This waits for the
        // commit, because removing a block straight away can break others, such as the top half of a
        // door, before the edit has read and recorded them.
        if (replacingAttached) {
            // Recorded too: if the edit stops before the real write, undo still has to bring it back.
            detach.add(location, AIR, previous);
        }

        if (BlockType.shouldPlaceLast(block.getType()) || BlockType.isAttachedByContents(block)) {
            // Place torches, etc. last
            stage2.add(location, block, previous);
            return changed;
        } else if (BlockType.shouldPlaceFinal(block.getType())) {
            // Place signs, reed, etc even later
            stage3.add(location, block, previous);
            return changed;
        } else {
            stage1.add(location, block, previous);
            return changed;
        }
    }

    /**
     * Whether any blocks are still waiting to be written.
     *
     * @return true if committing would write something
     */
    public boolean hasPendingBlocks() {
        return detach.hasRemaining() || stage1.hasRemaining() || stage2.hasRemaining() || stage3.hasRemaining();
    }

    /**
     * Get how many blocks are waiting to be written.
     *
     * @return the number of blocks
     */
    public int pendingBlockCount() {
        return detach.remaining() + stage1.remaining() + stage2.remaining() + stage3.remaining();
    }

    /**
     * Throw away everything waiting to be written.
     */
    public void discard() {
        if (history != null) {
            for (Map.Entry<BlockVector, BaseBlock> entry : detached.entrySet()) {
                history.add(new BlockChange(entry.getKey(), entry.getValue(), AIR));
            }
        }
        detached.clear();
        detach.clear();
        stage1.clear();
        stage2.clear();
        stage3.clear();
    }

    @Override
    public Operation commitBefore() {
        return new OperationQueue(
            new EntryPlacer(detach.drainEntries(), true),
            new EntryPlacer(Iterators.concat(stage1.drainEntries(), stage2.drainEntries()), false),
            new Stage3Committer());
    }

    private void write(BlockVector position, BaseBlock block, @Nullable BaseBlock previous) throws WorldEditException {
        getExtent().setBlock(position, block);
        detached.remove(position);
        if (previous != null && history != null) {
            history.add(new BlockChange(position, previous, block));
        }
    }

    private void detach(BlockVector position, BaseBlock block, @Nullable BaseBlock previous) throws WorldEditException {
        getExtent().setBlock(position, block);
        if (previous != null && history != null && !detached.containsKey(position)) {
            detached.put(position, previous);
        }
    }

    /**
     * Writes queued entries in order and records each one.
     */
    private class EntryPlacer implements Operation {

        private final Iterator<PackedBlockList.Entry> entries;
        private final boolean detaching;

        private EntryPlacer(Iterator<PackedBlockList.Entry> entries, boolean detaching) {
            this.entries = entries;
            this.detaching = detaching;
        }

        @Override
        public Operation resume(RunContext run) throws WorldEditException {
            while (entries.hasNext()) {
                PackedBlockList.Entry entry = entries.next();
                if (detaching) {
                    detach(entry.position, entry.first, entry.second);
                } else {
                    write(entry.position, entry.first, entry.second);
                }
                if (!run.shouldContinue()) {
                    return this;
                }
            }
            return null;
        }

        @Override
        public void cancel() {}

        @Override
        public void addStatusMessages(List<String> messages) {}
    }

    private class Stage3Committer implements Operation {

        // Linked, so that taking the next block is quick however many have been placed already.
        private final Set<BlockVector> blocks = new LinkedHashSet<BlockVector>();
        private final Map<BlockVector, BaseBlock> blockTypes = new HashMap<BlockVector, BaseBlock>();
        private final Map<BlockVector, BaseBlock> previous = new HashMap<BlockVector, BaseBlock>();
        private Iterator<PackedBlockList.Entry> loading;

        @Override
        public Operation resume(RunContext run) throws WorldEditException {
            Extent extent = getExtent();

            if (loading == null) {
                loading = stage3.drainEntries();
            }
            while (loading.hasNext()) {
                final PackedBlockList.Entry entry = loading.next();
                final BlockVector pt = entry.position;
                blocks.add(pt);
                blockTypes.put(pt, entry.first);
                // A position queued twice keeps the block from before the first, which is what was there.
                if (!previous.containsKey(pt)) {
                    previous.put(pt, entry.second);
                }
                if (!run.shouldContinue()) {
                    return this;
                }
            }

            while (!blocks.isEmpty()) {
                BlockVector current = blocks.iterator()
                    .next();

                final Deque<BlockVector> walked = new LinkedList<BlockVector>();

                while (true) {
                    walked.addFirst(current);

                    assert (blockTypes.containsKey(current));

                    final BaseBlock baseBlock = blockTypes.get(current);

                    final int type = baseBlock.getType();
                    final int data = baseBlock.getData();

                    switch (type) {
                        case BlockID.WOODEN_DOOR:
                        case BlockID.IRON_DOOR:
                            if ((data & 0x8) == 0) {
                                // Deal with lower door halves being attached to the floor AND the upper half
                                BlockVector upperBlock = current.add(0, 1, 0)
                                    .toBlockVector();
                                if (blocks.contains(upperBlock) && !walked.contains(upperBlock)) {
                                    walked.addFirst(upperBlock);
                                }
                            }
                            break;

                        case BlockID.MINECART_TRACKS:
                        case BlockID.POWERED_RAIL:
                        case BlockID.DETECTOR_RAIL:
                        case BlockID.ACTIVATOR_RAIL:
                            // Here, rails are hardcoded to be attached to the block below them.
                            // They're also attached to the block they're ascending towards via BlockType.getAttachment.
                            BlockVector lowerBlock = current.add(0, -1, 0)
                                .toBlockVector();
                            if (blocks.contains(lowerBlock) && !walked.contains(lowerBlock)) {
                                walked.addFirst(lowerBlock);
                            }
                            break;
                    }

                    final PlayerDirection attachment = BlockType.getAttachment(type, data);
                    if (attachment == null) {
                        // Block is not attached to anything => we can place it
                        break;
                    }

                    current = current.add(attachment.vector())
                        .toBlockVector();

                    if (!blocks.contains(current)) {
                        // We ran outside the remaining set => assume we can place blocks on this
                        break;
                    }

                    if (walked.contains(current)) {
                        // Cycle detected => This will most likely go wrong, but there's nothing we can do about it.
                        break;
                    }
                }

                for (BlockVector pt : walked) {
                    write(pt, blockTypes.remove(pt), previous.remove(pt));
                    blocks.remove(pt);
                }

                // A whole chain at a time, so that nothing is placed before what it hangs from.
                if (!blocks.isEmpty() && !run.shouldContinue()) {
                    return this;
                }
            }

            detach.clear();
            stage1.clear();
            stage2.clear();
            stage3.clear();

            return null;
        }

        @Override
        public void cancel() {
            blocks.clear();
            blockTypes.clear();
            previous.clear();
        }

        @Override
        public void addStatusMessages(List<String> messages) {}

    }

}
