package com.sk89q.worldedit.extent.world;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.World;

public class BlockQuirkExtentTest {

    private static final Vector POSITION = new Vector(1, 2, 3);
    private static final int MODDED_MACHINE = 4000;

    private final Extent extent = mock(Extent.class);
    private final World world = mock(World.class);
    private final BlockQuirkExtent quirks = new BlockQuirkExtent(extent, world);

    private void existing(BaseBlock block) {
        when(extent.getLazyBlock(POSITION)).thenReturn(block);
    }

    @Test
    public void emptiesModdedBlocksBeforeReplacingThem() throws Exception {
        existing(new BaseBlock(MODDED_MACHINE, 1));

        quirks.setBlock(POSITION, new BaseBlock(BlockID.AIR));

        verify(world).clearContainerBlockContents(POSITION);
    }

    @Test
    public void emptiesWhenOnlyTheDataChanges() throws Exception {
        existing(new BaseBlock(MODDED_MACHINE, 1));

        quirks.setBlock(POSITION, new BaseBlock(MODDED_MACHINE, 2));

        verify(world).clearContainerBlockContents(POSITION);
    }

    /** The chunk ignores a write of the same block, so emptying it would just lose its contents. */
    @Test
    public void keepsContentsWhenTheBlockIsNotChanging() throws Exception {
        existing(new BaseBlock(MODDED_MACHINE, 1));

        quirks.setBlock(POSITION, new BaseBlock(MODDED_MACHINE, 1));

        verify(world, never()).clearContainerBlockContents(any(Vector.class));
    }

    @Test
    public void airHasNothingToEmpty() throws Exception {
        existing(new BaseBlock(BlockID.AIR));

        quirks.setBlock(POSITION, new BaseBlock(MODDED_MACHINE));

        verify(world, never()).clearContainerBlockContents(any(Vector.class));
    }

    @Test
    public void iceIsStillRemovedFirst() throws Exception {
        existing(new BaseBlock(BlockID.ICE));

        quirks.setBlock(POSITION, new BaseBlock(BlockID.STONE));

        verify(world).setBlock(POSITION, new BaseBlock(BlockID.AIR));
    }
}
