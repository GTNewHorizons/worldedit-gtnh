package com.sk89q.worldedit.forge;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;

import net.minecraft.launchwrapper.Launch;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.sk89q.worldedit.LocalSession;

import cpw.mods.fml.relauncher.FMLInjectionData;

/**
 * Runs the real GTNHLib config manager on a temporary game folder. The manager keeps its file for the whole JVM, so the
 * lifecycle of one file is a single test.
 */
public class ForgeConfigurationTest {

    @ClassRule
    public static final TemporaryFolder game = new TemporaryFolder();

    private static File workingDir;

    @BeforeClass
    public static void pointTheGameAtATemporaryFolder() throws Exception {
        Launch.minecraftHome = game.getRoot();
        Field home = FMLInjectionData.class.getDeclaredField("minecraftHome");
        home.setAccessible(true);
        home.set(null, game.getRoot());
        Field major = FMLInjectionData.class.getDeclaredField("major");
        major.setAccessible(true);
        major.set(null, "");
        workingDir = new File(game.getRoot(), "config/worldedit");
        assertTrue(workingDir.mkdirs());
    }

    private static File file(String name) {
        return new File(workingDir, name);
    }

    private static void replaceInConfig(String from, String to) throws Exception {
        File config = file("worldedit.cfg");
        String text = new String(Files.readAllBytes(config.toPath()), StandardCharsets.UTF_8);
        assertTrue(from + " in:\n" + text, text.contains(from));
        Files.write(
            config.toPath(),
            text.replace(from, to)
                .getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void importsTheOldFileOnceAndReloadsTheNewOne() throws Exception {
        Properties old = new Properties();
        old.setProperty("max-radius", "100");
        old.setProperty("max-brush-radius", "not a number");
        old.setProperty("cheat-mode", "1");
        old.setProperty("use-in-creative", "TRUE");
        old.setProperty("disallowed-blocks", "1, 2,x,3");
        old.setProperty("place-last-blocks", " a:b ,,c:d");
        old.setProperty("schematic-save-dir", "my-schematics");
        old.setProperty("snapshots-dir", "old-backups");
        old.setProperty("history-size", "5");
        old.setProperty("register-help", "true");
        try (OutputStream stream = new FileOutputStream(file("worldedit.properties"))) {
            old.store(stream, null);
        }

        // First start: the values come over and the old file is put aside
        ForgeConfiguration config = new ForgeConfiguration(workingDir);
        config.load();

        assertEquals(100, config.maxRadius);
        assertEquals("a bad number keeps the default", 6, config.maxBrushRadius);
        assertEquals("an absent key keeps the default", 5, config.maxSuperPickaxeSize);
        assertTrue(config.cheatMode);
        assertTrue(config.creativeEnable);
        assertEquals(new HashSet<>(java.util.Arrays.asList(1, 2, 3)), config.disallowedBlocks);
        assertEquals(new HashSet<>(java.util.Arrays.asList("a:b", "c:d")), config.placeLastBlocks);
        assertEquals(Collections.emptySet(), config.placeNormallyBlocks);
        assertEquals("my-schematics", config.saveDir);
        assertNotNull(config.snapshotRepo);
        assertEquals("the minimum still applies", 15, LocalSession.MAX_HISTORY_SIZE);

        assertTrue(file("worldedit.cfg").isFile());
        assertFalse(file("worldedit.properties").exists());
        assertTrue(file("worldedit.properties.migrated").isFile());

        // The values were written to the new file
        String text = new String(Files.readAllBytes(file("worldedit.cfg").toPath()), StandardCharsets.UTF_8);
        assertTrue(text, text.contains("I:maxRadius=100"));
        assertTrue(text, text.contains("S:schematicSaveDir=my-schematics"));
        assertTrue(text, text.contains("B:cheatMode=true"));
        assertTrue(text, text.contains("I:historySize=15"));
        assertFalse(text, text.contains("registerHelp"));

        // //we reload picks up edits to the new file, and an emptied snapshot folder turns restoring off
        replaceInConfig("I:maxRadius=100", "I:maxRadius=200");
        replaceInConfig("S:snapshotsDir=old-backups", "S:snapshotsDir=");
        config.load();

        assertEquals(200, config.maxRadius);
        assertNull(config.snapshotRepo);

        // Values out of range are corrected
        replaceInConfig("I:maxRadius=200", "I:maxRadius=-5");
        config.load();
        assertEquals(-1, config.maxRadius);

        // A later file with the old name is not imported over the new one
        try (OutputStream stream = new FileOutputStream(file("worldedit.properties"))) {
            old.store(stream, null);
        }
        assertFalse(LegacyConfigImport.isNeeded(workingDir));
    }

    @Test
    public void readingBlockIdsIgnoresWhatIsNotANumber() {
        int[] before = WorldEditConfig.Limits.disallowedBlocks;
        try {
            Properties old = new Properties();
            old.setProperty("disallowed-blocks", "7,,oops, 8");
            LegacyConfigImport.apply(old);
            assertArrayEquals(new int[] { 7, 8 }, WorldEditConfig.Limits.disallowedBlocks);
        } finally {
            WorldEditConfig.Limits.disallowedBlocks = before;
        }
    }
}
