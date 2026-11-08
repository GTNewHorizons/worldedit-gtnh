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

import com.sk89q.worldedit.EditSession;
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
     * Submit an operation to run against the given edit session.
     *
     * @param editSession the edit session the operation writes to
     * @param operation   the operation to run
     * @param completion  notified once both phases have finished
     * @return true if the operation was queued and will finish later, false if it already ran to
     *         completion before this method returned
     */
    boolean submit(EditSession editSession, Operation operation, OperationCompletion completion);

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
     * The scheduler is responsible for flushing, so the task must not flush again: the reorder
     * stages are not cleared on commit, and flushing twice would write every buffered block a
     * second time.
     * </p>
     *
     * @param editSession the edit session
     * @param task        the task to run once the session is finished with
     */
    void whenFinished(EditSession editSession, Runnable task);

    /**
     * Drop any queued work and stop.
     */
    void shutdown();
}
