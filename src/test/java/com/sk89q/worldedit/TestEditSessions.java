package com.sk89q.worldedit;

import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.util.eventbus.EventBus;
import com.sk89q.worldedit.world.World;

/** Lets tests in other packages build a real edit session, whose constructor is package-private. */
public final class TestEditSessions {

    private TestEditSessions() {}

    /**
     * @return a new edit session with no block limit and the reorder queue enabled, like a command's
     */
    public static EditSession create(World world) {
        EditSession editSession = new EditSession(
            new EventBus(),
            world,
            -1,
            null,
            new EditSessionEvent(world, null, -1, null));
        editSession.enableQueue();
        return editSession;
    }
}
