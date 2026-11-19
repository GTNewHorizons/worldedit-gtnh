package com.sk89q.worldedit.command.brigadier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import net.minecraft.command.ICommandSender;

import org.junit.Before;
import org.junit.Test;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.session.SessionManager;
import com.sk89q.worldedit.util.operation.OperationScheduler;

/**
 * A command that edits straight away must not run while the player's earlier edits are still queued, or the
 * older edits would land on top of it.
 */
public class PendingWorkTest {

    private final UUID id = UUID.randomUUID();
    private final Player player = mock(Player.class);
    private final LocalSession session = mock(LocalSession.class);
    private final EditSession editSession = mock(EditSession.class);
    private final OperationScheduler scheduler = mock(OperationScheduler.class);
    private final WorldEdit worldEdit = mock(WorldEdit.class);
    private CommandCall call;

    @Before
    public void setUp() throws Exception {
        when(player.getUniqueId()).thenReturn(id);
        SessionManager sessions = mock(SessionManager.class);
        when(sessions.get(player)).thenReturn(session);
        when(worldEdit.getSessionManager()).thenReturn(sessions);
        when(worldEdit.getOperationScheduler()).thenReturn(scheduler);
        when(session.createEditSession(any(Player.class))).thenReturn(editSession);

        CommandDispatcher<ICommandSender> dispatcher = new CommandDispatcher<>();
        dispatcher.register(
            LiteralArgumentBuilder.<ICommandSender>literal("t")
                .executes(ctx -> {
                    call = new CommandCall(worldEdit, ctx, player);
                    return 1;
                }));
        dispatcher.execute("t", mock(ICommandSender.class));
    }

    @Test
    public void anImmediateEditIsRefusedWhileWorkIsQueued() {
        when(scheduler.hasPendingWork(id)).thenReturn(true);

        try {
            call.editSession();
            fail("expected the edit to be refused");
        } catch (CommandFailure e) {
            assertEquals(OperationScheduler.STILL_RUNNING, e.getMessage());
        }
        verify(session, never()).createEditSession(any(Player.class));
    }

    @Test
    public void aQueuedEditIsAllowedWhileWorkIsQueued() {
        when(scheduler.hasPendingWork(id)).thenReturn(true);

        assertSame(editSession, call.queuedEditSession());
    }

    @Test
    public void anImmediateEditRunsWhenNothingIsQueued() {
        assertSame(editSession, call.editSession());
    }

    @Test
    public void theCheckOnlyHappensWhenTheEditSessionIsCreated() {
        assertSame(editSession, call.editSession());
        when(scheduler.hasPendingWork(id)).thenReturn(true);

        // The command's own queued work must not lock it out of its own edit session.
        assertSame(editSession, call.editSession());
    }
}
