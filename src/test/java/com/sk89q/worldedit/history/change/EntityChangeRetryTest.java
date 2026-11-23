package com.sk89q.worldedit.history.change;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.history.UndoContext;
import com.sk89q.worldedit.util.Location;

public class EntityChangeRetryTest {

    private final Extent extent = mock(Extent.class);
    private final Entity spawned = mock(Entity.class);
    private final UndoContext context = new UndoContext();
    private final BaseEntity state = new BaseEntity("Pig");
    private Location location;

    @Before
    public void setUp() {
        location = new Location(extent, new Vector(0, 0, 0));
        context.setExtent(extent);
        when(extent.createEntity(any(Location.class), any(BaseEntity.class))).thenReturn(spawned);
    }

    @Test
    public void retriedUndoRestoresARemovedEntityOnce() throws Exception {
        EntityRemove change = new EntityRemove(location, state);

        change.undo(context);
        change.undo(context);

        verify(extent, times(1)).createEntity(any(Location.class), any(BaseEntity.class));
    }

    @Test
    public void undoAfterRedoRestoresTheEntityAgain() throws Exception {
        EntityRemove change = new EntityRemove(location, state);

        change.undo(context);
        change.redo(context);
        change.undo(context);

        verify(extent, times(2)).createEntity(any(Location.class), any(BaseEntity.class));
    }

    @Test
    public void retriedRedoCreatesAnEntityOnce() throws Exception {
        EntityCreate change = new EntityCreate(location, state, mock(Entity.class));

        change.undo(context);
        change.redo(context);
        change.redo(context);

        verify(extent, times(1)).createEntity(any(Location.class), any(BaseEntity.class));
    }
}
