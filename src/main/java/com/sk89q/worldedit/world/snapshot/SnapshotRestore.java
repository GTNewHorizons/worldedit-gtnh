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

package com.sk89q.worldedit.world.snapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.operation.RunContext;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.DataException;
import com.sk89q.worldedit.world.chunk.Chunk;
import com.sk89q.worldedit.world.storage.ChunkStore;
import com.sk89q.worldedit.world.storage.MissingChunkException;
import com.sk89q.worldedit.world.storage.MissingWorldException;

/**
 * Restores a region from a snapshot one chunk at a time, so that it can be spread over several server
 * ticks. Each chunk has to be read and decompressed from the snapshot, which is the slow part.
 */
public class SnapshotRestore implements Operation {

    private final ChunkStore chunkStore;
    private final EditSession editSession;
    private final Region region;
    private final List<Vector2D> chunks;
    private final Iterator<Vector2D> remaining;
    private final ArrayList<Vector2D> missingChunks = new ArrayList<Vector2D>();
    private final ArrayList<Vector2D> errorChunks = new ArrayList<Vector2D>();
    private String lastErrorMessage;

    /**
     * Construct the snapshot restore operation.
     *
     * @param chunkStore  The {@link ChunkStore} to restore from
     * @param editSession The {@link EditSession} to restore to
     * @param region      The {@link Region} to restore to, which is copied so that later changes to it
     *                    don't affect the restore
     */
    public SnapshotRestore(ChunkStore chunkStore, EditSession editSession, Region region) {
        this.chunkStore = chunkStore;
        this.editSession = editSession;
        this.region = region.clone();
        this.chunks = new ArrayList<Vector2D>(this.region.getChunks());
        this.remaining = chunks.iterator();
    }

    /**
     * Get the number of chunks that are needed.
     *
     * @return a number of chunks
     */
    public int getChunksAffected() {
        return chunks.size();
    }

    /**
     * Restores to world in one go.
     *
     * @throws MaxChangedBlocksException
     */
    public void restore() throws MaxChangedBlocksException {
        Operations.completeLegacy(this);
    }

    @Override
    public Operation resume(RunContext run) throws WorldEditException {
        while (remaining.hasNext()) {
            restoreChunk(remaining.next());
            if (!run.shouldContinueAfterLargeStep()) {
                return remaining.hasNext() ? this : null;
            }
        }
        return null;
    }

    private void restoreChunk(Vector2D chunkPos) throws MaxChangedBlocksException {
        Chunk chunk;
        try {
            chunk = chunkStore.getChunk(chunkPos, editSession.getWorld());
        } catch (MissingChunkException me) {
            missingChunks.add(chunkPos);
            return;
        } catch (MissingWorldException me) {
            errorChunks.add(chunkPos);
            lastErrorMessage = me.getMessage();
            return;
        } catch (DataException de) {
            errorChunks.add(chunkPos);
            lastErrorMessage = de.getMessage();
            return;
        } catch (IOException ioe) {
            errorChunks.add(chunkPos);
            lastErrorMessage = ioe.getMessage();
            return;
        }

        Vector min = region.getMinimumPoint();
        Vector max = region.getMaximumPoint();
        int minX = Math.max(min.getBlockX(), chunkPos.getBlockX() << 4);
        int maxX = Math.min(max.getBlockX(), (chunkPos.getBlockX() << 4) + 15);
        int minZ = Math.max(min.getBlockZ(), chunkPos.getBlockZ() << 4);
        int maxZ = Math.min(max.getBlockZ(), (chunkPos.getBlockZ() << 4) + 15);
        Mask mask = editSession.getMask();

        for (int x = minX; x <= maxX; ++x) {
            for (int z = minZ; z <= maxZ; ++z) {
                for (int y = min.getBlockY(); y <= max.getBlockY(); ++y) {
                    Vector pos = new Vector(x, y, z);
                    if (!region.contains(pos) || (mask != null && !mask.test(pos))) {
                        continue;
                    }
                    try {
                        editSession.setBlock(pos, chunk.getBlock(pos));
                    } catch (DataException e) {
                        // this is a workaround: just ignore for now
                    }
                }
            }
        }
    }

    @Override
    public void cancel() {}

    @Override
    public void addStatusMessages(List<String> messages) {}

    /**
     * Get a list of the missing chunks.
     *
     * @return a list of coordinates
     */
    public List<Vector2D> getMissingChunks() {
        return missingChunks;
    }

    /**
     * Get a list of the chunks that could not have been loaded for other reasons.
     *
     * @return a list of coordinates
     */
    public List<Vector2D> getErrorChunks() {
        return errorChunks;
    }

    /**
     * Checks to see where the backup succeeded in any capacity. False will
     * be returned if no chunk could be successfully loaded.
     *
     * @return true if there was total failure
     */
    public boolean hadTotalFailure() {
        return missingChunks.size() + errorChunks.size() == getChunksAffected();
    }

    /**
     * Get the last error message.
     *
     * @return a message
     */
    public String getLastErrorMessage() {
        return lastErrorMessage;
    }
}
