package com.sk89q.worldedit.regions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.Test;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.operation.RunContext;
import com.sk89q.worldedit.function.visitor.RegionVisitor;

public class ChunkOrderedTraversalTest {

    @Test
    public void eachChunkIsReadOnceEvenWhenTheVisitorYieldsInsideIt() throws Exception {
        CuboidRegion region = new CuboidRegion(new Vector(16, 18, 16), new Vector(-17, 15, -17));
        List<Vector> positions = new ArrayList<>();
        RegionVisitor visitor = new RegionVisitor(region, pt -> {
            assertTrue(region.contains(pt));
            positions.add(pt);
            return pt.getBlockY() == 16;
        }, true);
        RunContext singleBlock = new RunContext() {

            @Override
            public boolean shouldContinue() {
                return false;
            }
        };
        Operation remaining = visitor;
        int slices = 0;
        while (remaining != null) {
            int before = positions.size();
            remaining = remaining.resume(singleBlock);
            assertTrue(positions.size() - before <= 1);
            assertTrue(++slices <= 34 * 34 * 4 + 1);
        }
        assertEquals(34 * 34 * 4, positions.size());
        assertEquals(positions.size(), new HashSet<>(positions).size());
        assertEquals(34 * 34, visitor.getAffected());

        Set<BlockVector2D> chunks = new HashSet<>();
        BlockVector2D lastChunk = null;
        for (Vector pt : positions) {
            BlockVector2D chunk = new BlockVector2D(pt.getBlockX() >> 4, pt.getBlockZ() >> 4);
            if (!chunk.equals(lastChunk)) {
                assertTrue("revisited " + chunk, chunks.add(chunk));
                lastChunk = chunk;
            }
        }
        assertEquals(16, chunks.size());
    }

    @Test
    public void bothEndsOfTheIntegerRangeCanBeVisitedAndExhausted() {
        for (int coordinate : new int[] { Integer.MIN_VALUE, Integer.MAX_VALUE - 1 }) {
            CuboidRegion region = new CuboidRegion(
                new Vector(coordinate, 0, coordinate),
                new Vector(coordinate + 1, 0, coordinate + 1));
            Iterator<BlockVector> positions = region.chunkOrderedIterator();
            List<BlockVector> expected = Arrays.asList(
                new BlockVector(coordinate, 0, coordinate),
                new BlockVector(coordinate + 1, 0, coordinate),
                new BlockVector(coordinate, 0, coordinate + 1),
                new BlockVector(coordinate + 1, 0, coordinate + 1));
            for (BlockVector block : expected) {
                assertTrue(positions.hasNext());
                assertEquals(block, positions.next());
            }
            assertFalse(positions.hasNext());
            assertThrows(NoSuchElementException.class, positions::next);
        }
    }

    @Test
    public void customRegionIteratorsStillDefineWhichBlocksAreVisited() throws Exception {
        BlockVector only = new BlockVector(16, 0, 0);
        CuboidRegion custom = new CuboidRegion(new Vector(0, 0, 0), only) {

            @Override
            public Iterator<BlockVector> iterator() {
                return Collections.singleton(only)
                    .iterator();
            }
        };
        RegionVisitor visitor = new RegionVisitor(custom, pt -> {
            assertEquals(only, pt);
            return true;
        }, true);
        Operations.complete(visitor);
        assertEquals(1, visitor.getAffected());
    }
}
