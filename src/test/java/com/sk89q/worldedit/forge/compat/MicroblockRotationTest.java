package com.sk89q.worldedit.forge.compat;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.sk89q.jnbt.ByteTag;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.math.transform.AffineTransform;

/**
 * Checks each microblock slot against geometry: the slot's position in the block, moved by the
 * transform, must be the position of the slot it ends up in.
 */
public class MicroblockRotationTest {

    private static final AffineTransform[] TRANSFORMS = { new AffineTransform().rotateY(-90),
        new AffineTransform().rotateY(90), new AffineTransform().rotateY(180), new AffineTransform().rotateX(90),
        new AffineTransform().rotateZ(-90), new AffineTransform().scale(-1, 1, 1),
        new AffineTransform().scale(1, -1, 1), new AffineTransform().scale(1, 1, -1) };

    private static CompoundTag micro(String id, int size, int slot) {
        Map<String, Tag> tags = new HashMap<String, Tag>();
        tags.put("id", new StringTag(id));
        tags.put("shape", new ByteTag((byte) (size << 4 | slot)));
        tags.put("material", new StringTag("minecraft:stone"));
        return new CompoundTag(tags);
    }

    private static int slotAfter(CompoundTag part, AffineTransform transform) {
        CompoundTag moved = ForgeMultipartExistsCompat
            .transformMicroblock(part, ForgeMultipartExistsCompat.sideMap(transform));
        assertEquals("the size must not change", part.getByte("shape") >> 4, moved.getByte("shape") >> 4);
        return moved.getByte("shape") & 0xF;
    }

    private static Vector move(AffineTransform transform, Vector v) {
        Vector moved = transform.apply(v)
            .subtract(transform.apply(Vector.ZERO));
        return new Vector(Math.round(moved.getX()), Math.round(moved.getY()), Math.round(moved.getZ()));
    }

    /** Y is bit 1, Z bit 2, X bit 4, as ForgeMultipart numbers axes. */
    private static Vector fromBits(int free, int positive) {
        double y = (free & 1) == 0 ? 0 : (positive & 1) != 0 ? 1 : -1;
        double z = (free & 2) == 0 ? 0 : (positive & 2) != 0 ? 1 : -1;
        double x = (free & 4) == 0 ? 0 : (positive & 4) != 0 ? 1 : -1;
        return new Vector(x, y, z);
    }

    /**
     * Written from the names of ForgeMultipart's PartMap EDGE_NYN to EDGE_XPP: the letters give X, Y
     * and Z in order, N or P for the negative or positive end, and the axis letter for the edge's own
     * axis.
     */
    private static final String[] EDGE_NAMES = { "NYN", "NYP", "PYN", "PYP", "NNZ", "PNZ", "NPZ", "PPZ", "XNN", "XPN",
        "XNP", "XPP" };

    private static double coordinate(char c) {
        return c == 'N' ? -1 : c == 'P' ? 1 : 0;
    }

    private static Vector edge(int e) {
        String name = EDGE_NAMES[e];
        return new Vector(coordinate(name.charAt(0)), coordinate(name.charAt(1)), coordinate(name.charAt(2)));
    }

    private static final Vector[] SIDES = { new Vector(0, -1, 0), new Vector(0, 1, 0), new Vector(0, 0, -1),
        new Vector(0, 0, 1), new Vector(-1, 0, 0), new Vector(1, 0, 0) };

    @Test
    public void facesAndHollowsGoWhereTheirSideGoes() {
        for (AffineTransform transform : TRANSFORMS) {
            for (int side = 0; side < 6; side++) {
                Vector expected = move(transform, SIDES[side]);
                assertEquals(expected, SIDES[slotAfter(micro("mcr_face", 1, side), transform)]);
                assertEquals(expected, SIDES[slotAfter(micro("mcr_hllw", 2, side), transform)]);
            }
        }
    }

    @Test
    public void cornersGoWhereTheirCornerGoes() {
        for (AffineTransform transform : TRANSFORMS) {
            for (int corner = 0; corner < 8; corner++) {
                Vector expected = move(transform, fromBits(7, corner));
                assertEquals(expected, fromBits(7, slotAfter(micro("mcr_cnr", 1, corner), transform)));
            }
        }
    }

    @Test
    public void edgesGoWhereTheirEdgeGoes() {
        for (AffineTransform transform : TRANSFORMS) {
            for (int e = 0; e < 12; e++) {
                Vector expected = move(transform, edge(e));
                assertEquals(expected, edge(slotAfter(micro("mcr_edge", 2, e), transform)));
            }
        }
    }

    @Test
    public void postsFollowTheirAxis() {
        Vector[] axes = { new Vector(0, 1, 0), new Vector(0, 0, 1), new Vector(1, 0, 0) };
        for (AffineTransform transform : TRANSFORMS) {
            for (int axis = 0; axis < 3; axis++) {
                Vector moved = move(transform, axes[axis]);
                Vector expected = new Vector(Math.abs(moved.getX()), Math.abs(moved.getY()), Math.abs(moved.getZ()));
                assertEquals(expected, axes[slotAfter(micro("mcr_post", 4, axis), transform)]);
            }
        }
    }

    @Test
    public void rotatingFourTimesComesBackToTheStart() {
        AffineTransform quarter = new AffineTransform().rotateY(-90);
        for (int e = 0; e < 12; e++) {
            CompoundTag part = micro("mcr_edge", 2, e);
            for (int i = 0; i < 4; i++) {
                part = ForgeMultipartExistsCompat
                    .transformMicroblock(part, ForgeMultipartExistsCompat.sideMap(quarter));
            }
            assertEquals(e, part.getByte("shape") & 0xF);
        }
    }
}
