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

package com.sk89q.worldedit.forge;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.RunContext;
import com.sk89q.worldedit.function.operation.TimeBudgetRunContext;
import com.sk89q.worldedit.util.operation.DirectOperationScheduler;
import com.sk89q.worldedit.util.operation.OperationCompletion;
import com.sk89q.worldedit.util.operation.OperationScheduler;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * An operation scheduler that spends a fixed slice of each server tick on queued work, so a large
 * edit no longer blocks the server thread until it finishes.
 *
 * <p>
 * Commands are dispatched on the server thread and ticks run on the server thread, so this class
 * assumes single-threaded access and does not lock.
 * </p>
 */
public class ForgeOperationScheduler implements OperationScheduler {

    /**
     * Wall-clock time per server tick that queued operations may use. A tick is 50ms, so this
     * leaves the rest of the tick for everything else the server has to do.
     */
    private static final long BUDGET_NANOS = 20L * 1000L * 1000L;

    /**
     * Once this many jobs are queued, further submissions run synchronously instead, so a player
     * spamming commands cannot grow the backlog without bound.
     */
    private static final int MAX_QUEUED = 8;

    private static final Logger logger = Logger.getLogger(ForgeOperationScheduler.class.getCanonicalName());

    private final Deque<Job> queue = new ArrayDeque<Job>();
    private final DirectOperationScheduler fallback = new DirectOperationScheduler();
    private Job current;
    private boolean registered;

    @Override
    public boolean submit(Actor owner, EditSession editSession, Operation operation, OperationCompletion completion) {
        checkNotNull(owner);
        checkNotNull(editSession);
        checkNotNull(operation);
        checkNotNull(completion);

        if (queue.size() >= MAX_QUEUED) {
            return fallback.submit(owner, editSession, operation, completion);
        }

        queue.offer(new Job(owner.getUniqueId(), editSession, operation, completion));
        return true;
    }

    @Override
    public boolean isPending(EditSession editSession) {
        return find(editSession) != null;
    }

    @Override
    public void whenFinished(EditSession editSession, Runnable task) {
        checkNotNull(editSession);
        checkNotNull(task);

        Job job = find(editSession);
        if (job == null) {
            // Nothing queued for this session, so it was an ordinary synchronous command.
            editSession.flushQueue();
            task.run();
            return;
        }

        job.finalizer = task;
    }

    @Override
    public List<EditSession> cancel(UUID owner) {
        checkNotNull(owner);

        List<Job> cancelled = new ArrayList<Job>();
        if (current != null && owner.equals(current.owner)) {
            cancelled.add(current);
            current = null;
        }
        for (Iterator<Job> it = queue.iterator(); it.hasNext();) {
            Job job = it.next();
            if (owner.equals(job.owner)) {
                cancelled.add(job);
                it.remove();
            }
        }

        List<EditSession> editSessions = new ArrayList<EditSession>(cancelled.size());
        for (Job job : cancelled) {
            abort(job);
            editSessions.add(job.editSession);
        }
        return editSessions;
    }

    @Override
    public void shutdown() {
        unregister();

        // Run whatever is left to completion rather than leaving edits half applied.
        Job job = current;
        current = null;
        if (job != null) {
            drain(job);
        }
        while (!queue.isEmpty()) {
            drain(queue.poll());
        }
    }

    private Job find(EditSession editSession) {
        if (current != null && current.editSession == editSession) {
            return current;
        }
        for (Job job : queue) {
            if (job.editSession == editSession) {
                return job;
            }
        }
        return null;
    }

    /**
     * Start receiving server ticks. Until this is called, submitted work stays queued.
     */
    public void register() {
        if (!registered) {
            FMLCommonHandler.instance()
                .bus()
                .register(this);
            registered = true;
        }
    }

    private void unregister() {
        if (registered) {
            FMLCommonHandler.instance()
                .bus()
                .unregister(this);
            registered = false;
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (current == null) {
            current = queue.poll();
        }

        Job job = current;
        if (job == null) {
            return;
        }

        RunContext run = new TimeBudgetRunContext(BUDGET_NANOS);
        do {
            if (advance(job, run)) {
                current = null;
                finish(job);
                return;
            }
        } while (run.shouldContinue());
    }

    /**
     * Push a job forward as far as the given budget allows.
     *
     * @param job the job
     * @param run the budget
     * @return true if the job is finished and should be reported
     */
    private boolean advance(Job job, RunContext run) {
        while (true) {
            if (job.operation != null) {
                try {
                    job.operation = job.operation.resume(run);
                } catch (WorldEditException e) {
                    fail(job, e);
                } catch (RuntimeException e) {
                    logger.log(Level.WARNING, "An operation failed", e);
                    fail(job, new OperationCrashedException(e));
                }

                if (job.operation != null) {
                    return false;
                }
            }

            if (!job.committing) {
                // The operation itself only buffers blocks while the reorder queue is enabled;
                // committing is what actually writes them, so it gets budgeted too.
                job.committing = true;
                try {
                    job.operation = job.editSession.commit();
                } catch (RuntimeException e) {
                    logger.log(Level.WARNING, "Failed to commit an edit session", e);
                    fail(job, new OperationCrashedException(e));
                }
                continue;
            }

            return true;
        }
    }

    /**
     * Stop the job's current phase. A failed operation still gets committed, so what it did write
     * reaches the world and can be undone. The first failure is the one reported.
     */
    private void fail(Job job, WorldEditException e) {
        if (job.failure == null) {
            job.failure = e;
        }
        job.operation = null;
    }

    private void drain(Job job) {
        RunContext run = new RunContext();
        while (!advance(job, run)) {
            // RunContext never asks to stop, so this converges.
        }
        finish(job);
    }

    private void finish(Job job) {
        try {
            if (job.failure != null) {
                job.completion.onFailure(job.failure);
            } else {
                job.completion.onSuccess();
            }
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "An operation completion handler failed", e);
        } finally {
            runFinalizer(job);
        }
    }

    private void abort(Job job) {
        try {
            if (job.operation != null) {
                job.operation.cancel();
            }
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "Failed to cancel an operation", e);
        } finally {
            runFinalizer(job);
        }
    }

    private void runFinalizer(Job job) {
        if (job.finalizer != null) {
            try {
                job.finalizer.run();
            } catch (RuntimeException e) {
                logger.log(Level.WARNING, "Failed to finalize an edit session", e);
            }
        }
    }

    private static final class OperationCrashedException extends WorldEditException {

        private OperationCrashedException(RuntimeException cause) {
            super("An unexpected error stopped this edit. See the server log.", cause);
        }
    }

    private static final class Job {

        private final UUID owner;
        private final EditSession editSession;
        private final OperationCompletion completion;
        private Operation operation;
        private boolean committing;
        private WorldEditException failure;
        private Runnable finalizer;

        private Job(UUID owner, EditSession editSession, Operation operation, OperationCompletion completion) {
            this.owner = owner;
            this.editSession = editSession;
            this.operation = operation;
            this.completion = completion;
        }
    }
}
