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

import com.sk89q.worldedit.WorldEditException;

/**
 * Notified once an operation submitted to an {@link OperationScheduler} has finished, which may be
 * several server ticks after the command that submitted it returned. An operation that gets cancelled
 * gets {@link #onCancelled(boolean)} instead.
 */
public interface OperationCompletion {

    /**
     * Called after the operation and the edit session's commit have both run to completion.
     */
    void onSuccess();

    /**
     * Called if the operation aborted with an exception.
     *
     * @param e the exception that stopped the operation
     */
    void onFailure(WorldEditException e);

    /**
     * Called if the operation was cancelled before it finished.
     *
     * @param started false if it was cancelled while waiting for its turn, so it changed nothing
     */
    default void onCancelled(boolean started) {}
}
