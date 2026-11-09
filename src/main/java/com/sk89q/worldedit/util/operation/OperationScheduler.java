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

package com.sk89q.worldedit.util.operation;

import java.util.List;
import java.util.UUID;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.function.operation.Operation;

/**
 * Runs {@link Operation}s on behalf of a command.
 *
 * <p>
 * The default implementation ({@link DirectOperationScheduler}) runs an operation to completion
 * before returning, which is how WorldEdit has always behaved. A platform may install an
 * implementation that spreads the work over several ticks instead, so a large edit does not block
 * the server thread until it finishes.
 * </p>
 *
 * <p>
 * An operation runs in two phases: the operation itself, and then the edit session's
 * {@link EditSession#commit() commit}, which is where buffered blocks actually reach the world when
 * the reorder queue is enabled. A scheduler must drive both before reporting completion.
 * </p>
 */
public interface OperationScheduler {

    /**
     * Told to a player who tries to edit straight away while edits of theirs are still queued.
     */
    String STILL_RUNNING = "You have an edit still running. Wait for it to finish, or use //cancel.";

    /**
     * Submit an operation to run against the given edit session.
     *
     * @param owner       the actor the work is done for, who is the one able to cancel it
     * @param editSession the edit session the operation writes to
     * @param operation   the operation to run
     * @param completion  notified once both phases have finished
     * @return true if the operation was queued and will finish later, false if it already ran to
     *         completion before this method returned
     */
    boolean submit(Actor owner, EditSession editSession, Operation operation, OperationCompletion completion);

    /**
     * Whether work submitted for the given edit session has not finished yet.
     *
     * @param editSession the edit session
     * @return true if work is still queued or running
     */
    boolean isPending(EditSession editSession);

    /**
     * Flush the edit session and then run the given task, either immediately if no work is pending
     * for it, or after that work finishes.
     *
     * <p>
     * The scheduler is responsible for flushing because a flush has to wait for queued work: it
     * writes out whatever the reorder queue holds, so running it early would write a half-built
     * edit. The task itself must not flush.
     * </p>
     *
     * @param editSession the edit session
     * @param task        the task to run once the session is finished with
     */
    void whenFinished(EditSession editSession, Runnable task);

    /**
     * Stop all work submitted by the given owner, running or queued.
     *
     * <p>
     * A cancelled edit's reorder queue is discarded, so blocks still waiting in it are dropped and
     * blocks already written stay written. The task given to {@link #whenFinished} still runs before
     * this method returns, which records the edit for undo. Its history can cover more than reached
     * the world, which is harmless: undoing a block that was never changed writes what is already
     * there.
     * </p>
     *
     * @param owner the unique ID of the owner
     * @return the edit sessions that were cancelled, oldest first
     */
    List<EditSession> cancel(UUID owner);

    /**
     * Whether an owner has work that is running or queued.
     *
     * @param owner the unique ID of the owner
     * @return true if there is unfinished work
     */
    boolean hasPendingWork(UUID owner);

    /**
     * Run any queued work to completion and stop.
     */
    void shutdown();
}
