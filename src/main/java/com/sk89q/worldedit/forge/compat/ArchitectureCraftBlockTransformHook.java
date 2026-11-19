package com.sk89q.worldedit.forge.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;

import javax.annotation.Nullable;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.CompoundTagBuilder;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.transform.BlockTransformHook;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;

/**
 * Turns and mirrors ArchitectureCraft shapes, which keep their orientation as a {@code side} and a
 * {@code turn} in their tile entity.
 *
 * <p>
 * The orientation is the rotation ArchitectureCraft builds from those two numbers (its
 * {@code Matrix3.sideTurnRotations}), from the shape's own coordinates to the world's. Rotating the
 * clipboard rotates that too. A mirror can't be stored as a side and turn, so a mirrored shape is the
 * rotation that matches it, given the shape's symmetry: most shapes are symmetric across their own X
 * axis, and corners across their diagonal.
 * </p>
 */
public class ArchitectureCraftBlockTransformHook implements BlockTransformHook {

    /** ArchitectureCraft's orientation for each side and turn, as integer matrices. */
    private static final int[][][][] ORIENTATIONS = new int[6][4][][];

    /** Mirror across the shape's own X axis, which Bilateral and Quadrilateral shapes don't mind. */
    private static final int[][] MIRROR_X = { { -1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } };

    /**
     * Mirror across the diagonal through the corner a Unilateral shape faces at turn 0, which is at
     * negative X and positive Z (ArchitectureCraft's {@code turnForPlacementHit}).
     */
    private static final int[][] MIRROR_DIAGONAL = { { 0, 0, -1 }, { 0, 1, 0 }, { -1, 0, 0 } };

    static {
        int[][][] sides = { identity(), rotX(180), rotX(90), mul(rotX(-90), rotY(180)), mul(rotZ(-90), rotY(90)),
            mul(rotZ(90), rotY(-90)) };
        for (int side = 0; side < 6; side++) {
            for (int turn = 0; turn < 4; turn++) {
                ORIENTATIONS[side][turn] = mul(sides[side], rotY(90 * turn));
            }
        }
    }

    private final IntFunction<String> symmetry;

    public ArchitectureCraftBlockTransformHook() {
        this(new ShapeSymmetryLookup());
    }

    /**
     * @param symmetry gives the name of a shape's {@code ShapeSymmetry} from its ID, or null if unknown
     */
    ArchitectureCraftBlockTransformHook(IntFunction<String> symmetry) {
        this.symmetry = symmetry;
    }

    @Override
    public BaseBlock transformBlock(BaseBlock block, Transform transform) {
        CompoundTag nbt = block.getNbtData();
        if (nbt == null || !"gcewing.shape".equals(nbt.getString("id"))) {
            return block;
        }
        if (!(transform instanceof AffineTransform affine)) {
            return block;
        }

        int[] sides = SideMaps.sideMap(affine);
        if (SideMaps.isIdentity(sides)) {
            return block;
        }

        boolean mirrored = det(world(sides)) < 0;
        int[] orientation = transform(nbt.getByte("side"), nbt.getByte("turn"), sides, mirroring(nbt.getInt("Shape")));
        if (orientation == null) {
            return block;
        }

        CompoundTagBuilder builder = nbt.createBuilder()
            .putByte("side", (byte) orientation[0])
            .putByte("turn", (byte) orientation[1]);
        if (nbt.containsKey("Disconnected")) {
            // Connections are kept by world direction.
            int disconnected = nbt.getInt("Disconnected");
            int moved = disconnected & ~0x3F;
            for (int side = 0; side < 6; side++) {
                if ((disconnected & 1 << side) != 0) {
                    moved |= 1 << sides[side];
                }
            }
            builder.putInt("Disconnected", moved);
        }
        if (mirrored && nbt.containsKey("offsetX") && mirroring(nbt.getInt("Shape")) == MIRROR_X) {
            // The offset is along the shape's own X axis, which the mirror flipped.
            builder.putByte("offsetX", (byte) -nbt.getByte("offsetX"));
        }
        return new BaseBlock(block.getId(), block.getData(), builder.build());
    }

    private int[][] mirroring(int shape) {
        return "Unilateral".equals(symmetry.apply(shape)) ? MIRROR_DIAGONAL : MIRROR_X;
    }

    /**
     * Work out the side and turn after the transform.
     *
     * @param sides  where the transform sends each side
     * @param mirror how to undo a mirror in the shape's own coordinates, for transforms that mirror
     * @return the new side and turn, or null if the old ones were invalid
     */
    @Nullable
    static int[] transform(int side, int turn, int[] sides, int[][] mirror) {
        if (side < 0 || side > 5 || turn < 0 || turn > 3) {
            return null;
        }
        int[][] world = world(sides);
        int[][] target = mul(world, ORIENTATIONS[side][turn]);
        if (det(world) < 0) {
            target = mul(target, mirror);
        }
        for (int newSide = 0; newSide < 6; newSide++) {
            for (int newTurn = 0; newTurn < 4; newTurn++) {
                if (equal(ORIENTATIONS[newSide][newTurn], target)) {
                    return new int[] { newSide, newTurn };
                }
            }
        }
        return null;
    }

    static int[][] orientation(int side, int turn) {
        return ORIENTATIONS[side][turn];
    }

    static int[][] mirrorX() {
        return MIRROR_X;
    }

    static int[][] mirrorDiagonal() {
        return MIRROR_DIAGONAL;
    }

    /** The transform as a matrix, built from where it sends the positive X, Y and Z sides. */
    static int[][] world(int[] sides) {
        int[][] m = new int[3][3];
        int[] positive = { 5, 1, 3 };
        for (int axis = 0; axis < 3; axis++) {
            int side = sides[positive[axis]];
            int sign = (side & 1) != 0 ? 1 : -1;
            // Side axes: 0 and 1 are Y, 2 and 3 are Z, 4 and 5 are X.
            int row = side >> 1 == 0 ? 1 : side >> 1 == 1 ? 2 : 0;
            m[row][axis] = sign;
        }
        return m;
    }

    private static int[][] identity() {
        return new int[][] { { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } };
    }

    private static int[][] rotX(int deg) {
        return rot(deg, 1, 2);
    }

    private static int[][] rotY(int deg) {
        return rot(deg, 2, 0);
    }

    private static int[][] rotZ(int deg) {
        return rot(deg, 0, 1);
    }

    /** ArchitectureCraft's {@code Matrix3.rot}. */
    private static int[][] rot(int deg, int i, int j) {
        int quarter = ((deg / 90) % 4 + 4) % 4;
        int s = quarter == 1 ? 1 : quarter == 3 ? -1 : 0;
        int c = quarter == 0 ? 1 : quarter == 2 ? -1 : 0;
        int[][] r = identity();
        r[i][i] = c;
        r[i][j] = -s;
        r[j][i] = s;
        r[j][j] = c;
        return r;
    }

    static int[][] mul(int[][] a, int[][] b) {
        int[][] r = new int[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                r[i][j] = a[i][0] * b[0][j] + a[i][1] * b[1][j] + a[i][2] * b[2][j];
            }
        }
        return r;
    }

    private static int det(int[][] m) {
        return m[0][0] * (m[1][1] * m[2][2] - m[1][2] * m[2][1]) - m[0][1] * (m[1][0] * m[2][2] - m[1][2] * m[2][0])
            + m[0][2] * (m[1][0] * m[2][1] - m[1][1] * m[2][0]);
    }

    private static boolean equal(int[][] a, int[][] b) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (a[i][j] != b[i][j]) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Asks ArchitectureCraft for a shape's symmetry, which WorldEdit doesn't compile against.
     */
    private static final class ShapeSymmetryLookup implements IntFunction<String> {

        private final Map<Integer, String> known = new HashMap<Integer, String>();
        private Method forId;
        private Field symmetry;
        private boolean unavailable;

        @Override
        public synchronized String apply(int shape) {
            if (known.containsKey(shape)) {
                return known.get(shape);
            }
            String name = null;
            if (!unavailable) {
                try {
                    if (forId == null) {
                        Class<?> shapes = Class.forName("gcewing.architecture.common.shape.Shape");
                        forId = shapes.getMethod("forId", int.class);
                        symmetry = shapes.getField("symmetry");
                    }
                    Object found = forId.invoke(null, shape);
                    Object value = found != null ? symmetry.get(found) : null;
                    name = value != null ? ((Enum<?>) value).name() : null;
                } catch (ReflectiveOperationException | LinkageError | ClassCastException e) {
                    unavailable = true;
                }
            }
            known.put(shape, name);
            return name;
        }
    }
}
