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

import com.gtnewhorizon.gtnhlib.config.Config;
import com.sk89q.worldedit.LocalConfiguration;

/**
 * The options of {@code config/worldedit/worldedit.cfg}, one class per category.
 *
 * <p>
 * The rest of WorldEdit does not read these directly: {@link ForgeConfiguration} copies them to
 * {@link LocalConfiguration}. All fields are reloadable with {@code //we reload}.
 * </p>
 */
public final class WorldEditConfig {

    /** The id that reloads every option. */
    static final String RELOAD = "reload";

    static final String FILE_NAME = "worldedit";

    /** Every category of the file. */
    static final Class<?>[] CATEGORIES = { General.class, Permissions.class, Limits.class, Tools.class, Inventory.class,
        Files.class, Placement.class };

    private WorldEditConfig() {}

    @Config(
        modid = ForgeWorldEdit.MOD_ID,
        category = "general",
        configSubDirectory = ForgeWorldEdit.MOD_ID,
        filename = FILE_NAME)
    public static final class General {

        @Config.Comment("Show how long each edit took and how many blocks it changed.")
        @Config.DefaultBoolean(false)
        @Config.Reloadable(RELOAD)
        public static boolean profile;

        @Config.Comment("Also accept commands such as //set when they are typed with one slash, as /set.")
        @Config.DefaultBoolean(false)
        @Config.RequiresMcRestart
        public static boolean noDoubleSlash;

        @Config.Comment("Write the WorldEdit commands players use to the log file below.")
        @Config.DefaultBoolean(false)
        @Config.Reloadable(RELOAD)
        public static boolean logCommands;

        @Config.Comment("The file for logCommands, relative to this folder. Nothing is logged while this is empty.")
        @Config.DefaultString("")
        @Config.Reloadable(RELOAD)
        public static String logFile;
    }

    @Config(
        modid = ForgeWorldEdit.MOD_ID,
        category = "permissions",
        configSubDirectory = ForgeWorldEdit.MOD_ID,
        filename = FILE_NAME)
    public static final class Permissions {

        @Config.Comment("Let players in creative mode use WorldEdit without being an operator.")
        @Config.DefaultBoolean(false)
        @Config.Reloadable(RELOAD)
        public static boolean useInCreative;

        @Config.Comment("Let every player use WorldEdit.")
        @Config.DefaultBoolean(false)
        @Config.Reloadable(RELOAD)
        public static boolean cheatMode;
    }

    @Config(
        modid = ForgeWorldEdit.MOD_ID,
        category = "limits",
        configSubDirectory = ForgeWorldEdit.MOD_ID,
        filename = FILE_NAME)
    public static final class Limits {

        @Config.Comment("How many blocks one edit may change by default. -1 means no limit.")
        @Config.DefaultInt(-1)
        @Config.RangeInt(min = -1)
        @Config.Reloadable(RELOAD)
        public static int defaultChangeLimit;

        @Config.Comment("The highest limit players can choose with //limit. -1 means no limit.")
        @Config.DefaultInt(-1)
        @Config.RangeInt(min = -1)
        @Config.Reloadable(RELOAD)
        public static int maxChangeLimit;

        @Config.Comment("The largest radius of commands such as //fill. -1 means no limit.")
        @Config.DefaultInt(-1)
        @Config.RangeInt(min = -1)
        @Config.Reloadable(RELOAD)
        public static int maxRadius;

        @Config.Comment("The largest brush radius. -1 means no limit.")
        @Config.DefaultInt(6)
        @Config.RangeInt(min = -1)
        @Config.Reloadable(RELOAD)
        public static int maxBrushRadius;

        @Config.Comment("The largest range of the super pickaxe and the flood fill tool.")
        @Config.DefaultInt(5)
        @Config.RangeInt(min = 0)
        @Config.Reloadable(RELOAD)
        public static int maxSuperPickaxeSize;

        @Config.Comment("The radius of /butcher when none is typed. -1 means all loaded chunks.")
        @Config.DefaultInt(-1)
        @Config.RangeInt(min = -1)
        @Config.Reloadable(RELOAD)
        public static int butcherDefaultRadius;

        @Config.Comment("The largest radius of /butcher. -1 means no limit.")
        @Config.DefaultInt(-1)
        @Config.RangeInt(min = -1)
        @Config.Reloadable(RELOAD)
        public static int butcherMaxRadius;

        @Config.Comment("How many points a polygon selection may have by default. -1 means the maximum below.")
        @Config.DefaultInt(-1)
        @Config.RangeInt(min = -1)
        @Config.Reloadable(RELOAD)
        public static int defaultMaxPolygonPoints;

        @Config.Comment("The most points a polygon selection may have. -1 means no limit.")
        @Config.DefaultInt(20)
        @Config.RangeInt(min = -1)
        @Config.Reloadable(RELOAD)
        public static int maxPolygonPoints;

        @Config.Comment("How many points a convex selection may have by default. -1 means the maximum below.")
        @Config.DefaultInt(-1)
        @Config.RangeInt(min = -1)
        @Config.Reloadable(RELOAD)
        public static int defaultMaxPolyhedronPoints;

        @Config.Comment("The most points a convex selection may have. -1 means no limit.")
        @Config.DefaultInt(20)
        @Config.RangeInt(min = -1)
        @Config.Reloadable(RELOAD)
        public static int maxPolyhedronPoints;

        @Config.Comment("How many edits each player can undo. At least 15.")
        @Config.DefaultInt(15)
        @Config.RangeInt(min = 15)
        @Config.Reloadable(RELOAD)
        public static int historySize;

        @Config.Comment({ "The most changed blocks each player's undo history may hold, or -1 for no limit.",
            "The oldest edits are forgotten past this. The newest edit is always kept, however large.",
            "Each block takes about 30 bytes, more for blocks with tile entity data." })
        @Config.DefaultInt(10000000)
        @Config.RangeInt(min = -1)
        @Config.Reloadable(RELOAD)
        public static int historyBlockLimit;

        @Config.Comment("The IDs of the blocks players can not place with WorldEdit.")
        @Config.Reloadable(RELOAD)
        public static int[] disallowedBlocks = LocalConfiguration.defaultDisallowedBlocks;
    }

    @Config(
        modid = ForgeWorldEdit.MOD_ID,
        category = "tools",
        configSubDirectory = ForgeWorldEdit.MOD_ID,
        filename = FILE_NAME)
    public static final class Tools {

        @Config.Comment("The item ID of the selection wand. 271 is the wooden axe. 0 turns the wand off.")
        @Config.DefaultInt(271)
        @Config.RangeInt(min = 0)
        @Config.Reloadable(RELOAD)
        public static int wandItem;

        @Config.Comment({ "Only wands given by //wand and //wand -n work, so ordinary items of the same type, such as",
            "the wooden axes everyone uses early on, behave normally. When false, any item of the wand's type",
            "is a wand. Either way, only players with permission can use a wand." })
        @Config.DefaultBoolean(true)
        @Config.Reloadable(RELOAD)
        public static boolean requireWandTag;

        @Config.Comment("Blocks broken with the single block super pickaxe drop as items.")
        @Config.DefaultBoolean(true)
        @Config.Reloadable(RELOAD)
        public static boolean superPickaxeDropItems;

        @Config.Comment("Blocks broken with the area and recursive super pickaxes drop as items.")
        @Config.DefaultBoolean(true)
        @Config.Reloadable(RELOAD)
        public static boolean superPickaxeManyDropItems;

        @Config.Comment("The item ID of the navigation wand. 345 is the compass. 0 turns the wand off.")
        @Config.DefaultInt(345)
        @Config.RangeInt(min = 0)
        @Config.Reloadable(RELOAD)
        public static int navigationWandItem;

        @Config.Comment("How far the navigation wand teleports.")
        @Config.DefaultInt(50)
        @Config.RangeInt(min = 0)
        @Config.Reloadable(RELOAD)
        public static int navigationWandDistance;

        @Config.Comment("Place glass under players who use /up or /ceil, unless they ask otherwise.")
        @Config.DefaultBoolean(true)
        @Config.Reloadable(RELOAD)
        public static boolean navigationUseGlass;
    }

    @Config(
        modid = ForgeWorldEdit.MOD_ID,
        category = "inventory",
        configSubDirectory = ForgeWorldEdit.MOD_ID,
        filename = FILE_NAME)
    public static final class Inventory {

        @Config.Comment("Edits take blocks from the inventory of the player and give removed blocks back.")
        @Config.DefaultBoolean(false)
        @Config.Reloadable(RELOAD)
        public static boolean useInventory;

        @Config.Comment("Players with the worldedit.inventory.unrestricted permission do not use their inventory.")
        @Config.DefaultBoolean(false)
        @Config.Reloadable(RELOAD)
        public static boolean useInventoryOverride;

        @Config.Comment("Players in creative mode do not use their inventory.")
        @Config.DefaultBoolean(false)
        @Config.Reloadable(RELOAD)
        public static boolean useInventoryCreativeOverride;
    }

    @Config(
        modid = ForgeWorldEdit.MOD_ID,
        category = "files",
        configSubDirectory = ForgeWorldEdit.MOD_ID,
        filename = FILE_NAME)
    public static final class Files {

        @Config.Comment("The folder for schematics, relative to this folder.")
        @Config.DefaultString("schematics")
        @Config.Reloadable(RELOAD)
        public static String schematicSaveDir;

        @Config.Comment({ "The folder that holds world backups for //restore, relative to the game folder.",
            "The default is where ServerUtilities saves its backups. Restoring is off while this is empty." })
        @Config.DefaultString("backups")
        @Config.Reloadable(RELOAD)
        public static String snapshotsDir;

        @Config.Comment("Allow file names to follow symbolic links out of the folders above.")
        @Config.DefaultBoolean(false)
        @Config.Reloadable(RELOAD)
        public static boolean allowSymbolicLinks;
    }

    @Config(
        modid = ForgeWorldEdit.MOD_ID,
        category = "placement",
        configSubDirectory = ForgeWorldEdit.MOD_ID,
        filename = FILE_NAME)
    public static final class Placement {

        @Config.Comment("Names of modded blocks to place after all others, because they attach to their neighbours.")
        @Config.DefaultStringList({})
        @Config.RequiresMcRestart
        public static String[] placeLastBlocks;

        @Config.Comment("Names of modded blocks to place in the normal order, even if they look like attached blocks.")
        @Config.DefaultStringList({})
        @Config.RequiresMcRestart
        public static String[] placeNormallyBlocks;
    }
}
