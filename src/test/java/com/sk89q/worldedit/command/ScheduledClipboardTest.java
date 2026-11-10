package com.sk89q.worldedit.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doReturn;
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
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.transform.BlockTransformHook;
import com.sk89q.worldedit.forge.ForgeOperationScheduler;
import com.sk89q.worldedit.function.pattern.BlockPattern;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.registry.BlockRegistry;
import com.sk89q.worldedit.world.registry.WorldData;

import cpw.mods.fml.common.gameevent.TickEvent;

/** //copy, //cut and //paste through the real tick scheduler against an in-memory world. */
public class ScheduledClipboardTest {

    private static final int SIDE = 30;

    private final Map<String, BaseBlock> blocks = new HashMap<String, BaseBlock>();
    private final ForgeOperationScheduler scheduler = new ForgeOperationScheduler();
    private final UUID id = UUID.randomUUID();
    private final CuboidRegion selection = new CuboidRegion(
        new Vector(0, 0, 0),
        new Vector(SIDE - 1, SIDE - 1, SIDE - 1));
    private Player player;
    private World world;
    private LocalSession session;
    private WorldEdit worldEdit;

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
        when(player.getWorld()).thenReturn(world);

        session = spy(new LocalSession());
        doReturn(new Vector(0, 0, 0)).when(session)
            .getPlacementPosition(player);

        worldEdit = mock(WorldEdit.class);
        when(worldEdit.getOperationScheduler()).thenReturn(scheduler);

        for (int x = 0; x < SIDE; x++) {
            for (int y = 0; y < SIDE; y++) {
                for (int z = 0; z < SIDE; z++) {
                    blocks.put(x + "," + y + "," + z, new BaseBlock(1 + (x + y + z) % 4));
                }
            }
        }
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

    private void runToCompletion() {
        int ticks = 0;
        while (scheduler.hasPendingWork(id)) {
            scheduler.onServerTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
            assertTrue("the scheduler stopped making progress", ++ticks < 100000);
        }
    }

    private void assertCopyAt(int offsetX) {
        for (int x = 0; x < SIDE; x += 5) {
            for (int y = 0; y < SIDE; y += 5) {
                for (int z = 0; z < SIDE; z += 5) {
                    assertEquals(1 + (x + y + z) % 4, get(new Vector(x + offsetX, y, z)).getType());
                }
            }
        }
    }

    @Test
    public void aPasteQueuedRightAfterACopyPastesTheNewContents() throws Exception {
        ClipboardCommands.copy(worldEdit, player, session, newSession(), selection, false, null);

        // Moving the selection while the copy waits must not change what gets copied.
        selection.setPos2(new Vector(5, 5, 5));
        doReturn(new Vector(100, 0, 0)).when(session)
            .getPlacementPosition(player);
        ClipboardCommands.paste(worldEdit, player, session, newSession(), false, false, false);

        runToCompletion();

        assertCopyAt(100);
    }

    @Test
    public void aCutRemovesOverTicksAndThePasteGetsEverything() throws Exception {
        ClipboardCommands
            .cut(worldEdit, player, session, newSession(), selection, new BlockPattern(new BaseBlock(0)), false, null);
        doReturn(new Vector(200, 0, 0)).when(session)
            .getPlacementPosition(player);
        ClipboardCommands.paste(worldEdit, player, session, newSession(), false, false, false);

        runToCompletion();

        assertCopyAt(200);
        assertEquals(0, get(new Vector(3, 3, 3)).getType());
    }

    @Test
    public void aPasteWaitsForASchematicStillLoading() throws Exception {
        session.startLoadingClipboard(session.nextClipboardRequest());

        ClipboardCommands.paste(worldEdit, player, session, newSession(), false, false, false);

        verify(player).printError(contains("still loading"));
        assertFalse(scheduler.hasPendingWork(id));
    }

    @Test
    public void aCopyAfterALoadDoesNotWaitForTheLoad() throws Exception {
        session.startLoadingClipboard(session.nextClipboardRequest());

        ClipboardCommands.copy(worldEdit, player, session, newSession(), selection, false, null);
        doReturn(new Vector(100, 0, 0)).when(session)
            .getPlacementPosition(player);
        ClipboardCommands.paste(worldEdit, player, session, newSession(), false, false, false);
        runToCompletion();

        assertCopyAt(100);
    }

    @Test
    public void cancellingTwoQueuedCopiesKeepsTheClipboardFromBefore() throws Exception {
        ClipboardHolder previous = new ClipboardHolder(
            new BlockArrayClipboard(selection.clone()),
            world.getWorldData());
        session.setClipboard(previous);

        ClipboardCommands.copy(worldEdit, player, session, newSession(), selection, false, null);
        ClipboardCommands.copy(worldEdit, player, session, newSession(), selection, false, null);
        scheduler.cancel(id);

        assertSame(previous, session.getClipboard());
    }

    @Test
    public void pasteSelectsWhereARotatedClipboardWent() throws Exception {
        CuboidRegion small = new CuboidRegion(new Vector(0, 0, 0), new Vector(4, 1, 2));
        ClipboardCommands.copy(worldEdit, player, session, newSession(), small, false, null);
        runToCompletion();
        session.getClipboard()
            .setTransform(new AffineTransform().rotateY(90));
        doReturn(new Vector(100, 0, 100)).when(session)
            .getPlacementPosition(player);

        ClipboardCommands.paste(worldEdit, player, session, newSession(), false, false, true);
        runToCompletion();

        Region selected = session.getRegionSelector(world)
            .getRegion();
        assertEquals(5 * 2 * 3, selected.getArea());
        assertEquals(3, selected.getWidth());
        assertEquals(5, selected.getLength());
    }

    @Test
    public void aCancelledCopyPutsThePreviousClipboardBack() throws Exception {
        ClipboardHolder previous = new ClipboardHolder(
            new BlockArrayClipboard(selection.clone()),
            world.getWorldData());
        session.setClipboard(previous);

        ClipboardCommands.copy(worldEdit, player, session, newSession(), selection, false, null);
        scheduler.cancel(id);

        assertSame(previous, session.getClipboard());
    }
}
