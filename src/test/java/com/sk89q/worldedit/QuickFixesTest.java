package com.sk89q.worldedit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.reorder.MultiStageReorder;
import com.sk89q.worldedit.extent.world.FastModeExtent;
import com.sk89q.worldedit.function.LayerFunction;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.visitor.LayerVisitor;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.World;

public class QuickFixesTest {

    private static final BaseBlock STONE = new BaseBlock(1);

    @Test
    public void disabledReorderDoesNotReadTheWorld() throws Exception {
        Extent extent = mock(Extent.class);

        new MultiStageReorder(extent, false).setBlock(new Vector(1, 2, 3), STONE);

        verify(extent, never()).getLazyBlock(any(Vector.class));
        verify(extent).setBlock(any(Vector.class), any(BaseBlock.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void fastModeTracksEveryChunkItTouches() throws Exception {
        World world = mock(World.class);
        FastModeExtent extent = new FastModeExtent(world, true);

        extent.setBlock(new Vector(1, 0, 1), STONE);
        extent.setBlock(new Vector(2, 0, 2), STONE);
        extent.setBlock(new Vector(16, 0, 1), STONE);
        extent.setBlock(new Vector(3, 0, 3), STONE);
        Operations.complete(extent.commit());

        ArgumentCaptor<Iterable<BlockVector2D>> chunks = ArgumentCaptor.forClass(Iterable.class);
        verify(world).fixAfterFastMode(chunks.capture());
        Set<BlockVector2D> dirty = new HashSet<BlockVector2D>();
        for (BlockVector2D chunk : chunks.getValue()) {
            dirty.add(chunk);
        }
        assertEquals(2, dirty.size());
        verify(world, times(4)).setBlock(any(Vector.class), any(BaseBlock.class), eq(false));
    }

    @Test
    public void blockTypeLookupsStillWorkAndIgnoreModdedIds() {
        assertTrue(BlockType.shouldPlaceLast(BlockID.TORCH));
        assertTrue(BlockType.shouldPlaceFinal(BlockID.SIGN_POST));
        assertTrue(BlockType.isContainerBlock(BlockID.CHEST));
        assertFalse(BlockType.shouldPlaceLast(BlockID.STONE));
        assertFalse(BlockType.isContainerBlock(5000));
        assertFalse(BlockType.shouldPlaceFinal(16000000));
    }

    @Test
    public void blocksWithDifferentDataHashApart() {
        assertNotEquals(new BaseBlock(1, 8).hashCode(), new BaseBlock(2, 0).hashCode());
        assertNotEquals(new BaseBlock(1, 1).hashCode(), new BaseBlock(1, 9).hashCode());
        assertEquals(new BaseBlock(4, 3).hashCode(), new BaseBlock(4, 3).hashCode());
    }

    @Test
    public void coveredColumnDoesNotStopTheOthers() throws Exception {
        final List<Vector> visited = new ArrayList<Vector>();
        CuboidRegion region = new CuboidRegion(new Vector(0, 0, 0), new Vector(1, 2, 1));

        LayerFunction function = new LayerFunction() {

            @Override
            public boolean isGround(Vector position) {
                boolean coveredColumn = position.getBlockX() == 0 && position.getBlockZ() == 0;
                return coveredColumn ? position.getBlockY() == 3 : position.getBlockY() == 2;
            }

            @Override
            public boolean apply(Vector position, int depth) {
                visited.add(position);
                return false;
            }
        };

        Operations.complete(new LayerVisitor(region, 0, 2, function));

        assertEquals(3, visited.size());
    }
}
