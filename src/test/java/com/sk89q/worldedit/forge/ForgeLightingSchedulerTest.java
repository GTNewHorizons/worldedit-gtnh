package com.sk89q.worldedit.forge;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldedit.world.World;

import cpw.mods.fml.common.gameevent.TickEvent;

/** Tests for {@link ForgeLightingScheduler}. */
public class ForgeLightingSchedulerTest {

    private final ForgeLightingScheduler scheduler = new ForgeLightingScheduler();
    private final List<BlockVector2D> chunks = Arrays.asList(new BlockVector2D(0, 0), new BlockVector2D(1, 0));
    private int completions;

    private final Runnable completion = new Runnable() {

        @Override
        public void run() {
            completions++;
        }
    };

    private void ticks(int count) {
        for (int i = 0; i < count; i++) {
            scheduler.onServerTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
        }
    }

    @Test
    public void aSecondRequestIsQueuedInsteadOfDropped() {
        World first = mock(World.class);
        World second = mock(World.class);

        scheduler.schedule(first, chunks, null, completion);
        scheduler.schedule(second, chunks, null, completion);
        ticks(200);

        verify(first, atLeastOnce()).fixLighting(any());
        verify(second, atLeastOnce()).fixLighting(any());
        assertEquals(2, completions);
    }

    @Test
    public void aFailureDoesNotEscapeIntoTheServerTick() {
        World broken = mock(World.class);
        doThrow(new RuntimeException("expected by this test")).when(broken)
            .fixLighting(any());
        World healthy = mock(World.class);

        scheduler.schedule(broken, chunks, null, completion);
        scheduler.schedule(healthy, chunks, null, completion);
        ticks(200);

        verify(healthy, atLeastOnce()).fixLighting(any());
        assertEquals(2, completions);
    }

    @Test
    public void aFailingCompletionDoesNotEscapeIntoTheServerTick() {
        World world = mock(World.class);

        scheduler.schedule(world, chunks, null, new Runnable() {

            @Override
            public void run() {
                throw new RuntimeException("expected by this test");
            }
        });
        ticks(200);

        verify(world, atLeastOnce()).fixLighting(any());
    }

    @Test
    public void nothingRunsWithoutTicks() {
        World world = mock(World.class);

        scheduler.schedule(world, chunks, null, completion);

        verify(world, never()).fixLighting(any());
    }
}
