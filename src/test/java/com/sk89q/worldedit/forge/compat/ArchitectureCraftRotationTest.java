package com.sk89q.worldedit.forge.compat;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.sk89q.jnbt.ByteTag;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.math.transform.AffineTransform;

public class ArchitectureCraftRotationTest {

    private static final AffineTransform[] ROTATIONS = { new AffineTransform().rotateY(-90),
        new AffineTransform().rotateY(90), new AffineTransform().rotateY(180), new AffineTransform().rotateX(90),
        new AffineTransform().rotateZ(-90) };

    private static final AffineTransform[] FLIPS = { new AffineTransform().scale(-1, 1, 1),
        new AffineTransform().scale(1, -1, 1), new AffineTransform().scale(1, 1, -1) };

    // ArchitectureCraft's Matrix3, copied with doubles from its source.
    private static double[][] rot(double deg, int i, int j) {
        double a = Math.toRadians(deg);
        double[][] r = { { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } };
        r[i][i] = Math.cos(a);
        r[i][j] = -Math.sin(a);
        r[j][i] = Math.sin(a);
        r[j][j] = Math.cos(a);
        return r;
    }

    private static double[][] mul(double[][] m, double[][] n) {
        double[][] r = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                r[i][j] = m[i][0] * n[0][j] + m[i][1] * n[1][j] + m[i][2] * n[2][j];
            }
        }
        return r;
    }

    private static double[][] acOrientation(int side, int turn) {
        double[][][] sides = { { { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } }, rot(180, 1, 2), rot(90, 1, 2),
            mul(rot(-90, 1, 2), rot(180, 2, 0)), mul(rot(-90, 0, 1), rot(90, 2, 0)),
            mul(rot(90, 0, 1), rot(-90, 2, 0)) };
        return mul(sides[side], rot(90 * turn, 2, 0));
    }

    private static Vector apply(int[][] m, Vector v) {
        return new Vector(
            m[0][0] * v.getX() + m[0][1] * v.getY() + m[0][2] * v.getZ(),
            m[1][0] * v.getX() + m[1][1] * v.getY() + m[1][2] * v.getZ(),
            m[2][0] * v.getX() + m[2][1] * v.getY() + m[2][2] * v.getZ());
    }

    private static Vector world(AffineTransform transform, Vector v) {
        Vector moved = transform.apply(v)
            .subtract(transform.apply(Vector.ZERO));
        return new Vector(Math.round(moved.getX()), Math.round(moved.getY()), Math.round(moved.getZ()));
    }

    /** Where a direction in the shape's own coordinates points in the world, before and after. */
    private static void assertKeeps(Vector local, int side, int turn, int[] after, AffineTransform transform) {
        Vector before = apply(ArchitectureCraftBlockTransformHook.orientation(side, turn), local);
        Vector expected = world(transform, before);
        assertEquals(
            "side " + side + " turn " + turn + " direction " + local,
            expected,
            apply(ArchitectureCraftBlockTransformHook.orientation(after[0], after[1]), local));
    }

    @Test
    public void orientationsMatchArchitectureCraft() {
        for (int side = 0; side < 6; side++) {
            for (int turn = 0; turn < 4; turn++) {
                double[][] expected = acOrientation(side, turn);
                int[][] actual = ArchitectureCraftBlockTransformHook.orientation(side, turn);
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        assertEquals(expected[i][j], actual[i][j], 1e-9);
                    }
                }
            }
        }
    }

    @Test
    public void rotationsTurnTheWholeShape() {
        Vector[] axes = { new Vector(1, 0, 0), new Vector(0, 1, 0), new Vector(0, 0, 1) };
        for (AffineTransform transform : ROTATIONS) {
            int[] sides = SideMaps.sideMap(transform);
            for (int side = 0; side < 6; side++) {
                for (int turn = 0; turn < 4; turn++) {
                    int[] after = ArchitectureCraftBlockTransformHook
                        .transform(side, turn, sides, ArchitectureCraftBlockTransformHook.mirrorX());
                    assertNotNull(after);
                    for (Vector axis : axes) {
                        assertKeeps(axis, side, turn, after, transform);
                    }
                }
            }
        }
    }

    @Test
    public void flippedShapesStayOnTheirSurfaceAndKeepWhatTheirSymmetryNeeds() {
        Vector down = new Vector(0, -1, 0);
        Vector front = new Vector(0, 0, 1);
        Vector corner = new Vector(-1, 0, 1);
        for (AffineTransform transform : FLIPS) {
            int[] sides = SideMaps.sideMap(transform);
            for (int side = 0; side < 6; side++) {
                for (int turn = 0; turn < 4; turn++) {
                    int[] bilateral = ArchitectureCraftBlockTransformHook
                        .transform(side, turn, sides, ArchitectureCraftBlockTransformHook.mirrorX());
                    assertKeeps(down, side, turn, bilateral, transform);
                    assertKeeps(front, side, turn, bilateral, transform);

                    int[] unilateral = ArchitectureCraftBlockTransformHook
                        .transform(side, turn, sides, ArchitectureCraftBlockTransformHook.mirrorDiagonal());
                    assertKeeps(down, side, turn, unilateral, transform);
                    assertKeeps(corner, side, turn, unilateral, transform);
                }
            }
        }
    }

    private static BaseBlock shape(int shape, int side, int turn, Map<String, Tag> extra) {
        Map<String, Tag> tags = new HashMap<String, Tag>(extra);
        tags.put("id", new StringTag("gcewing.shape"));
        tags.put("Shape", new IntTag(shape));
        tags.put("side", new ByteTag((byte) side));
        tags.put("turn", new ByteTag((byte) turn));
        return new BaseBlock(1000, 0, new CompoundTag(tags));
    }

    private static final ArchitectureCraftBlockTransformHook HOOK = new ArchitectureCraftBlockTransformHook(
        shape -> shape == 1 ? "Unilateral" : "Bilateral");

    @Test
    public void disconnectedSidesFollowTheRotation() {
        Map<String, Tag> extra = new HashMap<String, Tag>();
        extra.put("Disconnected", new IntTag(1 << 2 | 1 << 0));

        CompoundTag after = HOOK.transformBlock(shape(0, 0, 0, extra), new AffineTransform().rotateY(-90))
            .getNbtData();

        // //rotate 90 turns north into east; down stays down.
        assertEquals(1 << 5 | 1 << 0, after.getInt("Disconnected"));
    }

    @Test
    public void aFlipMirrorsTheOffsetOfSymmetricShapes() {
        Map<String, Tag> extra = new HashMap<String, Tag>();
        extra.put("offsetX", new ByteTag((byte) 4));

        CompoundTag flipped = HOOK.transformBlock(shape(0, 0, 0, extra), new AffineTransform().scale(-1, 1, 1))
            .getNbtData();
        CompoundTag turned = HOOK.transformBlock(shape(0, 0, 0, extra), new AffineTransform().rotateY(-90))
            .getNbtData();

        assertEquals(-4, flipped.getByte("offsetX"));
        assertEquals(4, turned.getByte("offsetX"));
    }

    @Test
    public void cornersMirrorAcrossTheirDiagonal() {
        int[] sides = SideMaps.sideMap(new AffineTransform().scale(-1, 1, 1));
        CompoundTag after = HOOK
            .transformBlock(shape(1, 0, 0, new HashMap<String, Tag>()), new AffineTransform().scale(-1, 1, 1))
            .getNbtData();

        assertArrayEquals(
            ArchitectureCraftBlockTransformHook
                .transform(0, 0, sides, ArchitectureCraftBlockTransformHook.mirrorDiagonal()),
            new int[] { after.getByte("side"), after.getByte("turn") });
    }

    @Test
    public void otherBlocksAndNoTransformAreLeftAlone() {
        BaseBlock block = shape(0, 2, 1, new HashMap<String, Tag>());

        assertSame(block, HOOK.transformBlock(block, new AffineTransform()));
        BaseBlock plain = new BaseBlock(1);
        assertSame(plain, HOOK.transformBlock(plain, new AffineTransform().rotateY(90)));
    }
}
