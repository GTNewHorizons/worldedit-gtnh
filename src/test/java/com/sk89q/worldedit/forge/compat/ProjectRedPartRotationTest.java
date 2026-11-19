package com.sk89q.worldedit.forge.compat;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.sk89q.jnbt.ByteTag;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.math.transform.AffineTransform;

import codechicken.lib.vec.Rotation;

public class ProjectRedPartRotationTest {

    private static final int DOWN = 0, UP = 1, NORTH = 2, SOUTH = 3, WEST = 4, EAST = 5;

    /** What //rotate 90 builds. */
    private static final AffineTransform ROTATE_90 = new AffineTransform().rotateY(-90);

    private static CompoundTag wire(int side, int connMap) {
        Map<String, Tag> tags = new HashMap<String, Tag>();
        tags.put("id", new StringTag("pr_insulated"));
        tags.put("side", new ByteTag((byte) side));
        tags.put("connMap", new IntTag(connMap));
        return new CompoundTag(tags);
    }

    private static CompoundTag gate(int side, int rotation) {
        Map<String, Tag> tags = new HashMap<String, Tag>();
        tags.put("id", new StringTag("pr_sgate"));
        tags.put("orient", new ByteTag((byte) (side << 2 | rotation)));
        return new CompoundTag(tags);
    }

    private static int front(CompoundTag gate) {
        int orient = gate.getByte("orient") & 0xFF;
        return Rotation.rotateSide(orient >> 2, ((orient & 3) + 2) % 4);
    }

    @Test
    public void rotatingNinetyDegreesTurnsSidesTheWayBlocksTurn() {
        int[] sides = ForgeMultipartExistsCompat.sideMap(ROTATE_90);

        // Clockwise seen from above, as //rotate 90 turns stairs and other blocks.
        assertArrayEquals(new int[] { DOWN, UP, EAST, WEST, NORTH, SOUTH }, sides);
    }

    @Test
    public void aWireOnAWallMovesToTheTurnedWallAndRecalculatesItsConnections() {
        int[] sides = ForgeMultipartExistsCompat.sideMap(ROTATE_90);

        CompoundTag turned = ForgeMultipartExistsCompat.transformFacePart(wire(SOUTH, 127008), sides);

        assertEquals(WEST, turned.getByte("side"));
        assertEquals(127008 | 0x80000000, turned.getInt("connMap"));
    }

    @Test
    public void aGateKeepsItsSideAndTurnsItsFront() {
        int[] sides = ForgeMultipartExistsCompat.sideMap(ROTATE_90);

        for (int side = 0; side < 6; side++) {
            for (int rotation = 0; rotation < 4; rotation++) {
                CompoundTag before = gate(side, rotation);
                CompoundTag after = ForgeMultipartExistsCompat.transformFacePart(before, sides);

                assertEquals(sides[side], (after.getByte("orient") & 0xFF) >> 2);
                assertEquals(sides[front(before)], front(after));
            }
        }
    }

    @Test
    public void aFlipMirrorsWallsAcrossTheAxis() {
        int[] sides = ForgeMultipartExistsCompat.sideMap(new AffineTransform().scale(-1, 1, 1));

        assertEquals(
            WEST,
            ForgeMultipartExistsCompat.transformFacePart(wire(EAST, 0), sides)
                .getByte("side"));
        assertEquals(
            NORTH,
            ForgeMultipartExistsCompat.transformFacePart(wire(NORTH, 0), sides)
                .getByte("side"));
    }

    @Test
    public void aFramedWireRecalculatesItsConnections() {
        Map<String, Tag> tags = new HashMap<String, Tag>();
        tags.put("id", new StringTag("pr_fbundled"));
        tags.put("connMap", new IntTag(258060));
        CompoundTag framed = new CompoundTag(tags);

        assertEquals(
            258060 | 0x80000000,
            ForgeMultipartExistsCompat.transformFacePart(framed, ForgeMultipartExistsCompat.sideMap(ROTATE_90))
                .getInt("connMap"));
    }

    @Test
    public void partsWithoutAFaceOrConnectionsAreLeftAlone() {
        Map<String, Tag> tags = new HashMap<String, Tag>();
        tags.put("id", new StringTag("pr_lamp"));
        CompoundTag framed = new CompoundTag(tags);

        assertSame(
            framed,
            ForgeMultipartExistsCompat.transformFacePart(framed, ForgeMultipartExistsCompat.sideMap(ROTATE_90)));
    }
}
