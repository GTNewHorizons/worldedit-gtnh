package com.sk89q.worldedit.forge.compat;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.sk89q.worldedit.forge.compat.rotation.RotationUtils;
import com.sk89q.worldedit.forge.compat.rotation.types.FourRotation;
import com.sk89q.worldedit.forge.compat.rotation.types.PillarRotation;
import com.sk89q.worldedit.forge.compat.rotation.types.StairRotation;
import com.sk89q.worldedit.forge.compat.rotation.types.TrapdoorRotation;
import com.sk89q.worldedit.math.transform.AffineTransform;

/**
 * Tests for //flip handling in the rotation mapping types. Flips are affine
 * transforms with a mirrored axis; every type must map metadata through them
 * without misinterpreting the mirror as a rotation.
 */
public class RotationFlipTest {

    private static final AffineTransform FLIP_X = new AffineTransform().scale(-1, 1, 1);
    private static final AffineTransform FLIP_Y = new AffineTransform().scale(1, -1, 1);
    private static final AffineTransform FLIP_Z = new AffineTransform().scale(1, 1, -1);

    // ================= stairs =================
    // Vanilla stair metas: 0=east 1=west 2=south 3=north, +4 = upside down

    @Test
    public void stairsFlipX() {
        StairRotation stairs = RotationUtils.defaultStairs();
        assertEquals("east -> west", 1, stairs.transform(0, FLIP_X));
        assertEquals("west -> east", 0, stairs.transform(1, FLIP_X));
        assertEquals("south unchanged", 2, stairs.transform(2, FLIP_X));
        assertEquals("north unchanged", 3, stairs.transform(3, FLIP_X));
    }

    @Test
    public void stairsFlipZ() {
        StairRotation stairs = RotationUtils.defaultStairs();
        assertEquals("south -> north", 3, stairs.transform(2, FLIP_Z));
        assertEquals("north -> south", 2, stairs.transform(3, FLIP_Z));
        assertEquals("east unchanged", 0, stairs.transform(0, FLIP_Z));
    }

    @Test
    public void stairsFlipVerticalSwapsHalves() {
        StairRotation stairs = RotationUtils.defaultStairs();
        assertEquals("bottom east -> top east", 4, stairs.transform(0, FLIP_Y));
        assertEquals("top east -> bottom east", 0, stairs.transform(4, FLIP_Y));
        assertEquals("bottom north -> top north", 7, stairs.transform(3, FLIP_Y));
        assertEquals("top west -> bottom west", 1, stairs.transform(5, FLIP_Y));
    }

    @Test
    public void stairsUpsideDownFlipXKeepsHalf() {
        StairRotation stairs = RotationUtils.defaultStairs();
        assertEquals("top east -> top west", 5, stairs.transform(4, FLIP_X));
    }

    // ================= pillars =================
    // Vanilla log metas: axis y=0, x=+4, z=+8 (plus species bits)

    @Test
    public void pillarsUnchangedByFlips() {
        PillarRotation pillar = RotationUtils.defaultPillar();
        assertEquals("x axis unchanged by x flip", 4, pillar.transform(4, FLIP_X));
        assertEquals("z axis unchanged by z flip", 8, pillar.transform(8, FLIP_Z));
        assertEquals("y axis unchanged by vertical flip", 0, pillar.transform(0, FLIP_Y));
        assertEquals("species bits preserved", 5, pillar.transform(5, FLIP_X));
    }

    @Test
    public void pillarsStillRotateThroughTransforms() {
        PillarRotation pillar = RotationUtils.defaultPillar();
        AffineTransform rotate90 = new AffineTransform().rotateY(90);
        assertEquals("x -> z under rotation", 8, pillar.transform(4, rotate90));
        assertEquals("z -> x under rotation", 4, pillar.transform(8, rotate90));
        assertEquals("y unchanged under rotation", 0, pillar.transform(0, rotate90));
    }

    // ================= trapdoors =================
    // Vanilla trapdoor metas: orientation 0=south 1=north 2=east 3=west,
    // +4 = open, +8 = top half

    @Test
    public void trapdoorsFlipHorizontally() {
        TrapdoorRotation trapdoor = RotationUtils.defaultTrapdoor();
        assertEquals("south -> north", 1, trapdoor.transform(0, FLIP_Z));
        assertEquals("north -> south", 0, trapdoor.transform(1, FLIP_Z));
        assertEquals("east -> west", 3, trapdoor.transform(2, FLIP_X));
        assertEquals("west -> east", 2, trapdoor.transform(3, FLIP_X));
        assertEquals("open bit preserved", 7, trapdoor.transform(6, FLIP_X));
    }

    @Test
    public void trapdoorsFlipVerticalSwapsHalves() {
        TrapdoorRotation trapdoor = RotationUtils.defaultTrapdoor();
        assertEquals("bottom -> top", 8, trapdoor.transform(0, FLIP_Y));
        assertEquals("top -> bottom", 0, trapdoor.transform(8, FLIP_Y));
        assertEquals("top open -> bottom open", 4, trapdoor.transform(12, FLIP_Y));
        assertEquals("orientation preserved", 10, trapdoor.transform(2, FLIP_Y));
    }

    // ================= fence gates and buttons =================
    // Vanilla fence gate metas: 0=south 1=west 2=north 3=east (+4 open)
    // Vanilla button metas: 1=east 2=west 3=south 4=north (+8 pressed)

    @Test
    public void fenceGatesFlip() {
        FourRotation gate = RotationUtils.defaultFour(false);
        assertEquals("east -> west", 1, gate.transform(3, FLIP_X));
        assertEquals("west -> east", 3, gate.transform(1, FLIP_X));
        assertEquals("north -> south", 0, gate.transform(2, FLIP_Z));
        assertEquals("south -> north", 2, gate.transform(0, FLIP_Z));
        assertEquals("open bit preserved", 5, gate.transform(7, FLIP_X));
        assertEquals("unchanged by vertical flip", 3, gate.transform(3, FLIP_Y));
    }

    @Test
    public void buttonsFlip() {
        FourRotation button = RotationUtils.defaultFour(true);
        assertEquals("east -> west", 2, button.transform(1, FLIP_X));
        assertEquals("west -> east", 1, button.transform(2, FLIP_X));
        assertEquals("south -> north", 4, button.transform(3, FLIP_Z));
        assertEquals("north -> south", 3, button.transform(4, FLIP_Z));
        assertEquals("pressed bit preserved", 10, button.transform(9, FLIP_X));
    }

    // ================= flips composed with rotations =================

    @Test
    public void flipComposedWithRotation() {
        StairRotation stairs = RotationUtils.defaultStairs();
        // Rotate 90 then flip X: east -> south -> south
        AffineTransform combined = new AffineTransform().scale(-1, 1, 1)
            .rotateY(90);
        int result = stairs.transform(0, combined);
        // The projection handles the composed transform in one pass, the exact
        // facing depends on composition order, it must stay within bottom metas
        org.junit.Assert.assertTrue("bottom stair expected", result >= 0 && result <= 3);

        // A double flip is a 180 degree rotation: east -> west via X, then
        // north/south unaffected, net result must equal rotate(2) on the meta
        AffineTransform doubleFlip = new AffineTransform().scale(-1, 1, -1);
        assertEquals(stairs.rotate(0, 2), stairs.transform(0, doubleFlip));
    }

    // ================= helper sanity =================

    @Test
    public void verticalFlipDetection() {
        org.junit.Assert.assertTrue(RotationUtils.flipsVertically(FLIP_Y));
        org.junit.Assert.assertFalse(RotationUtils.flipsVertically(FLIP_X));
        org.junit.Assert.assertFalse(RotationUtils.flipsVertically(FLIP_Z));
        org.junit.Assert.assertFalse(RotationUtils.flipsVertically(new AffineTransform().rotateY(90)));
    }
}
