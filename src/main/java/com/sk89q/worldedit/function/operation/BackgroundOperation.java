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
import static com.google.common.base.Preconditions.checkState;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.sk89q.worldedit.WorldEditException;

/**
 * Runs work that doesn't touch the world, such as reading a file, on a background thread, and waits
 * for it without holding up the server thread.
 *
 * <p>
 * The work must not touch the world or anything else that only the server thread may use. Its
 * result is picked up on the server thread once the operation finishes.
 * </p>
 *
 * @param <T> the type of the result
 */
public final class BackgroundOperation<T> implements Operation {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "WorldEdit background work");
        thread.setDaemon(true);
        return thread;
    });

    private final Callable<T> work;
    private Future<T> future;
    private T result;
    private boolean done;

    /**
     * Create a new instance.
     *
     * @param work the work to run on the background thread
     */
    public BackgroundOperation(Callable<T> work) {
        this.work = checkNotNull(work);
    }

    @Override
    public Operation resume(RunContext run) throws WorldEditException {
        if (future == null) {
            future = EXECUTOR.submit(work);
        }

        if (!future.isDone() && run.waitForNextTick()) {
            return this;
        }

        try {
            result = future.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new BackgroundWorkFailedException(cause);
        } catch (InterruptedException e) {
            Thread.currentThread()
                .interrupt();
            throw new BackgroundWorkFailedException(e);
        }
        done = true;
        return null;
    }

    /**
     * Get the result of the work.
     *
     * @return the result
     * @throws IllegalStateException if the operation hasn't finished
     */
    public T getResult() {
        checkState(done, "the work hasn't finished");
        return result;
    }

    @Override
    public void cancel() {
        if (future != null) {
            future.cancel(true);
        }
    }

    @Override
    public void addStatusMessages(List<String> messages) {}

    /**
     * Thrown when the background work fails, carrying its message.
     */
    public static final class BackgroundWorkFailedException extends WorldEditException {

        private BackgroundWorkFailedException(Throwable cause) {
            super(cause.getMessage() != null ? cause.getMessage() : cause.toString(), cause);
        }
    }
}
