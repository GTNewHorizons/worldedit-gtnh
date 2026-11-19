package com.sk89q.worldedit.forge.compat;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

import org.junit.Test;

import com.google.gson.Gson;
import com.sk89q.worldedit.forge.compat.rotation.RotationMapping;
import com.sk89q.worldedit.forge.compat.rotation.RotationMappings;
import com.sk89q.worldedit.forge.compat.rotation.RotationType;
import com.sk89q.worldedit.forge.compat.rotation.RotationUtils;
import com.sk89q.worldedit.forge.compat.rotation.types.FourRotation;
import com.sk89q.worldedit.forge.compat.rotation.types.PillarRotation;
import com.sk89q.worldedit.forge.compat.rotation.types.StairRotation;
import com.sk89q.worldedit.forge.compat.rotation.types.TrapdoorRotation;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.util.gson.GsonUtil;

/** Tests for {@link RotationMappings}. */
public class RotationMappingsTest {

    @Test
    public void testGenerateDefaults() throws Exception {
        File dir = Files.createTempDirectory("modrot")
            .toFile();
        RotationMappings.init(dir);
        RotationMappings cfg = RotationMappings.getInstance();
        assertNotNull(cfg);
        File f = new File(dir, "mappings/stairs.json");
        assertTrue("config file missing", f.exists());
        assertTrue(new File(dir, "mappings/trap_door.json").exists());
        assertFalse(new File(dir, "mappings/door.json").exists());

        Gson gson = GsonUtil.createBuilder()
            .create();
        try (java.io.FileReader r = new java.io.FileReader(f)) {
            com.google.gson.stream.JsonReader jr = new com.google.gson.stream.JsonReader(r);
            jr.setLenient(true);
            @SuppressWarnings("unchecked")
            Map<String, Object> map = gson.fromJson(jr, Map.class);
            if (map != null) {
                for (String key : map.keySet()) {
                    assertFalse("vanilla entry present", key.startsWith("minecraft:"));
                }
            }
        }
        // ensure comment exists
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(f))) {
            String first = br.readLine();
            assertTrue(first.startsWith("//"));
        }
        File trapFile = new File(dir, "mappings/trap_door.json");
        com.google.gson.JsonObject trapJson;
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(trapFile))) {
            String first = br.readLine();
            assertTrue(first.startsWith("//"));
            trapJson = new com.google.gson.JsonParser().parse(br)
                .getAsJsonObject();
        }
        if (!trapJson.entrySet()
            .isEmpty()) {
            java.util.Map.Entry<String, com.google.gson.JsonElement> ent = trapJson.entrySet()
                .iterator()
                .next();
            com.google.gson.JsonObject defaults = ent.getValue()
                .getAsJsonObject();
            assertTrue(
                defaults.getAsJsonObject("bottom")
                    .has("open"));
            assertTrue(
                defaults.getAsJsonObject("bottom")
                    .has("closed"));
            assertTrue(
                defaults.getAsJsonObject("top")
                    .has("open"));
            assertTrue(
                defaults.getAsJsonObject("top")
                    .has("closed"));
        }
        File pillarFile = new File(dir, "mappings/pillar.json");
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(pillarFile))) {
            String first = br.readLine();
            assertTrue(first.startsWith("//"));
        }

        // add a dummy custom mapping and verify type key
        RotationMapping custom = new RotationMapping(RotationType.OTHER, RotationUtils.defaultFour(true));
        cfg.put("dummy:button", custom);
        cfg.save();
        File other = new File(dir, "mappings/other.json");
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(other))) {
            String comment = br.readLine();
            assertTrue(comment.startsWith("//"));
            com.google.gson.JsonObject obj = new com.google.gson.JsonParser().parse(br)
                .getAsJsonObject();
            assertTrue(
                obj.getAsJsonObject("dummy:button")
                    .has("type"));
        }
    }

    // trapdoor defaults covered by rotateMeta tests

    @Test
    public void testStairsBigMetaRotation() {
        int meta = 10; // meta >= 8
        int result = RotationUtils.rotateMeta(RotationType.STAIRS, 1, meta);
        assertEquals("big meta stairs rotated", 9, result);
    }

    @Test
    public void testStairsLowMetaRotation() {
        int meta = 3; // north bottom
        int result = RotationUtils.rotateMeta(RotationType.STAIRS, 1, meta);
        assertEquals("low meta stairs rotated", 0, result);
    }

    @Test
    public void testStairsDefaultArrays() {
        StairRotation sr = RotationUtils.defaultStairs();
        assertEquals(3, sr.getBottom()[0]);
        assertEquals(7, sr.getTop()[0]);
    }

    @Test
    public void testStairsRotateAll() {
        StairRotation sr = RotationUtils.defaultStairs();
        for (int meta = 0; meta < 16; meta++) {
            int expected = RotationUtils.rotateStairs90(meta);
            assertEquals("meta " + meta, expected, sr.rotate(meta, 1));
        }
    }

    @Test
    public void testStairsTransformAll() {
        StairRotation sr = RotationUtils.defaultStairs();
        AffineTransform rot = new AffineTransform().rotateY(-90);
        for (int meta = 0; meta < 16; meta++) {
            assertEquals("meta " + meta, sr.rotate(meta, 1), sr.transform(meta, rot));
        }
    }

    @Test
    public void testPillarRotation() {
        int meta = 4; // x axis
        int result = RotationUtils.rotateMeta(RotationType.PILLAR, 1, meta);
        assertEquals("pillar axis swapped", 8, result);
        assertEquals("vertical pillar unchanged", 0, RotationUtils.rotateMeta(RotationType.PILLAR, 1, 0));
    }

    @Test
    public void testPillarGroupRotation() {
        PillarRotation pr = RotationUtils.defaultPillar();
        int[][] groups = pr.getGroups();
        assertTrue(groups.length > 1);
        int meta = groups[1][1]; // x from second group
        int rotated = pr.rotate(meta, 1);
        assertEquals(groups[1][2], rotated);
    }

    @Test
    public void testPillarRotationAllSteps() {
        PillarRotation pr = RotationUtils.defaultPillar();
        int[][] groups = pr.getGroups();
        int[] steps = { 1, 2, 3, -1, -2, -3 };
        for (int[] g : groups) {
            // vertical meta never changes
            for (int s : steps) {
                assertEquals(g[0], pr.rotate(g[0], s));
            }
            for (int s : steps) {
                int expected = (Math.abs(s) % 2 == 0) ? g[1] : g[2];
                assertEquals("x meta step " + s, expected, pr.rotate(g[1], s));
                expected = (Math.abs(s) % 2 == 0) ? g[2] : g[1];
                assertEquals("z meta step " + s, expected, pr.rotate(g[2], s));
            }
        }
    }

    @Test
    public void testPillarTransform() {
        PillarRotation pr = RotationUtils.defaultPillar();
        AffineTransform rot = new AffineTransform().rotateY(-90);
        int[][] groups = pr.getGroups();
        for (int[] g : groups) {
            assertEquals(g[2], pr.transform(g[1], rot));
            assertEquals(g[1], pr.transform(g[2], rot));
            assertEquals(g[0], pr.transform(g[0], rot));
        }
    }

    @Test
    public void testPillarUnknownExtraMeta() {
        PillarRotation pr = RotationUtils.defaultPillar();
        // 6 has x orientation plus unknown bits
        assertEquals(10, pr.rotate(6, 1));
    }

    @Test
    public void testPillarUnknownMeta() {
        PillarRotation pr = RotationUtils.defaultPillar();
        assertEquals(15, pr.rotate(15, 1));
    }

    @Test
    public void testPillarDefault() {
        PillarRotation pr = RotationUtils.defaultPillar();
        assertEquals(0, pr.getY());
        assertEquals(4, pr.getX());
        assertEquals(8, pr.getZ());
        assertEquals(4, pr.getGroups().length);
    }

    @Test
    public void testButtonDefaults() {
        FourRotation fr = RotationUtils.defaultFour(true);
        assertEquals(4, fr.getMetas()[0]);
    }

    @Test
    public void testButtonRotation() throws Exception {
        int out = RotationUtils.rotateButton90(4);
        assertEquals(1, out);
    }

    @Test
    public void testStairsBigMetaReverseRotation() {
        int meta = 12;
        int result = RotationUtils.rotateMeta(RotationType.STAIRS, -1, meta);
        assertEquals("big meta stairs reverse", 15, result);
    }

    @Test
    public void testStairsBigMetaDoubleRotation() {
        int meta = 8;
        int result = RotationUtils.rotateMeta(RotationType.STAIRS, 2, meta);
        assertEquals("big meta stairs 180", 9, result);
    }

    @Test
    public void testStairsReverseRotationAll() {
        for (int meta = 0; meta < 16; meta++) {
            int expected = RotationUtils.rotateStairs90Reverse(meta);
            assertEquals("meta " + meta, expected, RotationUtils.rotateMeta(RotationType.STAIRS, -1, meta));
        }
    }

    @Test
    public void testStairsDoubleRotationAll() {
        for (int meta = 0; meta < 16; meta++) {
            int expected = RotationUtils.rotateStairs90(RotationUtils.rotateStairs90(meta));
            assertEquals("meta " + meta, expected, RotationUtils.rotateMeta(RotationType.STAIRS, 2, meta));
        }
    }

    @Test
    public void testFenceGateDefault() {
        FourRotation fr = RotationUtils.defaultFour(false);
        assertEquals(2, fr.getMetas()[0]);
        assertEquals(3, fr.getMetas()[1]);
    }

    @Test
    public void testFenceGateRotation() {
        int meta = 0;
        int result = RotationUtils.rotateMeta(RotationType.FENCE_GATE, 3, meta);
        assertEquals("gate rotated thrice", 3, result);
    }

    @Test
    public void testFenceGateForwardRotationAll() {
        for (int meta = 0; meta < 16; meta++) {
            int expected = RotationUtils.rotateFenceGate90(meta);
            assertEquals("meta " + meta, expected, RotationUtils.rotateMeta(RotationType.FENCE_GATE, 1, meta));
        }
    }

    @Test
    public void testFenceGateReverseRotationAll() {
        for (int meta = 0; meta < 16; meta++) {
            int expected = RotationUtils.rotateFenceGate90Reverse(meta);
            assertEquals("meta " + meta, expected, RotationUtils.rotateMeta(RotationType.FENCE_GATE, -1, meta));
        }
    }

    @Test
    public void testFenceGateTripleRotationAll() {
        for (int meta = 0; meta < 16; meta++) {
            int expected = RotationUtils
                .rotateFenceGate90(RotationUtils.rotateFenceGate90(RotationUtils.rotateFenceGate90(meta)));
            assertEquals("meta " + meta, expected, RotationUtils.rotateMeta(RotationType.FENCE_GATE, 3, meta));
        }
    }

    @Test
    public void testFourRotationTransform() {
        FourRotation fr = RotationUtils.defaultFour(false);
        AffineTransform rot = new AffineTransform().rotateY(-90);
        for (int i = 0; i < 4; i++) {
            assertEquals(fr.rotate(fr.getMetas()[i], 1), fr.transform(fr.getMetas()[i], rot));
        }
    }

    @Test
    public void testFourRotationTransformOpenStates() {
        FourRotation fr = RotationUtils.defaultFour(false);
        AffineTransform rot = new AffineTransform().rotateY(-90);
        for (int meta = 0; meta < 16; meta++) {
            assertEquals("meta " + meta, RotationUtils.rotateFenceGate90(meta), fr.transform(meta, rot));
        }
    }

    @Test
    public void testTrapdoorDoubleRotation() {
        int meta = 1;
        int result = RotationUtils.rotateMeta(RotationType.TRAP_DOOR, 2, meta);
        assertEquals("trapdoor 180", 0, result);
    }

    @Test
    public void testTrapdoorDoubleRotationAll() {
        for (int meta = 0; meta < 16; meta++) {
            int expected = RotationUtils.rotateTrapdoor90(RotationUtils.rotateTrapdoor90(meta));
            assertEquals("meta " + meta, expected, RotationUtils.rotateMeta(RotationType.TRAP_DOOR, 2, meta));
        }
    }

    @Test
    public void testTrapdoorForwardRotationAll() {
        for (int meta = 0; meta < 16; meta++) {
            int expected = RotationUtils.rotateTrapdoor90(meta);
            assertEquals("meta " + meta, expected, RotationUtils.rotateMeta(RotationType.TRAP_DOOR, 1, meta));
        }
    }

    @Test
    public void testTrapdoorReverseRotationAll() {
        for (int meta = 0; meta < 16; meta++) {
            int expected = RotationUtils.rotateTrapdoor90Reverse(meta);
            assertEquals("meta " + meta, expected, RotationUtils.rotateMeta(RotationType.TRAP_DOOR, -1, meta));
        }
    }

    @Test
    public void testTrapdoorRotationClass() {
        TrapdoorRotation tr = new TrapdoorRotation();
        // forward rotation
        assertEquals(3, tr.rotate(0, 1));
        // reverse rotation
        assertEquals(2, tr.rotate(0, -1));
        // open state preserved
        assertEquals(7, tr.rotate(4, 1));
    }

    @Test
    public void testTrapdoorDefaultArrays() {
        TrapdoorRotation tr = RotationUtils.defaultTrapdoor();
        assertArrayEquals(new int[] { 0, 1, 2, 3 }, tr.getBottomClosed());
        assertArrayEquals(new int[] { 4, 5, 6, 7 }, tr.getBottomOpen());
        assertArrayEquals(new int[] { 8, 9, 10, 11 }, tr.getTopClosed());
        assertArrayEquals(new int[] { 12, 13, 14, 15 }, tr.getTopOpen());
    }
}
