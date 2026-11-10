package com.sk89q.worldedit.extent.world;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.world.World;

public class FastModeExtentTest {

    private final World world = mock(World.class);
    private final FastModeExtent extent = new FastModeExtent(world, true);

    @Test
    public void aSecondCommitDoesNotRepeatTheFixUp() throws Exception {
        extent.setBlock(new Vector(1, 1, 1), new BaseBlock(1));

        Operations.complete(extent.commit());
        Operations.complete(extent.commit());

        verify(world, times(1)).fixAfterFastMode(any());
    }

    @Test
    public void writesAfterACommitAreTrackedAgainInTheSameChunk() throws Exception {
        extent.setBlock(new Vector(1, 1, 1), new BaseBlock(1));
        Operations.complete(extent.commit());

        extent.setBlock(new Vector(2, 1, 1), new BaseBlock(1));
        Operations.complete(extent.commit());

        verify(world, times(2)).fixAfterFastMode(any());
    }
}
