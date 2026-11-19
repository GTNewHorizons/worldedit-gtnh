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

/**
 * Describes the current run.
 */
public class RunContext {

    /**
     * Return whether the current operation should still continue running.
     *
     * <p>
     * This method can be called frequently.
     * </p>
     *
     * @return true if the operation should continue running
     */
    public boolean shouldContinue() {
        return true;
    }

    /**
     * Same as {@link #shouldContinue()}, for an operation that has just finished a large piece of work
     * such as generating a chunk, where an implementation must not put off checking its budget.
     *
     * @return true if the operation should continue running
     */
    public boolean shouldContinueAfterLargeStep() {
        return shouldContinue();
    }

    /**
     * Ask to be resumed on a later tick, for an operation that is waiting on something else and has
     * nothing to do yet.
     *
     * @return true if the operation should return and will be resumed later, false if this context
     *         can't do that and the operation has to wait where it is
     */
    public boolean waitForNextTick() {
        return false;
    }

}
