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

package com.sk89q.worldedit.command;

import static com.sk89q.worldedit.command.brigadier.CommandRunner.run;
import static com.sk89q.worldedit.command.brigadier.Nodes.integer;
import static com.sk89q.worldedit.command.brigadier.Nodes.literal;
import static com.sk89q.worldedit.command.brigadier.Nodes.text;
import static com.sk89q.worldedit.command.brigadier.Nodes.word;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.command.ICommandSender;

import com.google.common.base.Optional;
import com.mojang.brigadier.CommandDispatcher;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.command.brigadier.CommandCall;
import com.sk89q.worldedit.command.brigadier.CommandFailure;
import com.sk89q.worldedit.command.brigadier.LogMode;
import com.sk89q.worldedit.command.brigadier.Nodes;
import com.sk89q.worldedit.command.util.CommandInput;
import com.sk89q.worldedit.command.util.RelativeCoordinates;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.permission.ActorSelectorLimits;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionOperationException;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.ConvexPolyhedralRegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.regions.selector.CylinderRegionSelector;
import com.sk89q.worldedit.regions.selector.EllipsoidRegionSelector;
import com.sk89q.worldedit.regions.selector.ExtendingCuboidRegionSelector;
import com.sk89q.worldedit.regions.selector.Polygonal2DRegionSelector;
import com.sk89q.worldedit.regions.selector.RegionSelectorType;
import com.sk89q.worldedit.regions.selector.SphereRegionSelector;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.Countable;
import com.sk89q.worldedit.util.formatting.ColorCodeBuilder;
import com.sk89q.worldedit.util.formatting.Style;
import com.sk89q.worldedit.util.formatting.StyledFragment;
import com.sk89q.worldedit.util.formatting.component.CommandListBox;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.storage.ChunkStore;

/**
 * Selection commands.
 */
public final class SelectionCommands {

    private SelectionCommands() {}

    // spotless:off
    public static void register(CommandDispatcher<ICommandSender> dispatcher) {
        Nodes.register(dispatcher, literal("/pos1", "Set position 1")
                .executes(run("worldedit.selection.pos", LogMode.POSITION, c -> pos1(c.player(), c.session(), c)))
                .then(text("coordinates")
                        .executes(run("worldedit.selection.pos", LogMode.POSITION, c -> pos1(c.player(), c.session(), c)))));

        Nodes.register(dispatcher, literal("/pos2", "Set position 2")
                .executes(run("worldedit.selection.pos", LogMode.POSITION, c -> pos2(c.player(), c.session(), c)))
                .then(text("coordinates")
                        .executes(run("worldedit.selection.pos", LogMode.POSITION, c -> pos2(c.player(), c.session(), c)))));

        Nodes.register(dispatcher, literal("/hpos1", "Set position 1 to targeted block")
                .executes(run("worldedit.selection.hpos", c -> hpos1(c.player(), c.session(), c))));

        Nodes.register(dispatcher, literal("/hpos2", "Set position 2 to targeted block")
                .executes(run("worldedit.selection.hpos", c -> hpos2(c.player(), c.session(), c))));

        Nodes.registerWithFlags(dispatcher, literal("/chunk", "Set the selection to your current chunk")
                .executes(run("worldedit.selection.chunk", LogMode.POSITION, c -> chunk(c.player(), c.session(), c)))
                .then(text("coordinates")
                        .executes(run("worldedit.selection.chunk", LogMode.POSITION, c -> chunk(c.player(), c.session(), c)))),
                "sc");

        Nodes.registerWithFlags(dispatcher, literal("/wand", "Get the selection wand, or the navigation wand with -n")
                .executes(run("worldedit.wand", c -> wand(c.player(), c.flag('n')))),
                "n");

        Nodes.register(dispatcher, literal("/expand", "Expand the selection area").executes(Nodes.USAGE)
                .then(literal("vert")
                        .executes(run("worldedit.selection.expand", LogMode.REGION, c -> expandVertical(c.player(), c.session()))))
                .then(literal("vertical")
                        .executes(run("worldedit.selection.expand", LogMode.REGION, c -> expandVertical(c.player(), c.session()))))
                .then(integer("amount")
                        .executes(run("worldedit.selection.expand", LogMode.REGION, c -> expand(c.player(), c.session(), c)))
                        .then(word("reverse-or-direction")
                                .executes(run("worldedit.selection.expand", LogMode.REGION, c -> expand(c.player(), c.session(), c)))
                                .then(word("direction")
                                        .executes(run("worldedit.selection.expand", LogMode.REGION, c -> expand(c.player(), c.session(), c)))))));

        Nodes.register(dispatcher, literal("/contract", "Contract the selection area").executes(Nodes.USAGE)
                .then(integer("amount")
                        .executes(run("worldedit.selection.contract", LogMode.REGION, c -> contract(c.player(), c.session(), c)))
                        .then(word("reverse-or-direction")
                                .executes(run("worldedit.selection.contract", LogMode.REGION, c -> contract(c.player(), c.session(), c)))
                                .then(word("direction")
                                        .executes(run("worldedit.selection.contract", LogMode.REGION, c -> contract(c.player(), c.session(), c)))))));

        Nodes.register(dispatcher, literal("/shift", "Shift the selection area").executes(Nodes.USAGE)
                .then(integer("amount")
                        .executes(run("worldedit.selection.shift", LogMode.REGION, c -> shift(c.player(), c.session(), c)))
                        .then(word("direction")
                                .executes(run("worldedit.selection.shift", LogMode.REGION, c -> shift(c.player(), c.session(), c))))));

        Nodes.registerWithFlags(dispatcher, literal("/outset", "Outset the selection area").executes(Nodes.USAGE)
                .then(integer("amount")
                        .executes(run("worldedit.selection.outset", LogMode.REGION, c -> outset(c.player(), c.session(), c)))),
                "hv");

        Nodes.registerWithFlags(dispatcher, literal("/inset", "Inset the selection area").executes(Nodes.USAGE)
                .then(integer("amount")
                        .executes(run("worldedit.selection.inset", LogMode.REGION, c -> inset(c.player(), c.session(), c)))),
                "hv");

        Nodes.registerWithFlags(dispatcher, literal("/size", "Get information about the selection")
                .executes(run("worldedit.selection.size", c -> size(c.player(), c.session(), c))),
                "c");

        Nodes.registerWithFlags(dispatcher, literal("/count", "Counts the number of a certain type of block").executes(Nodes.USAGE)
                .then(word("block")
                        .executes(run("worldedit.analysis.count", c -> count(c.player(), c.session(), c)))),
                "d");

        Nodes.registerWithFlags(dispatcher, literal("/distr", "Get the distribution of blocks in the selection")
                .executes(run("worldedit.analysis.distr", c -> distr(c.player(), c.session(), c))),
                "cd");

        Nodes.registerWithFlags(dispatcher, literal("/sel", "Choose a region selector")
                .executes(run(null, c -> select(c.player(), c.session(), c)))
                .then(word("type", "cuboid", "extend", "poly", "ellipsoid", "sphere", "cyl", "convex")
                        .executes(run(null, c -> select(c.player(), c.session(), c)))),
                "d", ";", "/desel", "/deselect");
    }
    // spotless:on

    /**
     * @return the typed coordinates, or the block the player is in if none were typed, or null if they are invalid
     */
    private static Vector position(Player player, CommandCall args) {
        if (!args.has("coordinates")) {
            return player.getBlockIn();
        }
        Vector here = player.getBlockIn();
        try {
            int[] coords = RelativeCoordinates
                .parse(args.string("coordinates"), here.getBlockX(), here.getBlockY(), here.getBlockZ());
            return new Vector(coords[0], coords[1], coords[2]);
        } catch (IllegalArgumentException e) {
            player.printError(e.getMessage());
            return null;
        }
    }

    private static void pos1(Player player, LocalSession session, CommandCall args) throws Exception {

        Vector pos = position(player, args);
        if (pos == null) {
            return;
        }

        if (!session.getRegionSelector(player.getWorld())
            .selectPrimary(pos, ActorSelectorLimits.forActor(player))) {
            player.printError("Position already set.");
            return;
        }

        session.getRegionSelector(player.getWorld())
            .explainPrimarySelection(player, session, pos);
    }

    private static void pos2(Player player, LocalSession session, CommandCall args) throws Exception {

        Vector pos = position(player, args);
        if (pos == null) {
            return;
        }

        if (!session.getRegionSelector(player.getWorld())
            .selectSecondary(pos, ActorSelectorLimits.forActor(player))) {
            player.printError("Position already set.");
            return;
        }

        session.getRegionSelector(player.getWorld())
            .explainSecondarySelection(player, session, pos);
    }

    private static void hpos1(Player player, LocalSession session, CommandCall args) throws Exception {

        Vector pos = player.getBlockTrace(300);

        if (pos != null) {
            if (!session.getRegionSelector(player.getWorld())
                .selectPrimary(pos, ActorSelectorLimits.forActor(player))) {
                player.printError("Position already set.");
                return;
            }

            session.getRegionSelector(player.getWorld())
                .explainPrimarySelection(player, session, pos);
        } else {
            player.printError("No block in sight!");
        }
    }

    private static void hpos2(Player player, LocalSession session, CommandCall args) throws Exception {

        Vector pos = player.getBlockTrace(300);

        if (pos != null) {
            if (!session.getRegionSelector(player.getWorld())
                .selectSecondary(pos, ActorSelectorLimits.forActor(player))) {
                player.printError("Position already set.");
                return;
            }

            session.getRegionSelector(player.getWorld())
                .explainSecondarySelection(player, session, pos);
        } else {
            player.printError("No block in sight!");
        }
    }

    private static void chunk(Player player, LocalSession session, CommandCall args) throws Exception {
        final Vector min;
        final Vector max;
        final World world = player.getWorld();
        if (args.flag('s')) {
            Region region = session.getSelection(world);

            final Vector2D min2D = ChunkStore.toChunk(region.getMinimumPoint());
            final Vector2D max2D = ChunkStore.toChunk(region.getMaximumPoint());

            min = new Vector(min2D.getBlockX() * 16, 0, min2D.getBlockZ() * 16);
            max = new Vector(max2D.getBlockX() * 16 + 15, world.getMaxY(), max2D.getBlockZ() * 16 + 15);

            player.print(
                "Chunks selected: (" + min2D.getBlockX()
                    + ", "
                    + min2D.getBlockZ()
                    + ") - ("
                    + max2D.getBlockX()
                    + ", "
                    + max2D.getBlockZ()
                    + ")");
        } else {
            final Vector2D min2D;
            if (args.has("coordinates")) {
                // coords specified
                // ~ is relative to the chunk you are in with -c, and to your block otherwise.
                Vector here = player.getBlockIn();
                Vector2D origin = args.flag('c') ? ChunkStore.toChunk(here)
                    : new Vector2D(here.getBlockX(), here.getBlockZ());
                int[] coords;
                try {
                    coords = RelativeCoordinates
                        .parse(args.string("coordinates"), origin.getBlockX(), origin.getBlockZ());
                } catch (IllegalArgumentException e) {
                    throw new InsufficientArgumentsException(e.getMessage());
                }
                Vector2D pos = new Vector2D(coords[0], coords[1]);
                min2D = (args.flag('c')) ? pos : ChunkStore.toChunk(pos.toVector());
            } else {
                // use player loc
                min2D = ChunkStore.toChunk(player.getBlockIn());
            }

            min = new Vector(min2D.getBlockX() * 16, 0, min2D.getBlockZ() * 16);
            max = min.add(15, world.getMaxY(), 15);

            player.print("Chunk selected: " + min2D.getBlockX() + ", " + min2D.getBlockZ());
        }

        final CuboidRegionSelector selector;
        if (session.getRegionSelector(world) instanceof ExtendingCuboidRegionSelector) {
            selector = new ExtendingCuboidRegionSelector(world);
        } else {
            selector = new CuboidRegionSelector(world);
        }
        selector.selectPrimary(min, ActorSelectorLimits.forActor(player));
        selector.selectSecondary(max, ActorSelectorLimits.forActor(player));
        session.setRegionSelector(world, selector);

        session.dispatchCUISelection(player);

    }

    static void wand(Player player, boolean navigation) {
        LocalConfiguration config = WorldEdit.getInstance()
            .getConfiguration();
        int item = navigation ? config.navigationWand : config.wandItem;
        if (item <= 0 || !player.giveWand(
            item,
            navigation ? Player.NAVIGATION_WAND : Player.SELECTION_WAND,
            navigation ? "WorldEdit Navigation Wand" : "WorldEdit Wand")) {
            player.printError((navigation ? "The navigation wand" : "The wand") + " is turned off on this server.");
            return;
        }

        if (navigation) {
            player.print("Left click: jump to where you look; Right click: pass through walls");
        } else {
            player.print("Left click: select pos #1; Right click: select pos #2");
        }
    }

    private static void expand(Player player, LocalSession session, CommandCall args) throws Exception {
        DirectionChange change = readChange(player, args);

        Region region = session.getSelection(player.getWorld());
        int oldSize = region.getArea();

        if (change.reverse == 0) {
            for (Vector dir : change.directions) {
                region.expand(dir.multiply(change.amount));
            }
        } else {
            for (Vector dir : change.directions) {
                region.expand(dir.multiply(change.amount), dir.multiply(-change.reverse));
            }
        }

        session.getRegionSelector(player.getWorld())
            .learnChanges();
        int newSize = region.getArea();

        session.getRegionSelector(player.getWorld())
            .explainRegionAdjust(player, session);

        player.print("Region expanded " + (newSize - oldSize) + " blocks.");
    }

    /**
     * {@code //expand vert}: expand the selection between sky and bedrock.
     */
    private static void expandVertical(Player player, LocalSession session) throws Exception {
        Region region = session.getSelection(player.getWorld());
        try {
            int oldSize = region.getArea();
            int height = player.getWorld()
                .getMaxY() + 1;
            region.expand(new Vector(0, height, 0), new Vector(0, -height, 0));
            session.getRegionSelector(player.getWorld())
                .learnChanges();
            int newSize = region.getArea();
            session.getRegionSelector(player.getWorld())
                .explainRegionAdjust(player, session);
            player.print("Region expanded " + (newSize - oldSize) + " blocks [top-to-bottom].");
        } catch (RegionOperationException e) {
            player.printError(e.getMessage());
        }
    }

    /**
     * What was typed after the amount of {@code //expand} and {@code //contract}: {@code [reverse-amount] [direction]}.
     */
    private static final class DirectionChange {

        int amount;
        int reverse;
        List<Vector> directions = new ArrayList<Vector>();
    }

    private static DirectionChange readChange(Player player, CommandCall args) throws Exception {
        DirectionChange change = new DirectionChange();
        change.amount = args.integer("amount");
        if (args.has("direction")) {
            // Both reverse amount and direction
            change.reverse = Integer.parseInt(args.string("reverse-or-direction"));
            addDirections(player, args.string("direction"), change.directions);
        } else if (args.has("reverse-or-direction")) {
            // Either a reverse amount or a direction
            String word = args.string("reverse-or-direction");
            try {
                change.reverse = Integer.parseInt(word);
                change.directions.add(
                    WorldEdit.getInstance()
                        .getDirection(player, "me"));
            } catch (NumberFormatException e) {
                addDirections(player, word, change.directions);
            }
        } else {
            change.directions.add(
                WorldEdit.getInstance()
                    .getDirection(player, "me"));
        }
        return change;
    }

    private static void addDirections(Player player, String list, List<Vector> directions) throws Exception {
        for (String direction : list.split(",")) {
            directions.add(
                WorldEdit.getInstance()
                    .getDirection(player, direction.toLowerCase()));
        }
    }

    private static void contract(Player player, LocalSession session, CommandCall args) throws Exception {
        DirectionChange change = readChange(player, args);

        try {
            Region region = session.getSelection(player.getWorld());
            int oldSize = region.getArea();
            if (change.reverse == 0) {
                for (Vector dir : change.directions) {
                    region.contract(dir.multiply(change.amount));
                }
            } else {
                for (Vector dir : change.directions) {
                    region.contract(dir.multiply(change.amount), dir.multiply(-change.reverse));
                }
            }

            session.getRegionSelector(player.getWorld())
                .learnChanges();
            int newSize = region.getArea();

            session.getRegionSelector(player.getWorld())
                .explainRegionAdjust(player, session);

            player.print("Region contracted " + (oldSize - newSize) + " blocks.");
        } catch (RegionOperationException e) {
            player.printError(e.getMessage());
        }
    }

    private static void shift(Player player, LocalSession session, CommandCall args) throws Exception {
        List<Vector> dirs = new ArrayList<Vector>();
        int change = args.integer("amount");
        if (args.has("direction")) {
            addDirections(player, args.string("direction"), dirs);
        } else {
            dirs.add(
                WorldEdit.getInstance()
                    .getDirection(player, "me"));
        }

        try {
            Region region = session.getSelection(player.getWorld());

            for (Vector dir : dirs) {
                region.shift(dir.multiply(change));
            }

            session.getRegionSelector(player.getWorld())
                .learnChanges();

            session.getRegionSelector(player.getWorld())
                .explainRegionAdjust(player, session);

            player.print("Region shifted.");
        } catch (RegionOperationException e) {
            player.printError(e.getMessage());
        }
    }

    private static void outset(Player player, LocalSession session, CommandCall args) throws Exception {
        Region region = session.getSelection(player.getWorld());
        region.expand(getChangesForEachDir(args));
        session.getRegionSelector(player.getWorld())
            .learnChanges();
        session.getRegionSelector(player.getWorld())
            .explainRegionAdjust(player, session);
        player.print("Region outset.");
    }

    private static void inset(Player player, LocalSession session, CommandCall args) throws Exception {
        Region region = session.getSelection(player.getWorld());
        region.contract(getChangesForEachDir(args));
        session.getRegionSelector(player.getWorld())
            .learnChanges();
        session.getRegionSelector(player.getWorld())
            .explainRegionAdjust(player, session);
        player.print("Region inset.");
    }

    private static Vector[] getChangesForEachDir(CommandCall args) {
        List<Vector> changes = new ArrayList<Vector>(6);
        int change = args.integer("amount");

        if (!args.flag('h')) {
            changes.add((new Vector(0, 1, 0)).multiply(change));
            changes.add((new Vector(0, -1, 0)).multiply(change));
        }

        if (!args.flag('v')) {
            changes.add((new Vector(1, 0, 0)).multiply(change));
            changes.add((new Vector(-1, 0, 0)).multiply(change));
            changes.add((new Vector(0, 0, 1)).multiply(change));
            changes.add((new Vector(0, 0, -1)).multiply(change));
        }

        return changes.toArray(new Vector[0]);
    }

    private static void size(Player player, LocalSession session, CommandCall args) throws Exception {

        if (args.flag('c')) {
            ClipboardHolder holder = session.getClipboard();
            Clipboard clipboard = holder.getClipboard();
            Region region = clipboard.getRegion();
            Vector size = region.getMaximumPoint()
                .subtract(region.getMinimumPoint());
            Vector origin = clipboard.getOrigin();

            player.print("Cuboid dimensions (max - min): " + size);
            player.print("Offset: " + origin);
            player.print("Cuboid distance: " + size.distance(Vector.ONE));
            player.print("# of blocks: " + (int) (size.getX() * size.getY() * size.getZ()));
            return;
        }

        Region region = session.getSelection(player.getWorld());
        Vector size = region.getMaximumPoint()
            .subtract(region.getMinimumPoint())
            .add(1, 1, 1);

        player.print(
            "Type: " + session.getRegionSelector(player.getWorld())
                .getTypeName());

        for (String line : session.getRegionSelector(player.getWorld())
            .getInformationLines()) {
            player.print(line);
        }

        player.print("Size: " + size);
        player.print(
            "Cuboid distance: " + region.getMaximumPoint()
                .distance(region.getMinimumPoint()));
        player.print("# of blocks: " + region.getArea());
    }

    private static void count(Player player, LocalSession session, CommandCall args) throws Exception {
        EditSession editSession = args.editSession();

        boolean useData = args.flag('d');
        if (args.string("block")
            .contains(":")) {
            useData = true; // override d flag, if they specified data they want it
        }
        if (useData) {
            Set<BaseBlock> searchBlocks = CommandInput.blocks(player, args.string("block"), true, false);
            int count = editSession.countBlocks(session.getSelection(player.getWorld()), searchBlocks);
            player.print("Counted: " + count);
        } else {
            Set<Integer> searchIDs = CommandInput.blockIds(player, args.string("block"), true);
            int count = editSession.countBlock(session.getSelection(player.getWorld()), searchIDs);
            player.print("Counted: " + count);
        }
    }

    private static void distr(Player player, LocalSession session, CommandCall args) throws Exception {
        EditSession editSession = args.editSession();

        int size;
        boolean useData = args.flag('d');
        List<Countable<Integer>> distribution = null;
        List<Countable<BaseBlock>> distributionData = null;

        if (args.flag('c')) {
            // TODO: Update for new clipboard
            throw new CommandFailure("Needs to be re-written again");
        } else {
            if (useData) {
                distributionData = editSession.getBlockDistributionWithData(session.getSelection(player.getWorld()));
            } else {
                distribution = editSession.getBlockDistribution(session.getSelection(player.getWorld()));
            }
            size = session.getSelection(player.getWorld())
                .getArea();
        }

        if ((useData && distributionData.size() <= 0) || (!useData && distribution.size() <= 0)) { // *Should* always be
                                                                                                   // false
            player.printError("No blocks counted.");
            return;
        }

        player.print("# total blocks: " + size);

        if (useData) {
            for (Countable<BaseBlock> c : distributionData) {
                String name = BlockType.fromID(
                    c.getID()
                        .getId())
                    .getName();
                String str = String.format(
                    "%-7s (%.3f%%) %s #%d:%d",
                    String.valueOf(c.getAmount()),
                    c.getAmount() / (double) size * 100,
                    name == null ? "Unknown" : name,
                    c.getID()
                        .getType(),
                    c.getID()
                        .getData());
                player.print(str);
            }
        } else {
            for (Countable<Integer> c : distribution) {
                BlockType block = BlockType.fromID(c.getID());
                String str = String.format(
                    "%-7s (%.3f%%) %s #%d",
                    String.valueOf(c.getAmount()),
                    c.getAmount() / (double) size * 100,
                    block == null ? "Unknown" : block.getName(),
                    c.getID());
                player.print(str);
            }
        }
    }

    private static void select(Player player, LocalSession session, CommandCall args) throws Exception {
        final World world = player.getWorld();
        if (!args.has("type")) {
            session.getRegionSelector(world)
                .clear();
            session.dispatchCUISelection(player);
            player.print("Selection cleared.");
            return;
        }

        final String typeName = args.string("type");
        final RegionSelector oldSelector = session.getRegionSelector(world);

        final RegionSelector selector;
        if (typeName.equalsIgnoreCase("cuboid")) {
            selector = new CuboidRegionSelector(oldSelector);
            player.print("Cuboid: left click for point 1, right click for point 2");
        } else if (typeName.equalsIgnoreCase("extend")) {
            selector = new ExtendingCuboidRegionSelector(oldSelector);
            player.print("Cuboid: left click for a starting point, right click to extend");
        } else if (typeName.equalsIgnoreCase("poly")) {
            selector = new Polygonal2DRegionSelector(oldSelector);
            player.print("2D polygon selector: Left/right click to add a point.");
            Optional<Integer> limit = ActorSelectorLimits.forActor(player)
                .getPolygonVertexLimit();
            if (limit.isPresent()) {
                player.print(limit.get() + " points maximum.");
            }
        } else if (typeName.equalsIgnoreCase("ellipsoid")) {
            selector = new EllipsoidRegionSelector(oldSelector);
            player.print("Ellipsoid selector: left click=center, right click to extend");
        } else if (typeName.equalsIgnoreCase("sphere")) {
            selector = new SphereRegionSelector(oldSelector);
            player.print("Sphere selector: left click=center, right click to set radius");
        } else if (typeName.equalsIgnoreCase("cyl")) {
            selector = new CylinderRegionSelector(oldSelector);
            player.print("Cylindrical selector: Left click=center, right click to extend.");
        } else if (typeName.equalsIgnoreCase("convex") || typeName.equalsIgnoreCase("hull")
            || typeName.equalsIgnoreCase("polyhedron")) {
                selector = new ConvexPolyhedralRegionSelector(oldSelector);
                player.print("Convex polyhedral selector: Left click=First vertex, right click to add more.");
                Optional<Integer> limit = ActorSelectorLimits.forActor(player)
                    .getPolyhedronVertexLimit();
                if (limit.isPresent()) {
                    player.print(limit.get() + " points maximum.");
                }
            } else {
                CommandListBox box = new CommandListBox("Selection modes");
                StyledFragment contents = box.getContents();
                StyledFragment tip = contents.createFragment(Style.RED);
                tip.append("Select one of the modes below:")
                    .newLine();

                box.appendCommand("cuboid", "Select two corners of a cuboid");
                box.appendCommand("extend", "Fast cuboid selection mode");
                box.appendCommand("poly", "Select a 2D polygon with height");
                box.appendCommand("ellipsoid", "Select an ellipsoid");
                box.appendCommand("sphere", "Select a sphere");
                box.appendCommand("cyl", "Select a cylinder");
                box.appendCommand("convex", "Select a convex polyhedral");

                player.printRaw(ColorCodeBuilder.asColorCodes(box));
                return;
            }

        if (args.flag('d')) {
            RegionSelectorType found = null;
            for (RegionSelectorType type : RegionSelectorType.values()) {
                if (type.getSelectorClass() == selector.getClass()) {
                    found = type;
                    break;
                }
            }

            if (found != null) {
                session.setDefaultRegionSelector(found);
                player.print("Your default region selector is now " + found.name() + ".");
            } else {
                throw new RuntimeException("Something unexpected happened. Please report this.");
            }
        }

        session.setRegionSelector(world, selector);
        session.dispatchCUISelection(player);
    }

}
