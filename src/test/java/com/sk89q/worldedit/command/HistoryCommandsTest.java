package com.sk89q.worldedit.command;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.util.operation.OperationScheduler;

public class HistoryCommandsTest {

    private final UUID id = UUID.randomUUID();
    private final Player player = mock(Player.class);
    private final LocalSession session = mock(LocalSession.class);
    private final EditSession editSession = mock(EditSession.class);
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

        commands.undo(player, session, editSession, new CommandContext("/undo"));

        verify(session, never()).undo(any(), any(Player.class));
        verify(player).printError(anyString());
    }

    @Test
    public void redoRefusesWhileAnEditIsRunning() throws Exception {
        when(scheduler.hasPendingWork(id)).thenReturn(true);

        commands.redo(player, session, editSession, new CommandContext("/redo"));

        verify(session, never()).redo(any(), any(Player.class));
        verify(player).printError(anyString());
    }

    @Test
    public void undoRunsWhenNothingIsRunning() throws Exception {
        commands.undo(player, session, editSession, new CommandContext("/undo"));

        verify(session).undo(any(), any(Player.class));
    }

    @Test
    public void redoRunsWhenNothingIsRunning() throws Exception {
        commands.redo(player, session, editSession, new CommandContext("/redo"));

        verify(session).redo(any(), any(Player.class));
    }
}
