package com.sk89q.worldedit.extent.world;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        when(world.setBlock(any(), any(), eq(false))).thenReturn(true);
        extent.setBlock(new Vector(1, 1, 1), new BaseBlock(1));

        Operations.complete(extent.commit());
        Operations.complete(extent.commit());

        verify(world, times(1)).fixAfterFastMode(any());
    }

    @Test
    public void writesAfterACommitAreTrackedAgainInTheSameChunk() throws Exception {
        when(world.setBlock(any(), any(), eq(false))).thenReturn(true);
        extent.setBlock(new Vector(1, 1, 1), new BaseBlock(1));
        Operations.complete(extent.commit());

        extent.setBlock(new Vector(2, 1, 1), new BaseBlock(1));
        Operations.complete(extent.commit());

        verify(world, times(2)).fixAfterFastMode(any());
    }

    @Test
    public void unchangedChunksNeedNoRelightOrClientRefresh() throws Exception {
        when(world.isKnownUnchangedAfterFailedFastWrite(any(), any())).thenReturn(true);
        extent.setBlock(new Vector(1, 1, 1), new BaseBlock(1));
        Operations.complete(extent.commit());
        verify(world, never()).fixAfterFastMode(any());
    }

    @Test
    public void unconfirmedFalseWritesStillRefreshTheChunk() throws Exception {
        when(world.isKnownUnchangedAfterFailedFastWrite(any(), any())).thenCallRealMethod();
        org.junit.Assert.assertFalse(extent.setBlock(new Vector(1, 1, 1), new BaseBlock(1)));
        Operations.complete(extent.commit());
        verify(world).fixAfterFastMode(any());
    }

    @Test
    public void failedWritesCanStillNeedRelighting() throws Exception {
        when(world.setBlock(any(), any(), eq(false))).thenThrow(new IllegalStateException());
        org.junit.Assert
            .assertThrows(IllegalStateException.class, () -> extent.setBlock(new Vector(1, 1, 1), new BaseBlock(1)));
        Operations.complete(extent.commit());
        verify(world).fixAfterFastMode(any());
    }
}
