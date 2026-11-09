package com.sk89q.worldedit.command;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.util.operation.OperationScheduler;

public class HistoryCommandsTest {

    private final UUID id = UUID.randomUUID();
    private final Player player = mock(Player.class);
    private final LocalSession session = mock(LocalSession.class);
    private final OperationScheduler scheduler = mock(OperationScheduler.class);
    private final WorldEdit worldEdit = mock(WorldEdit.class);
    private final HistoryCommands commands = new HistoryCommands(worldEdit);

    @Before
    public void setUp() {
        when(player.getUniqueId()).thenReturn(id);
        when(worldEdit.getOperationScheduler()).thenReturn(scheduler);
    }

    @Test
    public void undoRefusesWhileAnEditIsRunning() throws Exception {
        when(scheduler.hasPendingWork(id)).thenReturn(true);

        commands.undo(player, session, 1, null);

        verify(session, never()).stepBack();
        verify(player).printError(anyString());
    }

    @Test
    public void redoRefusesWhileAnEditIsRunning() throws Exception {
        when(scheduler.hasPendingWork(id)).thenReturn(true);

        commands.redo(player, session, 1, null);

        verify(session, never()).stepForward();
        verify(player).printError(anyString());
    }

    @Test
    public void undoIsHandedToTheScheduler() throws Exception {
        EditSession undone = mock(EditSession.class);
        EditSession writer = mock(EditSession.class);
        Operation operation = mock(Operation.class);
        when(session.stepBack()).thenReturn(undone);
        when(session.createHistorySession(eq(undone), any(), eq(player))).thenReturn(writer);
        when(undone.undoOperation(writer)).thenReturn(operation);

        commands.undo(player, session, 1, null);

        verify(scheduler).submit(eq(player), eq(writer), eq(operation), any());
        verify(scheduler).whenFinished(eq(writer), any());
        verify(player, never()).print(anyString());
    }

    @Test
    public void redoIsHandedToTheScheduler() throws Exception {
        EditSession redone = mock(EditSession.class);
        EditSession writer = mock(EditSession.class);
        Operation operation = mock(Operation.class);
        when(session.stepForward()).thenReturn(redone);
        when(session.createHistorySession(eq(redone), any(), eq(player))).thenReturn(writer);
        when(redone.redoOperation(writer)).thenReturn(operation);

        commands.redo(player, session, 1, null);

        verify(scheduler).submit(eq(player), eq(writer), eq(operation), any());
        verify(player, never()).print(anyString());
    }

    @Test
    public void reportsWhenThereIsNothingToUndo() throws Exception {
        commands.undo(player, session, 1, null);

        verify(player).printError("Nothing left to undo.");
        verify(scheduler, never()).submit(any(), any(), any(), any());
    }
}
