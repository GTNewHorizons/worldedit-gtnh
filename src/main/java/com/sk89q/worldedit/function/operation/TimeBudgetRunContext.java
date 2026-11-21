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
 * amortised across up to {@value #CHECK_INTERVAL} calls, with shorter intervals for small budgets.
 * Operations can overshoot by the time it takes to complete one such interval.
 * </p>
 */
public class TimeBudgetRunContext extends RunContext {

    /**
     * How many calls to {@link #shouldContinue()} pass before the clock is read again.
     */
    private static final int CHECK_INTERVAL = 256;

    private final long deadline;
    private final int checkInterval;
    private int countdown;
    private boolean expired;

    /**
     * Create a context that allows work until the given number of nanoseconds have elapsed.
     *
     * @param budgetNanos the budget, in nanoseconds
     */
    public TimeBudgetRunContext(long budgetNanos) {
        this.deadline = System.nanoTime() + budgetNanos;
        // A busy server can give the edit as little as a millisecond. Check more frequently
        // on those ticks, especially when the operation is placing tile entities.
        this.checkInterval = budgetNanos < 5_000_000L ? 16 : CHECK_INTERVAL;
        this.countdown = checkInterval;
    }

    @Override
    public boolean shouldContinue() {
        if (expired) {
            return false;
        }

        if (--countdown > 0) {
            return true;
        }

        countdown = checkInterval;
        return checkClock();
    }

    @Override
    public boolean waitForNextTick() {
        // Spending the rest of the budget would only spin on the server thread.
        expired = true;
        return true;
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
