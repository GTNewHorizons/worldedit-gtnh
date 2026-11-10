package com.sk89q.worldedit.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.TestEditSessions;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.forge.ForgeOperationScheduler;
import com.sk89q.worldedit.session.SessionManager;
import com.sk89q.worldedit.util.operation.OperationScheduler;
import com.sk89q.worldedit.world.World;

import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Cancelling an undo or redo must leave the history matching the world, whether or not the step had started.
 */
public class CancelledHistoryTest {

    private static final int SIDE = 40;

    private final Map<String, BaseBlock> blocks = new HashMap<String, BaseBlock>();
    private final ForgeOperationScheduler scheduler = new ForgeOperationScheduler();
    private final UUID id = UUID.randomUUID();
    private Player player;
    private World world;
    private LocalSession session;
    private HistoryCommands commands;
    private boolean writesFail;

    @Before
    public void setUp() throws Exception {
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(id);

        world = mock(World.class, withSettings().stubOnly());
        when(world.isValidBlockType(anyInt())).thenReturn(true);
        when(world.getMaxY()).thenReturn(255);
        Answer<BaseBlock> read = new Answer<BaseBlock>() {

            @Override
            public BaseBlock answer(InvocationOnMock call) {
                return get(call.<Vector>getArgument(0));
            }
        };
        when(world.getBlock(any(Vector.class))).thenAnswer(read);
        when(world.getLazyBlock(any(Vector.class))).thenAnswer(read);
        Answer<Boolean> write = new Answer<Boolean>() {

            @Override
            public Boolean answer(InvocationOnMock call) {
                if (writesFail) {
                    throw new IllegalStateException("expected by this test");
                }
                blocks.put(key(call.<Vector>getArgument(0)), call.<BaseBlock>getArgument(1));
                return true;
            }
        };
        when(world.setBlock(any(Vector.class), any(BaseBlock.class))).thenAnswer(write);
        when(world.setBlock(any(Vector.class), any(BaseBlock.class), anyBoolean())).thenAnswer(write);

        session = spy(new LocalSession());
        doAnswer(new Answer<EditSession>() {

            @Override
            public EditSession answer(InvocationOnMock call) {
                return newSession();
            }
        }).when(session)
            .createHistorySession(any(EditSession.class), any(), any(Player.class));

        WorldEdit worldEdit = mock(WorldEdit.class);
        when(worldEdit.getOperationScheduler()).thenReturn(scheduler);
        SessionManager sessions = mock(SessionManager.class);
        when(sessions.findByName("target")).thenReturn(session);
        when(sessions.findOwnerByName("target")).thenReturn(id);
        when(worldEdit.getSessionManager()).thenReturn(sessions);
        commands = new HistoryCommands(worldEdit);

        session.remember(filledEdit(1, 3));
    }

    private static String key(Vector position) {
        return position.getBlockX() + "," + position.getBlockY() + "," + position.getBlockZ();
    }

    private BaseBlock get(Vector position) {
        BaseBlock block = blocks.get(key(position));
        return block != null ? block : new BaseBlock(0);
    }

    private EditSession newSession() {
        return TestEditSessions.create(world);
    }

    private EditSession filledEdit(int before, int after) throws Exception {
        EditSession edit = newSession();
        for (int x = 0; x < SIDE; x++) {
            for (int z = 0; z < SIDE; z++) {
                for (int y = 0; y < SIDE; y++) {
                    blocks.put(x + "," + y + "," + z, new BaseBlock(before));
                    edit.setBlock(new Vector(x, y, z), new BaseBlock(after));
                }
            }
        }
        edit.flushQueue();
        return edit;
    }

    private int count(int type) {
        int found = 0;
        for (BaseBlock block : blocks.values()) {
            if (block.getType() == type) {
                found++;
            }
        }
        return found;
    }

    private void tick() {
        scheduler.onServerTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
    }

    private void runToCompletion() {
        int ticks = 0;
        while (scheduler.hasPendingWork(id)) {
            tick();
            assertTrue("the scheduler stopped making progress", ++ticks < 100000);
        }
    }

    private void tickUntilPartlyDone() {
        int ticks = 0;
        while (count(1) == 0 || count(3) == 0) {
            tick();
            assertTrue("the step never ran partly", ++ticks < 100000 && scheduler.hasPendingWork(id));
        }
    }

    @Test
    public void anUndoCancelledBeforeStartingCanStillBeUndone() throws Exception {
        commands.undo(player, session, 1, null);
        scheduler.cancel(id);

        assertEquals(SIDE * SIDE * SIDE, count(3));

        commands.undo(player, session, 1, null);
        runToCompletion();
        assertEquals(SIDE * SIDE * SIDE, count(1));
    }

    @Test
    public void anUndoCancelledPartwayCanBeFinished() throws Exception {
        commands.undo(player, session, 1, null);
        tickUntilPartlyDone();
        scheduler.cancel(id);

        verify(player).printError(contains("//undo again"));

        commands.undo(player, session, 1, null);
        runToCompletion();
        assertEquals(SIDE * SIDE * SIDE, count(1));
    }

    @Test
    public void aRedoCancelledBeforeStartingCanStillBeRedone() throws Exception {
        commands.undo(player, session, 1, null);
        runToCompletion();

        commands.redo(player, session, 1, null);
        scheduler.cancel(id);
        assertEquals(SIDE * SIDE * SIDE, count(1));

        commands.redo(player, session, 1, null);
        runToCompletion();
        assertEquals(SIDE * SIDE * SIDE, count(3));
    }

    @Test
    public void aRedoCancelledPartwayCanBeFinished() throws Exception {
        commands.undo(player, session, 1, null);
        runToCompletion();

        commands.redo(player, session, 1, null);
        tickUntilPartlyDone();
        scheduler.cancel(id);

        verify(player).printError(contains("//redo again"));

        commands.redo(player, session, 1, null);
        runToCompletion();
        assertEquals(SIDE * SIDE * SIDE, count(3));
    }

    private Player admin() {
        Player admin = mock(Player.class);
        when(admin.getUniqueId()).thenReturn(UUID.randomUUID());
        return admin;
    }

    private LocalSession adminSession() {
        return spy(new LocalSession());
    }

    @Test
    public void anotherPlayersUndoLocksTheirHistoryUntilItFinishes() throws Exception {
        Player admin = admin();
        commands.undo(admin, adminSession(), 1, "target");

        assertTrue(session.isHistoryHeld());
        commands.undo(player, session, 1, null);
        verify(player).printError(OperationScheduler.HISTORY_HELD);

        int ticks = 0;
        while (session.isHistoryHeld()) {
            tick();
            assertTrue("the scheduler stopped making progress", ++ticks < 100000);
        }

        assertFalse(session.isHistoryHeld());
        assertEquals(SIDE * SIDE * SIDE, count(1));
    }

    @Test
    public void aCancelledUndoOfAnotherPlayerReleasesTheirHistory() throws Exception {
        Player admin = admin();
        commands.undo(admin, adminSession(), 1, "target");

        scheduler.cancel(admin.getUniqueId());

        assertFalse(session.isHistoryHeld());
    }

    @Test
    public void anotherPlayersHistoryIsRefusedWhileTheyHaveEditsQueued() throws Exception {
        session.remember(filledEdit(3, 5));
        commands.undo(player, session, 1, null);

        Player admin = admin();
        commands.undo(admin, adminSession(), 1, "target");

        verify(admin).printError(contains("still running"));
        runToCompletion();
        assertEquals(SIDE * SIDE * SIDE, count(3));
    }

    @Test
    public void twoAdminsCannotUndoTheSameHistoryAtOnce() throws Exception {
        session.remember(filledEdit(3, 5));
        commands.undo(admin(), adminSession(), 1, "target");

        Player second = admin();
        commands.undo(second, adminSession(), 1, "target");

        verify(second).printError(contains("still running"));
    }

    @Test
    public void aFailedUndoCanBeRetried() throws Exception {
        writesFail = true;
        commands.undo(player, session, 1, null);
        runToCompletion();
        writesFail = false;

        verify(player).printError(contains("//undo again"));
        assertEquals(SIDE * SIDE * SIDE, count(3));

        commands.undo(player, session, 1, null);
        runToCompletion();
        assertEquals(SIDE * SIDE * SIDE, count(1));
    }

    @Test
    public void aFailedRedoCanBeRetried() throws Exception {
        commands.undo(player, session, 1, null);
        runToCompletion();

        writesFail = true;
        commands.redo(player, session, 1, null);
        runToCompletion();
        writesFail = false;

        verify(player).printError(contains("//redo again"));
        assertEquals(SIDE * SIDE * SIDE, count(1));

        commands.redo(player, session, 1, null);
        runToCompletion();
        assertEquals(SIDE * SIDE * SIDE, count(3));
    }

    @Test
    public void aFailedStepStopsTheRestOfItsBatch() throws Exception {
        session.remember(filledEdit(3, 5));

        writesFail = true;
        commands.undo(player, session, 2, null);
        runToCompletion();
        writesFail = false;
        assertEquals(SIDE * SIDE * SIDE, count(5));

        // Both steps gave their places back, so this undoes the newest edit first again.
        commands.undo(player, session, 1, null);
        runToCompletion();
        assertEquals(SIDE * SIDE * SIDE, count(3));

        commands.undo(player, session, 1, null);
        runToCompletion();
        assertEquals(SIDE * SIDE * SIDE, count(1));
    }

    @Test
    public void severalCancelledUndosAllGiveTheirPlaceBack() throws Exception {
        session.remember(filledEdit(3, 5));

        commands.undo(player, session, 2, null);
        scheduler.cancel(id);
        assertEquals(SIDE * SIDE * SIDE, count(5));

        commands.undo(player, session, 1, null);
        runToCompletion();
        assertEquals(SIDE * SIDE * SIDE, count(3));
    }
}
