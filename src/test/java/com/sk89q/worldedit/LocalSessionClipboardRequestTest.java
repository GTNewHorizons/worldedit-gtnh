package com.sk89q.worldedit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LocalSessionClipboardRequestTest {

    private final LocalSession session = new LocalSession();

    @Test
    public void onlyTheLatestOfTwoLoadsCountsAndKeepsTheClipboardLoading() {
        int first = session.nextClipboardRequest();
        session.startLoadingClipboard(first);
        int second = session.nextClipboardRequest();
        session.startLoadingClipboard(second);

        session.finishLoadingClipboard(first);
        assertFalse(session.isLatestClipboardRequest(first));
        assertTrue(session.isLoadingClipboard());

        session.finishLoadingClipboard(second);
        assertTrue(session.isLatestClipboardRequest(second));
        assertFalse(session.isLoadingClipboard());
    }

    @Test
    public void aCopyAfterALoadIsWhatTheClipboardWaitsFor() {
        int load = session.nextClipboardRequest();
        session.startLoadingClipboard(load);
        session.nextClipboardRequest();

        assertFalse(session.isLoadingClipboard());
        assertFalse(session.isLatestClipboardRequest(load));
    }
}
