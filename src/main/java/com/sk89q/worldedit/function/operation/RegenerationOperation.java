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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;

/**
 * Regenerates chunks a step at a time, so that the server keeps ticking between steps.
 *
 * <p>
 * All terrain is replaced before anything is decorated, so features can cross between regenerated
 * chunks without being wiped by the next replacement. A chunk only gets part of its features from its
 * own decoration pass, with the rest coming from the chunks at -x and -z, so those run too.
 * </p>
 *
 * <p>
 * Each step is one chunk's worth of world generation, which is far too slow to be covered by the
 * clock checks in {@link RunContext#shouldContinue()}, so the budget is checked after every step.
 * </p>
 */
public abstract class RegenerationOperation implements Operation {

    private static final Logger logger = Logger.getLogger(RegenerationOperation.class.getCanonicalName());

    private static final int[][] NEIGHBOUR_PASSES = { { -1, 0 }, { 0, -1 }, { -1, -1 } };

    private enum Phase {
        REPLACE,
        DECORATE,
        DECORATE_NEIGHBOURS,
        RESTORE,
        COMMIT,
        RESEND,
        DONE
    }

    private final Deque<Vector2D> toReplace = new ArrayDeque<Vector2D>();
    private final List<Vector2D> replaced = new ArrayList<Vector2D>();
    private final Set<Long> replacedKeys = new HashSet<Long>();
    private Deque<Vector2D> toDecorate = new ArrayDeque<Vector2D>();
    private final Deque<int[]> neighbourPasses = new ArrayDeque<int[]>();
    private Deque<Vector2D> toRestore = new ArrayDeque<Vector2D>();
    private Deque<Vector2D> toResend = new ArrayDeque<Vector2D>();
    private Phase phase = Phase.REPLACE;
    private boolean commitStarted;
    private Operation commit;
    private boolean failed;

    /**
     * Create a new instance.
     *
     * @param chunks the chunks to regenerate
     */
    protected RegenerationOperation(Iterable<? extends Vector2D> chunks) {
        for (Vector2D chunk : chunks) {
            toReplace.add(chunk);
        }
    }

    /**
     * Record a chunk and swap it for freshly generated terrain.
     *
     * @param chunkX the chunk's X coordinate
     * @param chunkZ the chunk's Z coordinate
     */
    protected abstract void replaceChunk(int chunkX, int chunkZ);

    /**
     * Decorate a chunk that has been replaced.
     *
     * @param chunkX the chunk's X coordinate
     * @param chunkZ the chunk's Z coordinate
     */
    protected abstract void decorateChunk(int chunkX, int chunkZ);

    /**
     * Run decoration for a chunk that has not been replaced, for the features it would place in a
     * chunk that has been. Whatever it changes outside the replaced chunks has to be undone.
     *
     * @param chunkX the chunk's X coordinate
     * @param chunkZ the chunk's Z coordinate
     */
    protected abstract void decorateNeighbour(int chunkX, int chunkZ);

    /**
     * Put back what a replaced chunk held outside the selection, and record the rest for undo.
     *
     * @param chunkX the chunk's X coordinate
     * @param chunkZ the chunk's Z coordinate
     */
    protected abstract void restoreChunk(int chunkX, int chunkZ);

    /**
     * Get the operation that writes out everything the steps above have queued.
     *
     * @return the operation, or null if there is nothing to do
     */
    protected abstract Operation commitEdits();

    /**
     * Send a replaced chunk to the players watching it.
     *
     * @param chunkX the chunk's X coordinate
     * @param chunkZ the chunk's Z coordinate
     */
    protected abstract void resendChunk(int chunkX, int chunkZ);

    /**
     * Whether a chunk has been replaced by this operation.
     *
     * @param chunkX the chunk's X coordinate
     * @param chunkZ the chunk's Z coordinate
     * @return true if it has been replaced
     */
    protected final boolean isReplaced(int chunkX, int chunkZ) {
        return replacedKeys.contains(key(chunkX, chunkZ));
    }

    private static long key(int chunkX, int chunkZ) {
        return (long) chunkX << 32 | chunkZ & 0xFFFFFFFFL;
    }

    @Override
    public Operation resume(RunContext run) throws WorldEditException {
        while (phase != Phase.DONE) {
            step(run);
            if (phase != Phase.DONE && !run.shouldContinueAfterLargeStep()) {
                return this;
            }
        }

        if (failed) {
            failed = false;
            throw new RegenerationFailedException();
        }
        return null;
    }

    private void step(RunContext run) {
        switch (phase) {
            case REPLACE:
                replaceNext();
                break;
            case DECORATE:
                decorateNext();
                break;
            case DECORATE_NEIGHBOURS:
                decorateNextNeighbour();
                break;
            case RESTORE:
                restoreNext();
                break;
            case COMMIT:
                commitNext(run);
                break;
            case RESEND:
                resendNext();
                break;
            default:
                break;
        }
    }

    private void replaceNext() {
        Vector2D chunk = toReplace.poll();
        if (chunk == null) {
            startDecorating();
            return;
        }

        try {
            replaceChunk(chunk.getBlockX(), chunk.getBlockZ());
            replaced.add(chunk);
            replacedKeys.add(key(chunk.getBlockX(), chunk.getBlockZ()));
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Failed to regenerate chunk " + chunk, t);
            failed = true;
            toReplace.clear();
        }
    }

    private void startDecorating() {
        toDecorate = new ArrayDeque<Vector2D>(replaced);
        toRestore = new ArrayDeque<Vector2D>(replaced);
        toResend = new ArrayDeque<Vector2D>(replaced);

        Set<Long> seen = new HashSet<Long>();
        for (Vector2D chunk : replaced) {
            for (int[] offset : NEIGHBOUR_PASSES) {
                int x = chunk.getBlockX() + offset[0];
                int z = chunk.getBlockZ() + offset[1];
                if (!isReplaced(x, z) && seen.add(key(x, z))) {
                    neighbourPasses.add(new int[] { x, z });
                }
            }
        }
        phase = Phase.DECORATE;
    }

    private void decorateNext() {
        Vector2D chunk = toDecorate.poll();
        if (chunk == null) {
            phase = Phase.DECORATE_NEIGHBOURS;
            return;
        }

        try {
            decorateChunk(chunk.getBlockX(), chunk.getBlockZ());
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Failed to decorate chunk " + chunk, t);
            failed = true;
        }
    }

    private void decorateNextNeighbour() {
        int[] pass = neighbourPasses.poll();
        if (pass == null) {
            phase = Phase.RESTORE;
            return;
        }

        try {
            decorateNeighbour(pass[0], pass[1]);
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Failed to decorate around chunk " + pass[0] + ", " + pass[1], t);
            failed = true;
        }
    }

    private void restoreNext() {
        Vector2D chunk = toRestore.poll();
        if (chunk == null) {
            phase = Phase.COMMIT;
            return;
        }

        try {
            restoreChunk(chunk.getBlockX(), chunk.getBlockZ());
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Failed to restore chunk " + chunk, t);
            failed = true;
        }
    }

    private void commitNext(RunContext run) {
        try {
            if (!commitStarted) {
                commitStarted = true;
                commit = commitEdits();
            }
            if (commit != null) {
                commit = commit.resume(run);
            }
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Failed to write out regenerated chunks", t);
            failed = true;
            commit = null;
        }

        if (commit == null) {
            phase = Phase.RESEND;
        }
    }

    private void resendNext() {
        Vector2D chunk = toResend.poll();
        if (chunk == null) {
            phase = Phase.DONE;
            return;
        }

        try {
            resendChunk(chunk.getBlockX(), chunk.getBlockZ());
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Failed to send chunk " + chunk, t);
            failed = true;
        }
    }

    /**
     * Stop replacing chunks, and finish the ones already replaced before returning. Those have lost
     * what they held, so leaving them undecorated and unrestored would not be a clean stop.
     */
    @Override
    public void cancel() {
        toReplace.clear();
        RunContext run = new RunContext();
        while (phase != Phase.DONE) {
            step(run);
        }
    }

    @Override
    public void addStatusMessages(List<String> messages) {
        messages.add(replaced.size() + " chunks regenerated");
    }

    /**
     * Thrown once an operation has finished if any of its steps failed.
     */
    public static final class RegenerationFailedException extends WorldEditException {

        private RegenerationFailedException() {
            super("Some of the region could not be regenerated. See the server log.");
        }
    }
}
