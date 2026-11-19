package com.sk89q.worldedit;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LocalSessionHistoryLimitTest {

    private int blocksBefore;
    private int sizeBefore;
    private LocalSession session;

    @Before
    public void setUp() {
        blocksBefore = LocalSession.MAX_HISTORY_BLOCKS;
        sizeBefore = LocalSession.MAX_HISTORY_SIZE;
        LocalSession.MAX_HISTORY_SIZE = 15;
        LocalSession.MAX_HISTORY_BLOCKS = 1000;
        session = new LocalSession();
    }

    @After
    public void tearDown() {
        LocalSession.MAX_HISTORY_BLOCKS = blocksBefore;
        LocalSession.MAX_HISTORY_SIZE = sizeBefore;
    }

    private static EditSession edit(int blocks) {
        EditSession edit = mock(EditSession.class);
        when(edit.size()).thenReturn(blocks);
        return edit;
    }

    @Test
    public void theOldestEditsAreForgottenPastTheLimit() {
        EditSession first = edit(400);
        EditSession second = edit(400);
        EditSession third = edit(400);
        session.remember(first);
        session.remember(second);
        session.remember(third);

        assertSame(third, session.stepBack());
        assertSame(second, session.stepBack());
        assertNull(session.stepBack());
    }

    @Test
    public void anEditLargerThanTheLimitIsStillKept() {
        session.remember(edit(10));
        EditSession huge = edit(5000);
        session.remember(huge);

        assertSame(huge, session.stepBack());
        assertNull(session.stepBack());
    }

    @Test
    public void noLimitKeepsEverything() {
        LocalSession.MAX_HISTORY_BLOCKS = -1;
        EditSession first = edit(5000);
        session.remember(first);
        session.remember(edit(5000));

        session.stepBack();
        assertSame(first, session.stepBack());
    }
}
