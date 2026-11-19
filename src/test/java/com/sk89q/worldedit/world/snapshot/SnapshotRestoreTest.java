package com.sk89q.worldedit.world.snapshot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.operation.TimeBudgetRunContext;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.chunk.Chunk;
import com.sk89q.worldedit.world.storage.ChunkStore;
import com.sk89q.worldedit.world.storage.MissingChunkException;

public class SnapshotRestoreTest {

    private final Map<String, Integer> written = new HashMap<String, Integer>();
    private final ChunkStore store = mock(ChunkStore.class);
    private final EditSession editSession = mock(EditSession.class);
    private Chunk five;
    private final CuboidRegion region = new CuboidRegion(new Vector(0, 0, 0), new Vector(31, 1, 15));

    @Before
    public void setUp() throws Exception {
        five = chunkOf(5);
        when(editSession.getWorld()).thenReturn(mock(World.class));
        when(editSession.setBlock(any(Vector.class), any(BaseBlock.class))).thenAnswer(new Answer<Boolean>() {

            @Override
            public Boolean answer(InvocationOnMock call) {
                Vector pos = call.getArgument(0);
                written.put(
                    pos.getBlockX() + "," + pos.getBlockY() + "," + pos.getBlockZ(),
                    call.<BaseBlock>getArgument(1)
                        .getType());
                return true;
            }
        });
    }

    private static Chunk chunkOf(final int type) throws Exception {
        Chunk chunk = mock(Chunk.class);
        when(chunk.getBlock(any(Vector.class))).thenReturn(new BaseBlock(type));
        return chunk;
    }

    @Test
    public void restoresEveryBlockOfTheRegionFromItsChunk() throws Exception {
        Chunk first = chunkOf(5);
        Chunk second = chunkOf(7);
        when(store.getChunk(eq(new Vector2D(0, 0)), any(World.class))).thenReturn(first);
        when(store.getChunk(eq(new Vector2D(1, 0)), any(World.class))).thenReturn(second);

        SnapshotRestore restore = new SnapshotRestore(store, editSession, region);
        Operations.complete(restore);

        assertEquals(32 * 2 * 16, written.size());
        assertEquals(Integer.valueOf(5), written.get("3,1,4"));
        assertEquals(Integer.valueOf(7), written.get("20,0,9"));
        assertFalse(restore.hadTotalFailure());
    }

    @Test
    public void aChunkIsOneStepSoTheRestoreCanPauseBetweenChunks() throws Exception {
        when(store.getChunk(any(Vector2D.class), any(World.class))).thenReturn(five);
        SnapshotRestore restore = new SnapshotRestore(store, editSession, region);

        Operation next = restore.resume(new TimeBudgetRunContext(0));

        assertNotNull(next);
        assertEquals(16 * 2 * 16, written.size());
        assertNull(next.resume(new TimeBudgetRunContext(0)));
        assertEquals(32 * 2 * 16, written.size());
    }

    @Test
    public void missingChunksAreCounted() throws Exception {
        when(store.getChunk(eq(new Vector2D(0, 0)), any(World.class))).thenReturn(five);
        when(store.getChunk(eq(new Vector2D(1, 0)), any(World.class))).thenThrow(new MissingChunkException());

        SnapshotRestore restore = new SnapshotRestore(store, editSession, region);
        Operations.complete(restore);

        assertEquals(
            1,
            restore.getMissingChunks()
                .size());
        assertEquals(16 * 2 * 16, written.size());
        assertFalse(restore.hadTotalFailure());
    }

    @Test
    public void laterChangesToTheSelectionDoNotMatter() throws Exception {
        when(store.getChunk(any(Vector2D.class), any(World.class))).thenReturn(five);
        SnapshotRestore restore = new SnapshotRestore(store, editSession, region);

        region.setPos2(new Vector(2, 0, 2));
        Operations.complete(restore);

        assertEquals(32 * 2 * 16, written.size());
        assertTrue(
            restore.getErrorChunks()
                .isEmpty());
    }
}
