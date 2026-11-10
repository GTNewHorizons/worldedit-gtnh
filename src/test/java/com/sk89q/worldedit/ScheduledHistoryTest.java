package com.sk89q.worldedit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.command.ClipboardCommands;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.transform.BlockTransformHook;
import com.sk89q.worldedit.forge.ForgeOperationScheduler;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.operation.RunContext;
import com.sk89q.worldedit.function.operation.TimeBudgetRunContext;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.eventbus.EventBus;
import com.sk89q.worldedit.util.operation.OperationCompletion;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.registry.BlockRegistry;
import com.sk89q.worldedit.world.registry.WorldData;

import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Runs real edit sessions through the real tick scheduler against an in-memory world, so undo, redo
 * and paste have to survive being paused and picked up again.
 */
public class ScheduledHistoryTest {

    private static final int SIDE = 40;
    private static final BaseBlock AIR = new BaseBlock(0);

    private final Map<String, BaseBlock> blocks = new HashMap<String, BaseBlock>();
    private final ForgeOperationScheduler scheduler = new ForgeOperationScheduler();
    private final UUID ownerId = UUID.randomUUID();
    private Player player;
    private World world;
    private int completions;

    @Before
    public void setUp() throws Exception {
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(ownerId);

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
                blocks.put(key(call.<Vector>getArgument(0)), call.<BaseBlock>getArgument(1));
                return true;
            }
        };
        when(world.setBlock(any(Vector.class), any(BaseBlock.class))).thenAnswer(write);
        when(world.setBlock(any(Vector.class), any(BaseBlock.class), anyBoolean())).thenAnswer(write);

        WorldData worldData = mock(WorldData.class);
        when(worldData.getBlockRegistry()).thenReturn(mock(BlockRegistry.class));
        BlockTransformHook hook = mock(BlockTransformHook.class);
        when(hook.transformBlock(any(), any())).thenAnswer(returnsFirstArg());
        when(worldData.getBlockTransformHook()).thenReturn(hook);
        when(world.getWorldData()).thenReturn(worldData);
    }

    private static String key(Vector position) {
        return position.getBlockX() + "," + position.getBlockY() + "," + position.getBlockZ();
    }

    private BaseBlock get(Vector position) {
        BaseBlock block = blocks.get(key(position));
        return block != null ? block : AIR;
    }

    private EditSession newSession() {
        EditSession session = new EditSession(
            new EventBus(),
            world,
            -1,
            null,
            new EditSessionEvent(world, null, -1, null));
        session.enableQueue();
        return session;
    }

    private EditSession filledEdit(int before, int after) throws Exception {
        for (int x = 0; x < SIDE; x++) {
            for (int z = 0; z < SIDE; z++) {
                for (int y = 0; y < SIDE; y++) {
                    blocks.put(x + "," + y + "," + z, new BaseBlock(before));
                }
            }
        }
        EditSession edit = newSession();
        for (int x = 0; x < SIDE; x++) {
            for (int z = 0; z < SIDE; z++) {
                for (int y = 0; y < SIDE; y++) {
                    edit.setBlock(new Vector(x, y, z), new BaseBlock(after));
                }
            }
        }
        edit.flushQueue();
        return edit;
    }

    private void assertFilledWith(int type) {
        for (int x = 0; x < SIDE; x += 7) {
            for (int z = 0; z < SIDE; z += 7) {
                for (int y = 0; y < SIDE; y += 7) {
                    assertEquals(type, get(new Vector(x, y, z)).getType());
                }
            }
        }
    }

    private int runToCompletion() {
        int ticks = 0;
        while (scheduler.hasPendingWork(ownerId)) {
            scheduler.onServerTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
            assertTrue("the scheduler stopped making progress", ++ticks < 100000);
        }
        return ticks;
    }

    private void submit(EditSession writer, Operation operation) {
        scheduler.submit(player, writer, operation, new OperationCompletion() {

            @Override
            public void onSuccess() {
                completions++;
            }

            @Override
            public void onFailure(WorldEditException e) {
                throw new AssertionError(e);
            }
        });
    }

    @Test
    public void anImmediateCommandsBlocksLandOverLaterTicks() throws Exception {
        EditSession edit = newSession();
        for (int x = 0; x < SIDE; x++) {
            for (int z = 0; z < SIDE; z++) {
                for (int y = 0; y < SIDE; y++) {
                    edit.setBlock(new Vector(x, y, z), new BaseBlock(7));
                }
            }
        }
        final int[] finalized = { 0 };

        scheduler.whenFinished(player, edit, new Runnable() {

            @Override
            public void run() {
                finalized[0]++;
            }
        });

        assertEquals(0, get(new Vector(0, 0, 0)).getType());
        assertEquals(0, finalized[0]);

        runToCompletion();

        assertFilledWith(7);
        assertEquals(1, finalized[0]);
        assertFalse(edit.hasQueuedBlocks());
    }

    @Test
    public void undoAndRedoResumeUntilEverythingIsBack() throws Exception {
        EditSession edit = filledEdit(1, 3);
        assertFilledWith(3);

        EditSession undoWriter = newSession();
        submit(undoWriter, edit.undoOperation(undoWriter));
        assertFilledWith(3);
        runToCompletion();
        assertFilledWith(1);

        EditSession redoWriter = newSession();
        submit(redoWriter, edit.redoOperation(redoWriter));
        runToCompletion();
        assertFilledWith(3);

        assertEquals(2, completions);
    }

    @Test
    public void undoCanStopPartWayAndCarryOn() throws Exception {
        EditSession edit = filledEdit(1, 3);
        EditSession writer = newSession();

        Operation operation = edit.undoOperation(writer);
        int resumes = 0;
        while (operation != null) {
            RunContext expired = new TimeBudgetRunContext(0);
            operation = operation.resume(expired);
            resumes++;
        }
        writer.flushQueue();

        assertTrue("expected the undo to pause, resumes=" + resumes, resumes > 1);
        assertFilledWith(1);
    }

    @Test
    public void historySessionsWriteNothingUntilCommitted() throws Exception {
        EditSession edit = filledEdit(1, 3);
        EditSession writer = newSession();

        Operations.complete(edit.undoOperation(writer));

        assertFilledWith(3);
        writer.flushQueue();
        assertFilledWith(1);
    }

    @Test
    public void pasteHappensOnTheScheduler() throws Exception {
        CuboidRegion region = new CuboidRegion(new Vector(0, 0, 0), new Vector(1, 1, 1));
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        clipboard.setOrigin(new Vector(0, 0, 0));
        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = 0; z <= 1; z++) {
                    clipboard.setBlock(new Vector(x, y, z), new BaseBlock(5));
                }
            }
        }

        LocalSession session = mock(LocalSession.class);
        ClipboardHolder holder = new ClipboardHolder(clipboard, world.getWorldData());
        when(session.getClipboard()).thenReturn(holder);
        when(session.getPlacementPosition(player)).thenReturn(new Vector(10, 0, 10));
        when(player.getWorld()).thenReturn(world);

        WorldEdit worldEdit = mock(WorldEdit.class);
        when(worldEdit.getOperationScheduler()).thenReturn(scheduler);

        EditSession writer = newSession();
        ClipboardCommands.paste(worldEdit, player, session, writer, false, false, true);

        assertNotEquals(5, get(new Vector(10, 0, 10)).getType());
        verify(player, never()).print(anyString());
        verify(session, never()).setRegionSelector(any(World.class), any());
        assertTrue(scheduler.hasPendingWork(ownerId));

        runToCompletion();

        assertEquals(5, get(new Vector(10, 0, 10)).getType());
        assertEquals(5, get(new Vector(11, 1, 11)).getType());
        assertEquals(8, writer.size());
        verify(player).print(anyString());
        verify(session).setRegionSelector(any(World.class), any());
        assertFalse(scheduler.hasPendingWork(ownerId));
    }
}
