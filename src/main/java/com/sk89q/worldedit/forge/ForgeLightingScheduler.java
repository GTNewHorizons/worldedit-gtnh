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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.util.lighting.LightingScheduler;
import com.sk89q.worldedit.world.World;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * A lighting scheduler that batches work across server ticks.
 */
public class ForgeLightingScheduler implements LightingScheduler {

    private static final Logger logger = Logger.getLogger(ForgeLightingScheduler.class.getCanonicalName());
    private static final int CHUNKS_PER_SECOND = 8;
    private static final double TICKS_PER_SECOND = 20.0;

    private final Object lock = new Object();
    private final Deque<LightingTask> queue = new ArrayDeque<LightingTask>();
    private LightingTask currentTask;
    private boolean registered;
    private double availableChunkBudget;

    @Override
    public void schedule(World world, Iterable<BlockVector2D> chunks, Player player, Runnable completion) {
        checkNotNull(world);
        checkNotNull(chunks);

        LightingTask task = new LightingTask(world, chunks, completion);
        if (task.isEmpty()) {
            if (completion != null) {
                completion.run();
            }
            return;
        }

        synchronized (lock) {
            queue.offer(task);
        }
    }

    @Override
    public void shutdown() {
        unregister();
        synchronized (lock) {
            queue.clear();
            currentTask = null;
        }
    }

    /**
     * Start receiving server ticks. Until this is called, scheduled work stays queued.
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

        availableChunkBudget = Math
            .min(CHUNKS_PER_SECOND, availableChunkBudget + (CHUNKS_PER_SECOND / TICKS_PER_SECOND));

        LightingTask task;
        synchronized (lock) {
            if (currentTask == null) {
                currentTask = queue.poll();
            }
            task = currentTask;
        }

        if (task == null) {
            return;
        }

        int allowed = (int) Math.floor(availableChunkBudget);
        if (allowed <= 0) {
            return;
        }

        List<BlockVector2D> batch = task.pollBatch(allowed);
        if (!batch.isEmpty()) {
            try {
                task.getWorld()
                    .fixLighting(batch);
            } catch (RuntimeException e) {
                // For example the world was unloaded. Exceptions here would take the server down.
                logger.log(Level.WARNING, "Failed to fix lighting; giving up on the remaining chunks", e);
                task.abandon();
            }
            availableChunkBudget -= batch.size();
        }

        if (task.isEmpty()) {
            Runnable completion = task.getCompletion();
            synchronized (lock) {
                currentTask = null;
            }
            if (completion != null) {
                try {
                    completion.run();
                } catch (RuntimeException e) {
                    logger.log(Level.WARNING, "A lighting fix completion handler failed", e);
                }
            }
        }
    }

    private static class LightingTask {

        private final World world;
        private final Deque<BlockVector2D> remaining = new ArrayDeque<BlockVector2D>();
        private final Runnable completion;

        private LightingTask(World world, Iterable<BlockVector2D> chunks, Runnable completion) {
            this.world = world;
            this.completion = completion;
            for (BlockVector2D chunk : chunks) {
                remaining.offer(chunk);
            }
        }

        private World getWorld() {
            return world;
        }

        private Runnable getCompletion() {
            return completion;
        }

        private boolean isEmpty() {
            return remaining.isEmpty();
        }

        private void abandon() {
            remaining.clear();
        }

        private List<BlockVector2D> pollBatch(int maxSize) {
            List<BlockVector2D> batch = new ArrayList<BlockVector2D>(Math.min(remaining.size(), maxSize));
            for (int i = 0; i < maxSize; i++) {
                BlockVector2D next = remaining.poll();
                if (next == null) {
                    break;
                }
                batch.add(next);
            }
            return batch;
        }
    }
}
