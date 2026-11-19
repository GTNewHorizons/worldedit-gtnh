package com.sk89q.worldedit.forge.compat.rotation;

import java.util.function.IntUnaryOperator;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.forge.compat.rotation.types.FourRotation;
import com.sk89q.worldedit.forge.compat.rotation.types.PillarRotation;
import com.sk89q.worldedit.forge.compat.rotation.types.StairRotation;
import com.sk89q.worldedit.forge.compat.rotation.types.TrapdoorRotation;
import com.sk89q.worldedit.math.transform.AffineTransform;

/**
 * Utility methods for rotation mappings.
 */
public final class RotationUtils {

    private RotationUtils() {}

    /**
     * Transform a direction vector through the given transform (ignoring translation).
     */
    public static Vector transformDirection(AffineTransform transform, Vector direction) {
        return transform.apply(direction)
            .subtract(transform.apply(Vector.ZERO));
    }

    /**
     * Whether the transform turns the world upside down (e.g. //flip up/down).
     */
    public static boolean flipsVertically(AffineTransform transform) {
        return transformDirection(transform, new Vector(0, 1, 0)).getY() < 0;
    }

    public static int rotatePillar90(int data) {
        int axis = data & 0xC;
        if (axis == 0x4) { // x -> z
            return (data & ~0xC) | 0x8;
        } else if (axis == 0x8) { // z -> x
            return (data & ~0xC) | 0x4;
        } else {
            return data;
        }
    }

    public static int rotateStairs90(int data) {
        boolean bigMeta = data >= 8;
        int meta = bigMeta ? data - 8 : data;
        int result;
        switch (meta) {
            case 0:
                result = 2;
                break;
            case 1:
                result = 3;
                break;
            case 2:
                result = 1;
                break;
            case 3:
                result = 0;
                break;
            case 4:
                result = 6;
                break;
            case 5:
                result = 7;
                break;
            case 6:
                result = 5;
                break;
            case 7:
                result = 4;
                break;
            default:
                result = meta;
        }
        return bigMeta ? result + 8 : result;
    }

    public static int rotateStairs90Reverse(int data) {
        boolean bigMeta = data >= 8;
        int meta = bigMeta ? data - 8 : data;
        int result;
        switch (meta) {
            case 2:
                result = 0;
                break;
            case 3:
                result = 1;
                break;
            case 1:
                result = 2;
                break;
            case 0:
                result = 3;
                break;
            case 6:
                result = 4;
                break;
            case 7:
                result = 5;
                break;
            case 5:
                result = 6;
                break;
            case 4:
                result = 7;
                break;
            default:
                result = meta;
        }
        return bigMeta ? result + 8 : result;
    }

    public static int rotateTrapdoor90(int data) {
        int without = data & ~0x3;
        int orientation = data & 0x3;
        switch (orientation) {
            case 0:
                return 3 | without;
            case 1:
                return 2 | without;
            case 2:
                return 0 | without;
            case 3:
                return 1 | without;
            default:
                return data;
        }
    }

    public static int rotateTrapdoor90Reverse(int data) {
        int without = data & ~0x3;
        int orientation = data & 0x3;
        switch (orientation) {
            case 3:
                return 0 | without;
            case 2:
                return 1 | without;
            case 0:
                return 2 | without;
            case 1:
                return 3 | without;
            default:
                return data;
        }
    }

    public static int rotateFenceGate90(int data) {
        int extra = data & ~0x3;
        int orientation = data & 0x3;
        orientation = (orientation + 1) & 3;
        return orientation | extra;
    }

    public static int rotateFenceGate90Reverse(int data) {
        int extra = data & ~0x3;
        int orientation = data & 0x3;
        orientation = (orientation + 3) & 3;
        return orientation | extra;
    }

    /**
     * Rotate the given metadata according to the rotation type and transform.
     */
    public static int rotateMeta(RotationType type, int ticks, int data) {
        if (ticks == 0) {
            return data;
        }
        int steps = Math.abs(ticks) % 4;
        for (int i = 0; i < steps; i++) {
            switch (type) {
                case STAIRS:
                    data = ticks > 0 ? rotateStairs90(data) : rotateStairs90Reverse(data);
                    break;
                case PILLAR:
                    data = rotatePillar90(data);
                    break;
                case TRAP_DOOR:
                    data = ticks > 0 ? rotateTrapdoor90(data) : rotateTrapdoor90Reverse(data);
                    break;
                case FENCE_GATE:
                    data = ticks > 0 ? rotateFenceGate90(data) : rotateFenceGate90Reverse(data);
                    break;
                default:
                    break;
            }
        }
        return data;
    }

    public static StairRotation defaultStairs() {
        StairRotation sr = new StairRotation();
        sr.setBottom(fillDirectional(3, RotationUtils::rotateStairs90));
        sr.setTop(fillDirectional(7, RotationUtils::rotateStairs90));
        return sr;
    }

    public static PillarRotation defaultPillar() {
        PillarRotation pr = new PillarRotation();
        pr.setGroups(new int[][] { { 0, 4, 8 }, { 1, 5, 9 }, { 2, 6, 10 }, { 3, 7, 11 } });
        return pr;
    }

    public static FourRotation defaultFour(boolean button) {
        int start = button ? 4 : 2; // north facing for fence gates
        IntUnaryOperator rot = button ? RotationUtils::rotateButton90 : RotationUtils::rotateFenceGate90;
        FourRotation fr = new FourRotation();
        fr.setMetas(fillDirectional(start, rot));
        return fr;
    }

    private static int[] fillDirectional(int start, IntUnaryOperator rot) {
        int[] arr = new int[4];
        int meta = start;
        for (int i = 0; i < 4; i++) {
            arr[i] = meta;
            meta = rot.applyAsInt(meta);
        }
        return arr;
    }

    public static int rotateButton90(int data) {
        int dir = data & 7;
        int pressed = data & 8;
        int out;
        switch (dir) {
            case 1:
                out = 3;
                break; // east -> south
            case 3:
                out = 2;
                break; // south -> west
            case 2:
                out = 4;
                break; // west -> north
            case 4:
                out = 1;
                break; // north -> east
            default:
                out = dir;
                break;
        }
        return out | pressed;
    }

    public static int rotateButton90Reverse(int data) {
        int dir = data & 7;
        int pressed = data & 8;
        int out;
        switch (dir) {
            case 3:
                out = 1;
                break; // south -> east
            case 2:
                out = 3;
                break; // west -> south
            case 4:
                out = 2;
                break; // north -> west
            case 1:
                out = 4;
                break; // east -> north
            default:
                out = dir;
                break;
        }
        return out | pressed;
    }

    public static TrapdoorRotation defaultTrapdoor() {
        return new TrapdoorRotation();
    }
}
