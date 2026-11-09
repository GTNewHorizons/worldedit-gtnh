package com.sk89q.worldedit.function.operation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class BackgroundOperationTest {

    @Test
    public void waitingDoesNotSpendTheRestOfTheBudget() throws Exception {
        final CountDownLatch release = new CountDownLatch(1);
        BackgroundOperation<String> operation = new BackgroundOperation<String>(() -> {
            release.await(10, TimeUnit.SECONDS);
            return "done";
        });

        TimeBudgetRunContext run = new TimeBudgetRunContext(TimeUnit.SECONDS.toNanos(5));
        long start = System.nanoTime();
        assertSame(operation, operation.resume(run));

        assertTrue(
            "it spun instead of waiting for the next tick",
            System.nanoTime() - start < TimeUnit.SECONDS.toNanos(1));
        assertEquals(false, run.shouldContinue());

        release.countDown();
        Operation next = operation;
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (next != null) {
            assertTrue(System.nanoTime() < deadline);
            next = next.resume(new TimeBudgetRunContext(TimeUnit.MILLISECONDS.toNanos(20)));
            Thread.sleep(5);
        }
        assertEquals("done", operation.getResult());
    }

    @Test
    public void aContextThatCannotWaitBlocksUntilDone() throws Exception {
        BackgroundOperation<String> operation = new BackgroundOperation<String>(() -> {
            Thread.sleep(50);
            return "done";
        });

        assertNull(operation.resume(new RunContext()));
        assertEquals("done", operation.getResult());
    }

    @Test
    public void aFailureIsReportedWithItsMessage() throws Exception {
        BackgroundOperation<String> operation = new BackgroundOperation<String>(
            () -> { throw new IOException("no such schematic"); });

        try {
            Operations.complete(operation);
            fail("expected a failure");
        } catch (BackgroundOperation.BackgroundWorkFailedException e) {
            assertEquals("no such schematic", e.getMessage());
        }
    }

    @Test
    public void theWorkRunsOffTheCallingThread() throws Exception {
        final Thread caller = Thread.currentThread();
        BackgroundOperation<Thread> operation = new BackgroundOperation<Thread>(Thread::currentThread);

        Operations.complete(operation);

        assertTrue(operation.getResult() != caller);
    }
}
