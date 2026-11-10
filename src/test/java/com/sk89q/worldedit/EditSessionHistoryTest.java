package com.sk89q.worldedit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.TimeBudgetRunContext;
import com.sk89q.worldedit.util.eventbus.EventBus;
import com.sk89q.worldedit.world.World;

/**
 * Runs edits through a real {@link EditSession}, including its reorder queue and history, against an
 * in-memory world, and checks what undo and redo leave behind.
 */
public class EditSessionHistoryTest {

    private static final BaseBlock AIR = new BaseBlock(BlockID.AIR);
    private static final BaseBlock STONE = new BaseBlock(BlockID.STONE);
    private static final BaseBlock DIRT = new BaseBlock(BlockID.DIRT);
    private static final BaseBlock TORCH = new BaseBlock(BlockID.TORCH, 5);

    private final Map<String, BaseBlock> blocks = new HashMap<String, BaseBlock>();
    private int writes;
    private int drops;
    /** A position whose next write changes the block and then fails, as a mod's breakBlock can. */
    private String failsAfterWriting;
    private final Map<String, String> attachedTo = new HashMap<String, String>();
    private World world;

    @Before
    public void setUp() throws Exception {
        // Stub only, or the mock would remember every call made during the big test.
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
                writes++;
                Vector at = call.<Vector>getArgument(0);
                String written = key(at);
                BaseBlock block = call.<BaseBlock>getArgument(1);
                BaseBlock replaced = blocks.put(written, block);
                if (written.equals(failsAfterWriting)) {
                    failsAfterWriting = null;
                    throw new IllegalStateException("mod block misbehaved");
                }

                // Like vanilla, a door's top half goes away as soon as its bottom half does.
                if (replaced != null && isDoor(replaced) && (replaced.getData() & 8) == 0 && !isDoor(block)) {
                    String above = key(at.add(0, 1, 0));
                    BaseBlock top = blocks.get(above);
                    if (top != null && isDoor(top) && (top.getData() & 8) != 0) {
                        blocks.remove(above);
                    }
                }

                // Anything still attached to a block that just went away pops off.
                String attached = attachedTo.get(written);
                if (block.getType() == BlockID.AIR && attached != null) {
                    BaseBlock held = blocks.get(attached);
                    if (held != null && held.getType() != BlockID.AIR && held.getType() != BlockID.LONG_GRASS) {
                        drops++;
                        blocks.remove(attached);
                    }
                }
                return true;
            }
        };
        when(world.setBlock(any(Vector.class), any(BaseBlock.class))).thenAnswer(write);
        when(world.setBlock(any(Vector.class), any(BaseBlock.class), anyBoolean())).thenAnswer(write);
    }

    private static boolean isDoor(BaseBlock block) {
        return block.getType() == BlockID.WOODEN_DOOR || block.getType() == BlockID.IRON_DOOR;
    }

    private static String key(Vector position) {
        return position.getBlockX() + "," + position.getBlockY() + "," + position.getBlockZ();
    }

    private BaseBlock get(Vector position) {
        BaseBlock block = blocks.get(key(position));
        return block != null ? block : AIR;
    }

    private void put(Vector position, BaseBlock block) {
        blocks.put(key(position), block);
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

    private void undo(EditSession edit) {
        EditSession session = newSession();
        edit.undo(session);
        session.flushQueue();
    }

    private void redo(EditSession edit) {
        EditSession session = newSession();
        edit.redo(session);
        session.flushQueue();
    }

    private static BaseBlock machine(String contents) {
        Map<String, Tag> nbt = new HashMap<String, Tag>();
        nbt.put("contents", new StringTag(contents));
        return new BaseBlock(4000, 3, new CompoundTag(nbt));
    }

    private static void assertSameBlock(BaseBlock expected, BaseBlock actual) {
        assertEquals(expected.getType(), actual.getType());
        assertEquals(expected.getData(), actual.getData());
        assertEquals(nbtOf(expected), nbtOf(actual));
    }

    private static Map<String, Tag> nbtOf(BaseBlock block) {
        return block.getNbtData() != null ? block.getNbtData()
            .getValue() : Collections.<String, Tag>emptyMap();
    }

    @Test
    public void undoAndRedoRestoreEveryKindOfBlock() throws Exception {
        List<Vector> positions = Arrays
            .asList(new Vector(0, 0, 0), new Vector(1, 64, -1), new Vector(-5, 255, 7), new Vector(2, 70, 2));
        BaseBlock[] before = { STONE, machine("before"), TORCH, DIRT };
        BaseBlock[] after = { TORCH, AIR, machine("after"), new BaseBlock(BlockID.SIGN_POST, 2) };
        for (int i = 0; i < before.length; i++) {
            put(positions.get(i), before[i]);
        }

        EditSession edit = newSession();
        for (int i = 0; i < after.length; i++) {
            edit.setBlock(positions.get(i), after[i]);
        }
        edit.flushQueue();

        assertEquals(after.length, edit.size());
        for (int i = 0; i < after.length; i++) {
            assertSameBlock(after[i], get(positions.get(i)));
        }

        undo(edit);
        for (int i = 0; i < before.length; i++) {
            assertSameBlock(before[i], get(positions.get(i)));
        }

        redo(edit);
        for (int i = 0; i < after.length; i++) {
            assertSameBlock(after[i], get(positions.get(i)));
        }
    }

    @Test
    public void repeatedWritesUndoToTheOriginalAndRedoToTheLast() throws Exception {
        Vector position = new Vector(3, 3, 3);
        put(position, DIRT);

        EditSession first = newSession();
        first.setBlock(position, STONE);
        first.flushQueue();
        first.setBlock(position, new BaseBlock(BlockID.GLASS));
        first.flushQueue();

        assertEquals(2, first.size());
        undo(first);
        assertSameBlock(DIRT, get(position));
        redo(first);
        assertSameBlock(new BaseBlock(BlockID.GLASS), get(position));
    }

    @Test
    public void lastWriteInOneQueueWins() throws Exception {
        Vector position = new Vector(9, 9, 9);

        EditSession edit = newSession();
        edit.setBlock(position, STONE);
        edit.setBlock(position, DIRT);
        edit.flushQueue();

        assertSameBlock(DIRT, get(position));
    }

    @Test
    public void doorsAreWrittenAndUndone() throws Exception {
        Vector lower = new Vector(0, 10, 0);
        Vector upper = new Vector(0, 11, 0);

        EditSession edit = newSession();
        edit.setBlock(upper, new BaseBlock(BlockID.WOODEN_DOOR, 8));
        edit.setBlock(lower, new BaseBlock(BlockID.WOODEN_DOOR, 0));
        edit.flushQueue();

        assertEquals(BlockID.WOODEN_DOOR, get(lower).getType());
        assertEquals(8, get(upper).getData());

        undo(edit);
        assertEquals(BlockID.AIR, get(lower).getType());
        assertEquals(BlockID.AIR, get(upper).getType());
    }

    @Test
    public void manyDoorsAndSignsArePlacedAcrossPauses() throws Exception {
        EditSession edit = newSession();
        for (int x = 0; x < 300; x++) {
            edit.setBlock(new Vector(x, 11, 0), new BaseBlock(BlockID.WOODEN_DOOR, 8));
            edit.setBlock(new Vector(x, 10, 0), new BaseBlock(BlockID.WOODEN_DOOR, 0));
            edit.setBlock(new Vector(x, 10, 2), new BaseBlock(BlockID.SIGN_POST, 4));
        }

        Operation next = edit.commit();
        int resumes = 0;
        while (next != null) {
            next = next.resume(new TimeBudgetRunContext(0));
            resumes++;
        }

        assertTrue("the pass never paused", resumes > 3);
        for (int x = 0; x < 300; x++) {
            assertEquals(0, get(new Vector(x, 10, 0)).getData());
            assertEquals(BlockID.WOODEN_DOOR, get(new Vector(x, 10, 0)).getType());
            assertEquals(8, get(new Vector(x, 11, 0)).getData());
            assertEquals(BlockID.SIGN_POST, get(new Vector(x, 10, 2)).getType());
        }
    }

    @Test
    public void extremeCoordinatesSurvive() throws Exception {
        List<Vector> positions = Arrays.asList(
            new Vector(29999999, 0, -29999999),
            new Vector(-29999999, 255, 29999999),
            new Vector(-1, 0, -1),
            new Vector(40000000, 5, -40000000));

        EditSession edit = newSession();
        for (Vector position : positions) {
            put(position, DIRT);
            edit.setBlock(position, STONE);
        }
        edit.flushQueue();
        for (Vector position : positions) {
            assertSameBlock(STONE, get(position));
        }

        undo(edit);
        for (Vector position : positions) {
            assertSameBlock(DIRT, get(position));
        }
    }

    @Test
    public void manyBlocksCrossingStorageBoundariesRoundTrip() throws Exception {
        int count = 200000;
        EditSession edit = newSession();
        for (int i = 0; i < count; i++) {
            Vector position = new Vector(i % 1000, i / 1000 % 256, i / 256000);
            put(position, new BaseBlock(1 + i % 50));
            edit.setBlock(position, new BaseBlock(100 + i % 7));
        }
        edit.flushQueue();

        assertEquals(count, edit.size());
        assertEquals(100, get(new Vector(0, 0, 0)).getType());

        undo(edit);
        for (int i = 0; i < count; i += 977) {
            Vector position = new Vector(i % 1000, i / 1000 % 256, i / 256000);
            assertEquals(1 + i % 50, get(position).getType());
        }
    }

    @Test
    public void flushingTwiceWritesOnce() throws Exception {
        EditSession edit = newSession();
        edit.setBlock(new Vector(1, 1, 1), STONE);
        edit.flushQueue();
        int afterFirst = writes;

        edit.flushQueue();

        assertEquals(afterFirst, writes);
        assertTrue(afterFirst > 0);
    }

    private void assertUndoKeepsAttachedBlock(BaseBlock restoredUnderIt, BaseBlock attached) throws Exception {
        Vector wall = new Vector(0, 64, 0);
        Vector spot = new Vector(1, 64, 0);
        attachedTo.put(key(wall), key(spot));
        put(spot, restoredUnderIt);

        EditSession edit = newSession();
        edit.setBlock(wall, STONE);
        edit.setBlock(spot, attached);
        edit.flushQueue();
        assertEquals(0, drops);

        undo(edit);

        assertEquals(0, drops);
        assertSameBlock(AIR, get(wall));
        assertSameBlock(restoredUnderIt, get(spot));
    }

    @Test
    public void undoRemovesATorchBeforeItsWallWhenAirIsRestored() throws Exception {
        assertUndoKeepsAttachedBlock(AIR, TORCH);
    }

    @Test
    public void undoRemovesATorchBeforeItsWallWhenGrassIsRestored() throws Exception {
        assertUndoKeepsAttachedBlock(new BaseBlock(BlockID.LONG_GRASS, 1), TORCH);
    }

    @Test
    public void undoRemovesATorchBeforeItsWallWhenASignIsRestored() throws Exception {
        assertUndoKeepsAttachedBlock(new BaseBlock(BlockID.SIGN_POST, 1), TORCH);
    }

    @Test
    public void undoRemovesASignBeforeItsWall() throws Exception {
        assertUndoKeepsAttachedBlock(AIR, new BaseBlock(BlockID.WALL_SIGN, 2));
    }

    @Test
    public void clearingADoorKeepsBothHalvesInHistory() throws Exception {
        Vector lower = new Vector(5, 64, 5);
        Vector upper = new Vector(5, 65, 5);
        BaseBlock bottom = new BaseBlock(BlockID.WOODEN_DOOR, 1);
        BaseBlock top = new BaseBlock(BlockID.WOODEN_DOOR, 8);
        put(lower, bottom);
        put(upper, top);

        EditSession edit = newSession();
        edit.setBlock(lower, AIR);
        edit.setBlock(upper, AIR);
        edit.flushQueue();

        assertSameBlock(AIR, get(lower));
        assertSameBlock(AIR, get(upper));

        undo(edit);

        assertSameBlock(bottom, get(lower));
        assertSameBlock(top, get(upper));
    }

    @Test
    public void nothingIsWrittenBeforeTheEditIsCommitted() throws Exception {
        Vector torch = new Vector(2, 64, 2);
        put(torch, TORCH);

        EditSession edit = newSession();
        edit.setBlock(torch, STONE);

        assertEquals(0, writes);
        assertSameBlock(TORCH, get(torch));

        edit.flushQueue();
        assertSameBlock(STONE, get(torch));
    }

    @Test
    public void rewritingAnAttachedBlockDoesNotRemoveItFirst() throws Exception {
        Vector spot = new Vector(1, 64, 0);
        put(spot, TORCH);

        EditSession edit = newSession();
        edit.setBlock(spot, TORCH);
        int before = writes;
        edit.flushQueue();

        assertEquals(TORCH.getType(), get(spot).getType());
        assertEquals(before + 1, writes);
    }

    @Test
    public void discardingTheQueueDropsWhatWasWaiting() throws Exception {
        EditSession edit = newSession();
        edit.setBlock(new Vector(1, 1, 1), STONE);
        edit.discardQueue();
        edit.flushQueue();

        assertEquals(0, writes);
        // Nothing was written, so there is nothing to undo either.
        assertEquals(0, edit.size());
    }

    @Test
    public void aCancelledEditOnlyRemembersWhatItWrote() throws Exception {
        EditSession edit = newSession();
        for (int x = 0; x < 1000; x++) {
            put(new Vector(x, 10, 0), DIRT);
            edit.setBlock(new Vector(x, 10, 0), STONE);
        }

        Operation commit = edit.commit();
        while (get(new Vector(0, 10, 0)).getType() != BlockID.STONE) {
            commit = commit.resume(new TimeBudgetRunContext(0));
        }
        edit.discardQueue();

        int written = 0;
        for (int x = 0; x < 1000; x++) {
            if (get(new Vector(x, 10, 0)).getType() == BlockID.STONE) {
                written++;
            }
        }
        assertTrue("written " + written, written > 0 && written < 1000);
        assertEquals(written, edit.size());

        undo(edit);
        for (int x = 0; x < 1000; x++) {
            assertSameBlock(DIRT, get(new Vector(x, 10, 0)));
        }
    }

    @Test
    public void aTorchClearedBeforeTheEditStoppedStillComesBack() throws Exception {
        Vector torch = new Vector(1, 64, 0);
        put(new Vector(0, 64, 0), STONE);
        put(torch, TORCH);

        EditSession edit = newSession();
        edit.setBlock(torch, new BaseBlock(BlockID.GLASS));
        Operation commit = edit.commit();
        while (get(torch).getType() == BlockID.TORCH) {
            commit = commit.resume(new TimeBudgetRunContext(0));
        }
        edit.discardQueue();

        if (get(torch).getType() == BlockID.AIR) {
            assertEquals(1, edit.size());
        }
        undo(edit);
        assertSameBlock(TORCH, get(torch));
    }

    @Test
    public void aWriteThatChangesTheBlockAndThenFailsCanBeUndone() throws Exception {
        Vector position = new Vector(1, 10, 0);
        put(position, DIRT);

        EditSession edit = newSession();
        edit.setBlock(position, STONE);
        failsAfterWriting = key(position);
        flushExpectingFailure(edit);
        edit.discardQueue();

        assertSameBlock(STONE, get(position));
        undo(edit);
        assertSameBlock(DIRT, get(position));
    }

    @Test
    public void aTorchWhoseClearingFailsStillComesBack() throws Exception {
        Vector torch = new Vector(1, 64, 0);
        put(new Vector(0, 64, 0), STONE);
        put(torch, TORCH);

        EditSession edit = newSession();
        edit.setBlock(torch, new BaseBlock(BlockID.GLASS));
        failsAfterWriting = key(torch);
        flushExpectingFailure(edit);
        edit.discardQueue();

        assertSameBlock(AIR, get(torch));
        undo(edit);
        assertSameBlock(TORCH, get(torch));
    }

    private static void flushExpectingFailure(EditSession edit) {
        try {
            edit.flushQueue();
        } catch (RuntimeException expected) {
            return;
        }
        throw new AssertionError("the write should have failed");
    }

    @Test
    public void tileEntityDataIsKeptByReference() throws Exception {
        Vector position = new Vector(4, 4, 4);
        BaseBlock original = machine("mine");
        put(position, original);

        EditSession edit = newSession();
        edit.setBlock(position, AIR);
        edit.flushQueue();
        undo(edit);

        assertSame(original.getNbtData(), get(position).getNbtData());
    }
}
