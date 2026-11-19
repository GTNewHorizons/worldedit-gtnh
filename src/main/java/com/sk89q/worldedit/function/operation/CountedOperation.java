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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import com.sk89q.worldedit.WorldEditException;

/**
 * An operation that reports its affected count from somewhere else, for cases where the object
 * doing the counting is not the operation being run.
 */
public class CountedOperation implements Operation, AffectedCounter {

    private final Operation original;
    private final AffectedCounter counter;
    private Operation current;

    /**
     * Create a new instance.
     *
     * @param operation the operation to run
     * @param counter   the object that knows how much was affected
     */
    public CountedOperation(Operation operation, AffectedCounter counter) {
        checkNotNull(operation);
        checkNotNull(counter);
        this.original = operation;
        this.current = operation;
        this.counter = counter;
    }

    @Override
    public int getAffected() {
        return counter.getAffected();
    }

    @Override
    public Operation resume(RunContext run) throws WorldEditException {
        if (current != null) {
            current = current.resume(run);
        }

        return current != null ? this : null;
    }

    @Override
    public void cancel() {
        if (current != null) {
            current.cancel();
        }
    }

    @Override
    public void addStatusMessages(List<String> messages) {
        original.addStatusMessages(messages);
    }
}
