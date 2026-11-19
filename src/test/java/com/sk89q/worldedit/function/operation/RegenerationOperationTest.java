package com.sk89q.worldedit.function.operation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;

public class RegenerationOperationTest {

    private static final class Recording extends RegenerationOperation {

        private final List<String> calls = new ArrayList<String>();
        private String failOn;
        private Operation commit;

        private Recording(Vector2D... chunks) {
            super(Arrays.asList(chunks));
        }

        private void record(String call) {
            calls.add(call);
            if (call.equals(failOn)) {
                throw new IllegalStateException("expected by this test");
            }
        }

        @Override
        protected void replaceChunk(int x, int z) {
            record("replace " + x + "," + z);
        }

        @Override
        protected void decorateChunk(int x, int z) {
            record("decorate " + x + "," + z);
        }

        @Override
        protected void decorateNeighbour(int x, int z) {
            record("neighbour " + x + "," + z);
        }

        @Override
        protected void restoreChunk(int x, int z) {
            record("restore " + x + "," + z);
        }

        @Override
        protected Operation commitEdits() {
            calls.add("commit");
            return commit;
        }

        @Override
        protected void resendChunk(int x, int z) {
            record("resend " + x + "," + z);
        }

        private boolean replaced(int x, int z) {
            return isReplaced(x, z);
        }
    }

    private static Vector2D chunk(int x, int z) {
        return new Vector2D(x, z);
    }

    private static void runAll(Operation operation) throws WorldEditException {
        Operations.complete(operation);
    }

    @Test
    public void everyChunkIsReplacedBeforeAnythingIsDecorated() throws Exception {
        Recording regen = new Recording(chunk(0, 0), chunk(1, 0));

        runAll(regen);

        assertEquals(
            Arrays.asList(
                "replace 0,0",
                "replace 1,0",
                "decorate 0,0",
                "decorate 1,0",
                "neighbour 0,-1",
                "neighbour -1,-1",
                "neighbour -1,0",
                "neighbour 1,-1",
                "restore 0,0",
                "restore 1,0",
                "commit",
                "resend 0,0",
                "resend 1,0"),
            sorted(regen.calls));
    }

    /** Neighbour passes come out in a fixed but uninteresting order, so compare them as a set. */
    private static List<String> sorted(List<String> calls) {
        List<String> copy = new ArrayList<String>(calls);
        int first = -1;
        int last = -1;
        for (int i = 0; i < copy.size(); i++) {
            if (copy.get(i)
                .startsWith("neighbour")) {
                if (first < 0) {
                    first = i;
                }
                last = i;
            }
        }
        assertTrue(first >= 0);
        List<String> expected = new ArrayList<String>(
            Arrays.asList("neighbour 0,-1", "neighbour -1,-1", "neighbour -1,0", "neighbour 1,-1"));
        assertEquals(expected.size(), last - first + 1);
        assertTrue(
            copy.subList(first, last + 1)
                .containsAll(expected));
        for (int i = 0; i < expected.size(); i++) {
            copy.set(first + i, expected.get(i));
        }
        return copy;
    }

    @Test
    public void aChunkInTheSelectionIsNeverRunAsANeighbour() throws Exception {
        Recording regen = new Recording(chunk(0, 0), chunk(1, 0), chunk(0, 1), chunk(1, 1));

        runAll(regen);

        for (String call : regen.calls) {
            assertFalse(call, call.equals("neighbour 0,0") || call.equals("neighbour 1,0"));
            assertFalse(call, call.equals("neighbour 0,1") || call.equals("neighbour 1,1"));
        }
        assertEquals(1, Collections.frequency(regen.calls, "neighbour -1,-1"));
    }

    @Test
    public void onlyOneChunkStepRunsWhenTheBudgetIsGone() throws Exception {
        Recording regen = new Recording(chunk(0, 0), chunk(1, 0));

        Operation next = regen.resume(new TimeBudgetRunContext(0));

        assertNotNull(next);
        assertEquals(Arrays.asList("replace 0,0"), regen.calls);
    }

    @Test
    public void aFreshBudgetIsCheckedAfterEveryStepNotEvery256() throws Exception {
        Recording regen = new Recording(chunk(0, 0), chunk(1, 0), chunk(2, 0));

        int resumes = 0;
        Operation next = regen;
        while (next != null) {
            next = next.resume(new TimeBudgetRunContext(0));
            resumes++;
        }

        assertTrue("resumes=" + resumes, resumes >= regen.calls.size());
    }

    @Test
    public void aWrittenChunkCountsAsReplacedForLaterSteps() throws Exception {
        Recording regen = new Recording(chunk(4, 4));
        assertFalse(regen.replaced(4, 4));

        regen.resume(new TimeBudgetRunContext(0));

        assertTrue(regen.replaced(4, 4));
        assertFalse(regen.replaced(-4, -4));
        assertFalse(regen.replaced(4, -4));
    }

    @Test
    public void commitIsResumedAcrossSteps() throws Exception {
        Recording regen = new Recording(chunk(0, 0));
        final int[] resumes = { 0 };
        regen.commit = new Operation() {

            @Override
            public Operation resume(RunContext run) {
                return ++resumes[0] < 3 ? this : null;
            }

            @Override
            public void cancel() {}

            @Override
            public void addStatusMessages(List<String> messages) {}
        };

        runAll(regen);

        assertEquals(3, resumes[0]);
        assertTrue(regen.calls.indexOf("resend 0,0") > regen.calls.indexOf("commit"));
    }

    @Test
    public void aFailedReplacementFinishesWhatWasReplacedAndReportsIt() throws Exception {
        Recording regen = new Recording(chunk(0, 0), chunk(1, 0), chunk(2, 0));
        regen.failOn = "replace 1,0";

        try {
            runAll(regen);
            fail("expected a failure");
        } catch (RegenerationOperation.RegenerationFailedException e) {
            assertNotNull(e.getMessage());
        }

        assertFalse(regen.calls.contains("replace 2,0"));
        assertFalse(regen.calls.contains("decorate 1,0"));
        assertTrue(regen.calls.contains("decorate 0,0"));
        assertTrue(regen.calls.contains("restore 0,0"));
        assertTrue(regen.calls.contains("resend 0,0"));
    }

    @Test
    public void aFailedDecorationStillRestoresEverything() throws Exception {
        Recording regen = new Recording(chunk(0, 0), chunk(1, 0));
        regen.failOn = "decorate 0,0";

        try {
            runAll(regen);
            fail("expected a failure");
        } catch (RegenerationOperation.RegenerationFailedException expected) {}

        assertTrue(regen.calls.contains("decorate 1,0"));
        assertTrue(regen.calls.contains("restore 0,0"));
        assertTrue(regen.calls.contains("restore 1,0"));
    }

    @Test
    public void cancelFinishesOnlyWhatWasAlreadyReplaced() throws Exception {
        Recording regen = new Recording(chunk(0, 0), chunk(5, 5));
        assertNotNull(regen.resume(new TimeBudgetRunContext(0)));

        regen.cancel();

        assertFalse(regen.calls.contains("replace 5,5"));
        assertFalse(regen.calls.contains("decorate 5,5"));
        assertTrue(regen.calls.contains("decorate 0,0"));
        assertTrue(regen.calls.contains("neighbour -1,-1"));
        assertTrue(regen.calls.contains("restore 0,0"));
        assertTrue(regen.calls.contains("resend 0,0"));
    }

    @Test
    public void cancelBeforeStartingTouchesNothing() {
        Recording regen = new Recording(chunk(0, 0));

        regen.cancel();

        assertEquals(Arrays.asList("commit"), regen.calls);
    }

    @Test
    public void reachingTheEndReturnsNull() throws Exception {
        Recording regen = new Recording(chunk(0, 0));

        assertNull(regen.resume(new RunContext()));
    }

    @Test
    public void budgetIsCheckedOnEveryLargeStepCall() throws InterruptedException {
        TimeBudgetRunContext expired = new TimeBudgetRunContext(0);
        assertFalse(expired.shouldContinueAfterLargeStep());
        assertFalse(expired.shouldContinue());

        TimeBudgetRunContext plenty = new TimeBudgetRunContext(3600000000000L);
        assertTrue(plenty.shouldContinueAfterLargeStep());
        assertTrue(plenty.shouldContinue());

        assertTrue(new RunContext().shouldContinueAfterLargeStep());
    }
}
