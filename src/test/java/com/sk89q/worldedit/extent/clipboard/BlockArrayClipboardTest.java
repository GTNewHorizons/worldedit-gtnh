package com.sk89q.worldedit.extent.clipboard;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.sk89q.jnbt.ByteArrayTag;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.EllipsoidRegion;
import com.sk89q.worldedit.regions.RegionIntersection;

public class BlockArrayClipboardTest {

    @Test
    public void sparsePositionsAcrossAWorldSizedRegionRemainIndependent() throws Exception {
        Vector min = new Vector(-29999999, 0, -29999999);
        Vector max = new Vector(29999999, 255, 29999999);
        BlockArrayClipboard clipboard = new BlockArrayClipboard(new CuboidRegion(min, max));
        Vector[] positions = { min, min.add(15, 15, 15), min.add(16, 0, 0), min.add(0, 16, 0), min.add(0, 0, 16), max };
        for (int i = 0; i < positions.length; i++) {
            assertTrue(clipboard.setBlock(positions[i], new BaseBlock(1, i)));
        }
        clipboard.setOrigin(new Vector(11, 22, 33));
        for (int i = 0; i < positions.length; i++) {
            assertEquals(new BaseBlock(1, i), clipboard.getBlock(positions[i]));
        }
        assertEquals(new BaseBlock(0), clipboard.getBlock(BlockVector.ZERO));
        assertTrue(clipboard.setBlock(max, new BaseBlock(0)));
        assertEquals(new BaseBlock(0), clipboard.getBlock(max));
    }

    @Test
    public void preservesSelectionShapeAndFractionalPositionRounding() throws Exception {
        Vector center = new Vector(0, 30, 0);
        BlockArrayClipboard sphere = new BlockArrayClipboard(new EllipsoidRegion(center, new Vector(3, 3, 3)));
        Vector corner = center.add(3, 3, 3);
        assertFalse(sphere.setBlock(corner, new BaseBlock(1)));
        assertEquals(new BaseBlock(0), sphere.getBlock(corner));
        assertTrue(sphere.setBlock(center, new BaseBlock(2, -1)));
        assertEquals(new BaseBlock(2, -1), sphere.getLazyBlock(center));

        BlockArrayClipboard fractional = new BlockArrayClipboard(
            new CuboidRegion(new Vector(-2.4, 0, 0), new Vector(2, 1, 1)));
        fractional.setBlock(new Vector(-1.85, 0, 0), new BaseBlock(3));
        assertEquals(new BaseBlock(3), fractional.getBlock(new Vector(-1, 0, 0)));
        assertEquals(new BaseBlock(0), fractional.getBlock(new Vector(-2, 0, 0)));
    }

    @Test
    public void thinSelectionsUseSixteenSlotsPerSectionInEveryDirection() throws Exception {
        Field sections = BlockArrayClipboard.class.getDeclaredField("sections");
        sections.setAccessible(true);
        Field indices = ClipboardSection.class.getDeclaredField("indices");
        indices.setAccessible(true);
        Vector start = new Vector(-11, 17, -33);
        BaseBlock stone = new BaseBlock(1);
        BaseBlock dirt = new BaseBlock(3);
        for (Vector axis : new Vector[] { Vector.UNIT_X, Vector.UNIT_Y, Vector.UNIT_Z }) {
            BlockArrayClipboard clipboard = new BlockArrayClipboard(
                new CuboidRegion(start, start.add(axis.multiply(31))));
            for (int i = 0; i < 32; i++) {
                clipboard.setBlock(start.add(axis.multiply(i)), stone);
            }
            Map<?, ?> allocated = (Map<?, ?>) sections.get(clipboard);
            assertEquals(2, allocated.size());
            for (Object section : allocated.values()) {
                assertNull(indices.get(section));
            }
            clipboard.setBlock(start, dirt);
            clipboard.setBlock(start.add(axis.multiply(31)), dirt);
            for (Object section : allocated.values()) {
                assertEquals(16, ((byte[]) indices.get(section)).length);
            }
            for (int i = 0; i < 32; i++) {
                assertEquals(i == 0 || i == 31 ? dirt : stone, clipboard.getBlock(start.add(axis.multiply(i))));
            }
        }
    }

    @Test
    public void positionsRemainRelativeToAMovingChildRegion() throws Exception {
        CuboidRegion child = new CuboidRegion(Vector.ZERO, Vector.UNIT_X);
        BlockArrayClipboard clipboard = new BlockArrayClipboard(new RegionIntersection(child));
        BaseBlock stone = new BaseBlock(1);
        BaseBlock dirt = new BaseBlock(3);
        clipboard.setBlock(Vector.ZERO, stone);
        clipboard.setBlock(Vector.UNIT_X, dirt);
        child.shift(Vector.UNIT_X);
        assertEquals(Vector.UNIT_X, clipboard.getMinimumPoint());
        assertEquals(stone, clipboard.getBlock(Vector.UNIT_X));
        assertEquals(dirt, clipboard.getBlock(new Vector(2, 0, 0)));
        clipboard.setBlock(new Vector(2, 0, 0), stone);
        child.shift(new Vector(-1, 0, 0));
        assertEquals(stone, clipboard.getBlock(Vector.UNIT_X));
    }

    @Test
    public void tagAndBlockMutationsCannotChangeOtherSnapshots() throws Exception {
        BlockArrayClipboard clipboard = new BlockArrayClipboard(new CuboidRegion(BlockVector.ZERO, BlockVector.ONE));
        byte[] data = { 42 };
        Map<String, Tag> fields = new HashMap<>();
        fields.put("cells", new ListTag(ByteArrayTag.class, Collections.singletonList(new ByteArrayTag(data))));
        BaseBlock source = new BaseBlock(54, 2, new CompoundTag(fields));
        clipboard.setBlock(BlockVector.ZERO, source);
        clipboard.setBlock(BlockVector.ONE, source);
        source.setId(1);
        source.setNbtData(null);
        data[0] = -1;
        fields.clear();

        BaseBlock read = clipboard.getBlock(BlockVector.ZERO);
        assertEquals(54, read.getId());
        assertArrayEquals(new byte[] { 42 }, cells(read));
        cells(read)[0] = 0;
        read.setData(0);
        assertArrayEquals(new byte[] { 42 }, cells(clipboard.getBlock(BlockVector.ZERO)));
        assertArrayEquals(new byte[] { 42 }, cells(clipboard.getBlock(BlockVector.ONE)));
        assertEquals(
            2,
            clipboard.getBlock(BlockVector.ZERO)
                .getData());
        clipboard.setBlock(BlockVector.ZERO, new BaseBlock(54, 2));
        assertNull(
            clipboard.getBlock(BlockVector.ZERO)
                .getNbtData());
        assertArrayEquals(new byte[] { 42 }, cells(clipboard.getBlock(BlockVector.ONE)));
    }

    private static byte[] cells(BaseBlock block) {
        ListTag list = (ListTag) block.getNbtData()
            .getValue()
            .get("cells");
        return list.getByteArray(0);
    }

    @Test
    public void fullWidthStatesSurvivePaletteGrowthAndUniformRewrites() {
        ClipboardSection section = new ClipboardSection(BlockVector.ZERO, 4096);
        long[] expected = new long[4096];
        long state = (123456L << 32) | 65534;
        for (int i = 0; i < expected.length; i++) {
            // Begin with a full uniform section and change it to well over 256 distinct states.
            section.set(i, state, null);
        }
        for (int i = 0; i < expected.length; i++) {
            int index = (i * 8191) & 4095;
            assertEquals(state, section.stateAt(index));
            expected[index] = (123456L + i) << 32 | 0xffffffffL;
            section.set(index, expected[index], null);
        }
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], section.stateAt(i));
            section.set(i, 0, null);
        }
        assertTrue(section.isEmpty());
        section.set(4095, 0, new CompoundTag(Collections.<String, Tag>emptyMap()));
        assertFalse(section.isEmpty());
        section.set(4095, 0, null);
        assertTrue(section.isEmpty());
    }
}
