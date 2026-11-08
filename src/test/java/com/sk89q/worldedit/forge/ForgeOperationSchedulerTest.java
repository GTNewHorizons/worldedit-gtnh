package com.sk89q.worldedit.forge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.RunContext;
import com.sk89q.worldedit.session.request.Request;
import com.sk89q.worldedit.util.operation.OperationCompletion;
import com.sk89q.worldedit.world.World;

import cpw.mods.fml.common.gameevent.TickEvent;

/** Tests for {@link ForgeOperationScheduler}. */
public class ForgeOperationSchedulerTest {

    private final ForgeOperationScheduler scheduler = new ForgeOperationScheduler();
    private final UUID ownerId = UUID.randomUUID();
    private final Actor owner = actor(ownerId);
    private final List<String> events = new ArrayList<String>();

    @Test
    public void workIsSpreadOverTicksAndThenCommittedAndFinalized() {
        EditSession session = mock(EditSession.class);
        FakeOperation operation = new FakeOperation(3);
        submit(owner, session, operation, "a");

        tick();
        assertEquals(1, operation.resumes);
        assertTrue(events.isEmpty());
        verify(session, never()).commit();

        tick();
        tick();

        assertEquals(3, operation.resumes);
        verify(session, times(1)).commit();
        assertEquals(Arrays.asList("success a", "finalize a"), events);
    }

    @Test
    public void cancelStopsARunningJob() {
        EditSession session = mock(EditSession.class);
        FakeOperation operation = new FakeOperation(100);
        submit(owner, session, operation, "a");
        tick();

        assertEquals(Collections.singletonList(session), scheduler.cancel(ownerId));
        tick();

        assertEquals(1, operation.resumes);
        assertTrue(operation.cancelled);
        verify(session, never()).commit();
        assertEquals(Collections.singletonList("finalize a"), events);
        assertFalse(scheduler.isPending(session));
    }

    @Test
    public void cancelDropsQueuedJobsOldestFirst() {
        EditSession first = mock(EditSession.class);
        EditSession second = mock(EditSession.class);
        FakeOperation firstOperation = new FakeOperation(100);
        FakeOperation secondOperation = new FakeOperation(100);
        submit(owner, first, firstOperation, "a");
        submit(owner, second, secondOperation, "b");
        tick();

        assertEquals(Arrays.asList(first, second), scheduler.cancel(ownerId));

        assertEquals(0, secondOperation.resumes);
        assertEquals(Arrays.asList("finalize a", "finalize b"), events);
    }

    @Test
    public void cancelDuringCommitStopsTheCommit() {
        EditSession session = mock(EditSession.class);
        FakeOperation commit = new FakeOperation(100);
        when(session.commit()).thenReturn(commit);
        submit(owner, session, new FakeOperation(1), "a");
        tick();
        assertEquals(1, commit.resumes);

        scheduler.cancel(ownerId);
        tick();

        assertEquals(1, commit.resumes);
        assertTrue(commit.cancelled);
        assertEquals(Collections.singletonList("finalize a"), events);
    }

    @Test
    public void cancelLeavesOtherOwnersAlone() {
        EditSession mine = mock(EditSession.class);
        EditSession theirs = mock(EditSession.class);
        UUID otherId = UUID.randomUUID();
        FakeOperation myOperation = new FakeOperation(100);
        FakeOperation theirOperation = new FakeOperation(100);
        submit(actor(otherId), theirs, theirOperation, "theirs");
        submit(owner, mine, myOperation, "mine");
        tick();

        assertEquals(Collections.singletonList(mine), scheduler.cancel(ownerId));
        tick();

        assertEquals(2, theirOperation.resumes);
        assertEquals(0, myOperation.resumes);
        assertTrue(scheduler.isPending(theirs));
    }

    @Test
    public void cancelWithNothingRunningReturnsNothing() {
        assertTrue(
            scheduler.cancel(ownerId)
                .isEmpty());
    }

    @Test
    public void crashIsReportedAsFailureNotSuccess() {
        EditSession session = mock(EditSession.class);
        submit(owner, session, new CrashingOperation(), "a");

        tick();

        verify(session, times(1)).commit();
        assertEquals(Arrays.asList("failure a", "finalize a"), events);
    }

    @Test
    public void crashDuringCommitIsReportedAsFailureNotSuccess() {
        EditSession session = mock(EditSession.class);
        when(session.commit()).thenReturn(new CrashingOperation());
        submit(owner, session, new FakeOperation(1), "a");

        tick();
        tick();

        assertEquals(Arrays.asList("failure a", "finalize a"), events);
    }

    @Test
    public void workSeesTheRequestOfTheCommandThatSubmittedIt() {
        final LocalSession commandSession = mock(LocalSession.class);
        final World commandWorld = mock(World.class);
        final LocalSession laterSession = mock(LocalSession.class);
        final EditSession session = mock(EditSession.class);
        final List<Object> seen = new ArrayList<Object>();

        Request.request()
            .setSession(commandSession);
        Request.request()
            .setWorld(commandWorld);
        submit(owner, session, new Operation() {

            @Override
            public Operation resume(RunContext run) {
                seen.add(
                    Request.request()
                        .getSession());
                seen.add(
                    Request.request()
                        .getWorld());
                seen.add(
                    Request.request()
                        .getEditSession());
                return null;
            }

            @Override
            public void cancel() {}

            @Override
            public void addStatusMessages(List<String> messages) {}
        }, "a");

        Request.reset();
        Request.request()
            .setSession(laterSession);
        tick();

        assertEquals(Arrays.asList(commandSession, commandWorld, session), seen);
        assertEquals(
            laterSession,
            Request.request()
                .getSession());
        Request.reset();
    }

    private void submit(Actor actor, EditSession session, Operation operation, final String name) {
        scheduler.submit(actor, session, operation, new OperationCompletion() {

            @Override
            public void onSuccess() {
                events.add("success " + name);
            }

            @Override
            public void onFailure(WorldEditException e) {
                events.add("failure " + name);
            }
        });
        scheduler.whenFinished(session, new Runnable() {

            @Override
            public void run() {
                events.add("finalize " + name);
            }
        });
    }

    private void tick() {
        scheduler.onServerTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
    }

    private static Actor actor(UUID id) {
        Actor actor = mock(Actor.class);
        when(actor.getUniqueId()).thenReturn(id);
        return actor;
    }

    /** Fails the way a mod block misbehaving inside an edit would. */
    private static final class CrashingOperation implements Operation {

        @Override
        public Operation resume(RunContext run) {
            throw new IllegalStateException("expected by this test");
        }

        @Override
        public void cancel() {}

        @Override
        public void addStatusMessages(List<String> messages) {}
    }

    /** Uses up the budget it is given on every resume, so each tick is exactly one resume. */
    private static final class FakeOperation implements Operation {

        private int remaining;
        private int resumes;
        private boolean cancelled;

        private FakeOperation(int steps) {
            this.remaining = steps;
        }

        @Override
        public Operation resume(RunContext run) {
            while (run.shouldContinue()) {
                Thread.yield();
            }
            resumes++;
            return --remaining > 0 ? this : null;
        }

        @Override
        public void cancel() {
            cancelled = true;
        }

        @Override
        public void addStatusMessages(List<String> messages) {}
    }
}
