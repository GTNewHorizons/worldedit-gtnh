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
 * A {@link RunContext} that stops an operation once it has used up a wall-clock budget, so a large
 * edit can be spread over several server ticks instead of blocking the server thread until it
 * finishes.
 *
 * <p>
 * Operations call {@link #shouldContinue()} once per unit of work, which for a block visitor means
 * once per block. Reading the clock that often would itself be measurable, so the check is
 * amortised: only every {@value #CHECK_INTERVAL} calls actually looks at the clock. An operation
 * can therefore overshoot its budget by up to {@value #CHECK_INTERVAL} units of work, which is the
 * intended trade-off.
 * </p>
 */
public class TimeBudgetRunContext extends RunContext {

    /**
     * How many calls to {@link #shouldContinue()} pass before the clock is read again.
     */
    private static final int CHECK_INTERVAL = 256;

    private final long deadline;
    private int countdown = CHECK_INTERVAL;
    private boolean expired;

    /**
     * Create a context that allows work until the given number of nanoseconds have elapsed.
     *
     * @param budgetNanos the budget, in nanoseconds
     */
    public TimeBudgetRunContext(long budgetNanos) {
        this.deadline = System.nanoTime() + budgetNanos;
    }

    @Override
    public boolean shouldContinue() {
        if (expired) {
            return false;
        }

        if (--countdown > 0) {
            return true;
        }

        countdown = CHECK_INTERVAL;
        return checkClock();
    }

    @Override
    public boolean shouldContinueAfterLargeStep() {
        return !expired && checkClock();
    }

    private boolean checkClock() {
        if (System.nanoTime() >= deadline) {
            expired = true;
            return false;
        }

        return true;
    }
}
