package com.sk89q.worldedit.forge;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import org.junit.Test;
import org.mockito.MockedStatic;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;

public class ForgeFastWriteTest {

    @Test
    public void aBreakHookReplacingTheTargetIsNotAnUnchangedWrite() throws Exception {
        World nativeWorld = mock(World.class);
        Chunk chunk = mock(Chunk.class);
        when(nativeWorld.getChunkFromChunkCoords(0, 0)).thenReturn(chunk);
        Block target = mock(Block.class);
        Block substitute = mock(Block.class);
        Vector position = new Vector(0, 64, 0);
        BaseBlock desired = new BaseBlock(1, 4);
        ForgeWorld world = new ForgeWorld(nativeWorld);
        try (MockedStatic<Block> registry = mockStatic(Block.class)) {
            registry.when(() -> Block.getBlockById(1))
                .thenReturn(target);
            // Chunk's write reports false both after the hook substitutes a different block and
            // when the requested block and metadata already occupy the position.
            when(chunk.getBlock(0, 64, 0)).thenReturn(substitute, target);
            when(chunk.getBlockMetadata(0, 64, 0)).thenReturn(4);
            assertFalse(world.setBlock(position, desired, false));
            assertFalse(world.isKnownUnchangedAfterFailedFastWrite(position, desired));
            assertFalse(world.setBlock(position, desired, false));
            assertTrue(world.isKnownUnchangedAfterFailedFastWrite(position, desired));
        }
    }
}
