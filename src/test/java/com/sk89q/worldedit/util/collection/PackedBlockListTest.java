package com.sk89q.worldedit.util.collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.Test;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;

public class PackedBlockListTest {

    private static final BaseBlock STONE = new BaseBlock(1);
    private static final BaseBlock DIRT = new BaseBlock(3, 2);

    @Test
    public void positionsRoundTripIncludingNegativesAndLimits() {
        Vector[] positions = { new Vector(0, 0, 0), new Vector(-1, -1, -1), new Vector(1, 255, -1),
            new Vector(-1, 0, 1), new Vector(29999999, 255, -29999999), new Vector(-29999999, 0, 29999999),
            new Vector((1 << 25) - 1, 2047, (1 << 25) - 1), new Vector(-(1 << 25), -2048, -(1 << 25)) };

        PackedBlockList list = new PackedBlockList(1);
        for (Vector position : positions) {
            assertTrue(position.toString(), PackedBlockList.fits(position));
            list.add(position, STONE);
        }

        for (int i = 0; i < positions.length; i++) {
            BlockVector stored = list.positionAt(i);
            assertEquals(positions[i].getBlockX(), stored.getBlockX());
            assertEquals(positions[i].getBlockY(), stored.getBlockY());
            assertEquals(positions[i].getBlockZ(), stored.getBlockZ());
        }
    }

    @Test
    public void positionsOutsideTheRangeDoNotFit() {
        assertFalse(PackedBlockList.fits(new Vector(1 << 25, 0, 0)));
        assertFalse(PackedBlockList.fits(new Vector(0, 0, -(1 << 25) - 1)));
        assertFalse(PackedBlockList.fits(new Vector(0, 2048, 0)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void refusesPositionsThatDoNotFit() {
        new PackedBlockList(1).add(new Vector(1 << 25, 0, 0), STONE);
    }

    @Test
    public void keepsBothColumnsAcrossSegments() {
        int count = 100000;
        PackedBlockList list = new PackedBlockList(2);
        for (int i = 0; i < count; i++) {
            list.add(new Vector(i, i & 255, -i), new BaseBlock(i % 9), new BaseBlock(i % 5 + 100, i % 16));
        }

        assertEquals(count, list.size());
        for (int i = 0; i < count; i++) {
            assertEquals(
                i,
                list.positionAt(i)
                    .getBlockX());
            assertEquals(
                -i,
                list.positionAt(i)
                    .getBlockZ());
            assertEquals(
                i % 9,
                list.blockAt(i, 0)
                    .getType());
            assertEquals(
                i % 5 + 100,
                list.blockAt(i, 1)
                    .getType());
            assertEquals(
                i % 16,
                list.blockAt(i, 1)
                    .getData());
        }
    }

    @Test
    public void blocksWithoutNbtShareAnInstance() {
        PackedBlockList list = new PackedBlockList(1);
        list.add(new Vector(0, 0, 0), new BaseBlock(1));
        list.add(new Vector(1, 0, 0), new BaseBlock(3));
        list.add(new Vector(2, 0, 0), new BaseBlock(1));

        assertSame(list.blockAt(0, 0), list.blockAt(2, 0));
    }

    @Test
    public void blocksWithNbtKeepTheirOwnData() {
        Map<String, Tag> first = new HashMap<String, Tag>();
        Map<String, Tag> second = new HashMap<String, Tag>();
        BaseBlock a = new BaseBlock(4000, 1, new CompoundTag(first));
        BaseBlock b = new BaseBlock(4000, 1, new CompoundTag(second));

        PackedBlockList list = new PackedBlockList(1);
        list.add(new Vector(0, 0, 0), a);
        list.add(new Vector(1, 0, 0), b);

        assertSame(a, list.blockAt(0, 0));
        assertSame(b, list.blockAt(1, 0));
    }

    @Test
    public void wildcardDataIsKept() {
        PackedBlockList list = new PackedBlockList(1);
        list.add(new Vector(0, 0, 0), new BaseBlock(1, 2));
        list.add(new Vector(1, 0, 0), new BaseBlock(1, -1));

        assertEquals(
            2,
            list.blockAt(0, 0)
                .getData());
        assertEquals(
            -1,
            list.blockAt(1, 0)
                .getData());
    }

    @Test
    public void drainVisitsEverythingOnceAndCanResume() {
        int count = 70000;
        PackedBlockList list = new PackedBlockList(1);
        for (int i = 0; i < count; i++) {
            list.add(new Vector(i, 0, 0), STONE);
        }

        Iterator<Map.Entry<BlockVector, BaseBlock>> first = list.drain();
        for (int i = 0; i < 40000; i++) {
            assertEquals(
                i,
                first.next()
                    .getKey()
                    .getBlockX());
        }

        Iterator<Map.Entry<BlockVector, BaseBlock>> second = list.drain();
        for (int i = 40000; i < count; i++) {
            assertEquals(
                i,
                second.next()
                    .getKey()
                    .getBlockX());
        }
        assertFalse(second.hasNext());
        assertFalse(first.hasNext());
    }

    @Test
    public void clearStartsOver() {
        PackedBlockList list = new PackedBlockList(1);
        list.add(new Vector(5, 5, 5), DIRT);
        list.drain()
            .next();

        list.clear();
        assertEquals(0, list.size());
        assertFalse(
            list.drain()
                .hasNext());

        list.add(new Vector(6, 6, 6), STONE);
        assertEquals(
            6,
            list.drain()
                .next()
                .getKey()
                .getBlockX());
    }
}
