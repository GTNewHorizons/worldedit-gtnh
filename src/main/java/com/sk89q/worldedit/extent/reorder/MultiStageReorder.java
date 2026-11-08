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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.sk89q.worldedit.function.operation.BlockMapEntryPlacer;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.OperationQueue;
import com.sk89q.worldedit.function.operation.RunContext;
import com.sk89q.worldedit.util.collection.PackedBlockList;

/**
 * Re-orders blocks into several stages.
 */
public class MultiStageReorder extends AbstractDelegateExtent implements ReorderingExtent {

    private final PackedBlockList stage1 = new PackedBlockList(1);
    private final PackedBlockList stage2 = new PackedBlockList(1);
    private final PackedBlockList stage3 = new PackedBlockList(1);
    private boolean enabled;

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

    @Override
    public boolean setBlock(Vector location, BaseBlock block) throws WorldEditException {
        if (!enabled || !PackedBlockList.fits(location)) {
            return super.setBlock(location, block);
        }

        BaseBlock lazyBlock = getLazyBlock(location);

        if (BlockType.shouldPlaceLast(block.getType())) {
            // Place torches, etc. last
            stage2.add(location, block);
            return !(lazyBlock.getType() == block.getType() && lazyBlock.getData() == block.getData());
        } else if (BlockType.shouldPlaceFinal(block.getType())) {
            // Place signs, reed, etc even later
            stage3.add(location, block);
            return !(lazyBlock.getType() == block.getType() && lazyBlock.getData() == block.getData());
        } else if (BlockType.shouldPlaceLast(lazyBlock.getType())) {
            // Destroy torches, etc. first
            super.setBlock(location, new BaseBlock(BlockID.AIR));
            return super.setBlock(location, block);
        } else {
            stage1.add(location, block);
            return !(lazyBlock.getType() == block.getType() && lazyBlock.getData() == block.getData());
        }
    }

    /**
     * Throw away everything waiting to be written.
     */
    public void discard() {
        stage1.clear();
        stage2.clear();
        stage3.clear();
    }

    @Override
    public Operation commitBefore() {
        return new OperationQueue(
            new BlockMapEntryPlacer(getExtent(), Iterators.concat(stage1.drain(), stage2.drain())),
            new Stage3Committer());
    }

    private class Stage3Committer implements Operation {

        @Override
        public Operation resume(RunContext run) throws WorldEditException {
            Extent extent = getExtent();

            final Set<BlockVector> blocks = new HashSet<BlockVector>();
            final Map<BlockVector, BaseBlock> blockTypes = new HashMap<BlockVector, BaseBlock>();
            for (Iterator<Map.Entry<BlockVector, BaseBlock>> it = stage3.drain(); it.hasNext();) {
                final Map.Entry<BlockVector, BaseBlock> entry = it.next();
                final BlockVector pt = entry.getKey();
                blocks.add(pt);
                blockTypes.put(pt, entry.getValue());
            }

            while (!blocks.isEmpty()) {
                BlockVector current = blocks.iterator()
                    .next();
                if (!blocks.contains(current)) {
                    continue;
                }

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
                    extent.setBlock(pt, blockTypes.get(pt));
                    blocks.remove(pt);
                }
            }

            stage1.clear();
            stage2.clear();
            stage3.clear();

            return null;
        }

        @Override
        public void cancel() {}

        @Override
        public void addStatusMessages(List<String> messages) {}

    }

}
