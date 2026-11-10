/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.forge;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Brings the values of the old {@code worldedit.properties} over to {@link WorldEditConfig}, once.
 *
 * <p>
 * A value that can not be read keeps the default, as it did in the old file.
 * </p>
 */
final class LegacyConfigImport {

    static final String LEGACY_FILE = "worldedit.properties";
    static final String MIGRATED_FILE = LEGACY_FILE + ".migrated";

    private static final Logger log = Logger.getLogger(LegacyConfigImport.class.getCanonicalName());

    private LegacyConfigImport() {}

    /**
     * @return true if the old file holds values that the new file does not have yet
     */
    static boolean isNeeded(File workingDir) {
        return new File(workingDir, LEGACY_FILE).isFile()
            && !new File(workingDir, WorldEditConfig.FILE_NAME + ".cfg").exists();
    }

    /**
     * Reads the old file into {@link WorldEditConfig}.
     *
     * @return true if it could be read
     */
    static boolean read(File workingDir) {
        Properties properties = new Properties();
        try (InputStream stream = new FileInputStream(new File(workingDir, LEGACY_FILE))) {
            properties.load(stream);
        } catch (IOException e) {
            log.log(Level.WARNING, "Could not read " + LEGACY_FILE + ", so it was not imported", e);
            return false;
        }
        apply(properties);
        return true;
    }

    /**
     * Renames the old file, to show that it is not used any more.
     */
    static void retire(File workingDir) {
        File legacy = new File(workingDir, LEGACY_FILE);
        if (legacy.renameTo(new File(workingDir, MIGRATED_FILE))) {
            log.info(
                "Imported " + LEGACY_FILE
                    + " into "
                    + WorldEditConfig.FILE_NAME
                    + ".cfg and renamed it to "
                    + MIGRATED_FILE);
        } else {
            log.warning("Imported " + LEGACY_FILE + " but could not rename it. It is not used any more.");
        }
    }

    /**
     * Every key the old file could hold and the option that replaces it. {@code register-help} and
     * {@code allow-extra-data-values} did nothing, so they are not carried over.
     */
    static void apply(Properties old) {
        bool(old, "profile", v -> WorldEditConfig.General.profile = v);
        bool(old, "no-double-slash", v -> WorldEditConfig.General.noDoubleSlash = v);
        bool(old, "log-commands", v -> WorldEditConfig.General.logCommands = v);
        string(old, "log-file", v -> WorldEditConfig.General.logFile = v);

        bool(old, "use-in-creative", v -> WorldEditConfig.Permissions.useInCreative = v);
        bool(old, "cheat-mode", v -> WorldEditConfig.Permissions.cheatMode = v);

        integer(old, "default-max-changed-blocks", v -> WorldEditConfig.Limits.defaultChangeLimit = v);
        integer(old, "max-changed-blocks", v -> WorldEditConfig.Limits.maxChangeLimit = v);
        integer(old, "max-radius", v -> WorldEditConfig.Limits.maxRadius = v);
        integer(old, "max-brush-radius", v -> WorldEditConfig.Limits.maxBrushRadius = v);
        integer(old, "max-super-pickaxe-size", v -> WorldEditConfig.Limits.maxSuperPickaxeSize = v);
        integer(old, "butcher-default-radius", v -> WorldEditConfig.Limits.butcherDefaultRadius = v);
        integer(old, "butcher-max-radius", v -> WorldEditConfig.Limits.butcherMaxRadius = v);
        integer(old, "default-max-polygon-points", v -> WorldEditConfig.Limits.defaultMaxPolygonPoints = v);
        integer(old, "max-polygon-points", v -> WorldEditConfig.Limits.maxPolygonPoints = v);
        integer(old, "default-max-polyhedron-points", v -> WorldEditConfig.Limits.defaultMaxPolyhedronPoints = v);
        integer(old, "max-polyhedron-points", v -> WorldEditConfig.Limits.maxPolyhedronPoints = v);
        // The old file raised anything lower to 15 without saying so
        integer(old, "history-size", v -> WorldEditConfig.Limits.historySize = Math.max(15, v));
        if (old.getProperty("disallowed-blocks") != null) {
            WorldEditConfig.Limits.disallowedBlocks = readIds(old.getProperty("disallowed-blocks"));
        }

        integer(old, "wand-item", v -> WorldEditConfig.Tools.wandItem = v);
        bool(old, "super-pickaxe-drop-items", v -> WorldEditConfig.Tools.superPickaxeDropItems = v);
        bool(old, "super-pickaxe-many-drop-items", v -> WorldEditConfig.Tools.superPickaxeManyDropItems = v);
        integer(old, "nav-wand-item", v -> WorldEditConfig.Tools.navigationWandItem = v);
        integer(old, "nav-wand-distance", v -> WorldEditConfig.Tools.navigationWandDistance = v);
        bool(old, "nav-use-glass", v -> WorldEditConfig.Tools.navigationUseGlass = v);

        string(old, "schematic-save-dir", v -> WorldEditConfig.Files.schematicSaveDir = v);
        string(old, "snapshots-dir", v -> WorldEditConfig.Files.snapshotsDir = v);
        bool(old, "allow-symbolic-links", v -> WorldEditConfig.Files.allowSymbolicLinks = v);

        if (old.getProperty("place-last-blocks") != null) {
            WorldEditConfig.Placement.placeLastBlocks = readNames(old.getProperty("place-last-blocks"));
        }
        if (old.getProperty("place-normally-blocks") != null) {
            WorldEditConfig.Placement.placeNormallyBlocks = readNames(old.getProperty("place-normally-blocks"));
        }
    }

    private static void bool(Properties old, String key, Consumer<Boolean> target) {
        String value = old.getProperty(key);
        if (value != null) {
            target.accept(value.equalsIgnoreCase("true") || value.equals("1"));
        }
    }

    private static void integer(Properties old, String key, Consumer<Integer> target) {
        String value = old.getProperty(key);
        if (value != null) {
            try {
                target.accept(Integer.parseInt(value.trim()));
            } catch (NumberFormatException e) {
                log.warning("Ignored the value '" + value + "' of " + key + " in " + LEGACY_FILE);
            }
        }
    }

    private static void string(Properties old, String key, Consumer<String> target) {
        String value = old.getProperty(key);
        if (value != null) {
            target.accept(value);
        }
    }

    private static int[] readIds(String list) {
        List<Integer> ids = new ArrayList<>();
        for (String part : list.split(",")) {
            try {
                ids.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignored) {
                // The old file skipped these too
            }
        }
        int[] result = new int[ids.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = ids.get(i);
        }
        return result;
    }

    private static String[] readNames(String list) {
        List<String> names = new ArrayList<>();
        for (String name : list.split(",")) {
            if (!name.trim()
                .isEmpty()) {
                names.add(name.trim());
            }
        }
        return names.toArray(new String[0]);
    }
}
