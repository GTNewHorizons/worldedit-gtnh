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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.world.snapshot.SnapshotRepository;

/**
 * Loads {@code worldedit.cfg} through GTNHLib, and passes the values on to the fields of
 * {@link LocalConfiguration} that the rest of WorldEdit reads.
 */
public class ForgeConfiguration extends LocalConfiguration {

    /** GTNHLib keeps the file open for the whole game, so it can only be registered once. */
    private static boolean registered = false;

    private final File workingDir;

    public boolean creativeEnable = false;
    public boolean cheatMode = false;
    public Set<String> placeLastBlocks = new HashSet<String>();
    public Set<String> placeNormallyBlocks = new HashSet<String>();
    public Set<String> naturalTerrainBlocks = new HashSet<String>();
    public Set<String> notNaturalTerrainBlocks = new HashSet<String>();

    public ForgeConfiguration(ForgeWorldEdit mod) {
        this(mod.getWorkingDir());
    }

    ForgeConfiguration(File workingDir) {
        this.workingDir = workingDir;
    }

    @Override
    public void load() {
        if (registered) {
            reload();
        } else {
            register();
            registered = true;
        }
        copyValues();
    }

    /**
     * Whether an import is needed has to be asked first, because registering creates the new file.
     */
    private void register() {
        boolean importLegacy = LegacyConfigImport.isNeeded(workingDir);
        try {
            for (Class<?> category : WorldEditConfig.CATEGORIES) {
                ConfigurationManager.registerConfig(category);
            }
        } catch (ConfigException e) {
            throw new IllegalStateException("Could not load the WorldEdit configuration", e);
        }
        if (importLegacy && LegacyConfigImport.read(workingDir)) {
            ConfigurationManager.save(WorldEditConfig.CATEGORIES);
            LegacyConfigImport.retire(workingDir);
            // The imported values may be outside the allowed ranges, reading them back corrects them
            reload();
        }
    }

    private static void reload() {
        for (Class<?> category : WorldEditConfig.CATEGORIES) {
            ConfigurationManager.reloadConfig(category, WorldEditConfig.RELOAD);
        }
    }

    private void copyValues() {
        profile = WorldEditConfig.General.profile;
        logCommands = WorldEditConfig.General.logCommands;
        logFile = WorldEditConfig.General.logFile;

        creativeEnable = WorldEditConfig.Permissions.useInCreative;
        cheatMode = WorldEditConfig.Permissions.cheatMode;

        defaultChangeLimit = WorldEditConfig.Limits.defaultChangeLimit;
        maxChangeLimit = WorldEditConfig.Limits.maxChangeLimit;
        maxRadius = WorldEditConfig.Limits.maxRadius;
        maxBrushRadius = WorldEditConfig.Limits.maxBrushRadius;
        maxSuperPickaxeSize = WorldEditConfig.Limits.maxSuperPickaxeSize;
        butcherDefaultRadius = WorldEditConfig.Limits.butcherDefaultRadius;
        butcherMaxRadius = WorldEditConfig.Limits.butcherMaxRadius;
        defaultMaxPolygonalPoints = WorldEditConfig.Limits.defaultMaxPolygonPoints;
        maxPolygonalPoints = WorldEditConfig.Limits.maxPolygonPoints;
        defaultMaxPolyhedronPoints = WorldEditConfig.Limits.defaultMaxPolyhedronPoints;
        maxPolyhedronPoints = WorldEditConfig.Limits.maxPolyhedronPoints;
        LocalSession.MAX_HISTORY_SIZE = WorldEditConfig.Limits.historySize;
        LocalSession.MAX_HISTORY_BLOCKS = WorldEditConfig.Limits.historyBlockLimit;
        disallowedBlocks = new HashSet<Integer>();
        for (int id : WorldEditConfig.Limits.disallowedBlocks) {
            disallowedBlocks.add(id);
        }

        wandItem = WorldEditConfig.Tools.wandItem;
        requireWandTag = WorldEditConfig.Tools.requireWandTag;
        superPickaxeDrop = WorldEditConfig.Tools.superPickaxeDropItems;
        superPickaxeManyDrop = WorldEditConfig.Tools.superPickaxeManyDropItems;
        navigationWand = WorldEditConfig.Tools.navigationWandItem;
        navigationWandMaxDistance = WorldEditConfig.Tools.navigationWandDistance;
        navigationUseGlass = WorldEditConfig.Tools.navigationUseGlass;

        saveDir = WorldEditConfig.Files.schematicSaveDir;
        snapshotRepo = WorldEditConfig.Files.snapshotsDir.isEmpty() ? null
            : new SnapshotRepository(WorldEditConfig.Files.snapshotsDir);
        allowSymlinks = WorldEditConfig.Files.allowSymbolicLinks;

        placeLastBlocks = new HashSet<String>(Arrays.asList(WorldEditConfig.Placement.placeLastBlocks));
        placeNormallyBlocks = new HashSet<String>(Arrays.asList(WorldEditConfig.Placement.placeNormallyBlocks));
        naturalTerrainBlocks = new HashSet<String>(Arrays.asList(WorldEditConfig.Placement.naturalTerrainBlocks));
        notNaturalTerrainBlocks = new HashSet<String>(Arrays.asList(WorldEditConfig.Placement.notNaturalTerrainBlocks));
    }

    @Override
    public File getWorkingDirectory() {
        return workingDir;
    }
}
