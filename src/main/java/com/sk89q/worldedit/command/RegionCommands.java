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
import static com.sk89q.worldedit.command.brigadier.Nodes.decimal;
import static com.sk89q.worldedit.command.brigadier.Nodes.integer;
import static com.sk89q.worldedit.command.brigadier.Nodes.literal;
import static com.sk89q.worldedit.command.brigadier.Nodes.text;
import static com.sk89q.worldedit.command.brigadier.Nodes.word;
import static com.sk89q.worldedit.regions.Regions.asFlatRegion;
import static com.sk89q.worldedit.regions.Regions.maximumBlockY;
import static com.sk89q.worldedit.regions.Regions.minimumBlockY;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.command.ICommandSender;

import com.mojang.brigadier.CommandDispatcher;
import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.command.brigadier.CommandCall;
import com.sk89q.worldedit.command.brigadier.LogMode;
import com.sk89q.worldedit.command.brigadier.Nodes;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.function.Contextual;
import com.sk89q.worldedit.function.EditContext;
import com.sk89q.worldedit.function.GroundFunction;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.block.BlockReplace;
import com.sk89q.worldedit.function.factory.Apply;
import com.sk89q.worldedit.function.generator.FloraGenerator;
import com.sk89q.worldedit.function.generator.ForestGenerator;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.NoiseFilter2D;
import com.sk89q.worldedit.function.operation.AffectedCounter;
import com.sk89q.worldedit.function.operation.CountedOperation;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.LayerVisitor;
import com.sk89q.worldedit.function.visitor.RegionVisitor;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.math.convolution.GaussianKernel;
import com.sk89q.worldedit.math.convolution.HeightMap;
import com.sk89q.worldedit.math.convolution.HeightMapFilter;
import com.sk89q.worldedit.math.noise.RandomNoise;
import com.sk89q.worldedit.regions.ConvexPolyhedralRegion;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionOperationException;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.util.TreeGenerator.TreeType;
import com.sk89q.worldedit.util.lighting.LightingScheduler;
import com.sk89q.worldedit.util.operation.OperationCompletion;
import com.sk89q.worldedit.util.operation.OperationScheduler;
import com.sk89q.worldedit.world.World;

/**
 * Commands that operate on regions.
 */
public final class RegionCommands {

    private RegionCommands() {}

    // spotless:off
    public static void register(CommandDispatcher<ICommandSender> dispatcher) {
        Nodes.register(dispatcher, literal("/set", "Set all blocks within selection").executes(Nodes.USAGE)
                .then(word("pattern")
                        .executes(run("worldedit.region.set", RegionCommands::set))));

        Nodes.registerWithFlags(dispatcher, literal("/line", "Draws a line segment between cuboid selection corners").executes(Nodes.USAGE)
                .then(word("pattern")
                        .executes(run("worldedit.region.line", LogMode.REGION, c ->
                                line(c.player(), c.editSession(), c.region(), c.pattern("pattern"), 0, c.flag('h'))))
                        .then(integer("thickness", 0)
                                .executes(run("worldedit.region.line", LogMode.REGION, c ->
                                        line(c.player(), c.editSession(), c.region(), c.pattern("pattern"), c.integer("thickness"), c.flag('h')))))),
                "h");

        Nodes.registerWithFlags(dispatcher, literal("/curve", "Draws a spline through selected points").executes(Nodes.USAGE)
                .then(word("pattern")
                        .executes(run("worldedit.region.curve", LogMode.REGION, c ->
                                curve(c.player(), c.editSession(), c.region(), c.pattern("pattern"), 0, c.flag('h'))))
                        .then(integer("thickness", 0)
                                .executes(run("worldedit.region.curve", LogMode.REGION, c ->
                                        curve(c.player(), c.editSession(), c.region(), c.pattern("pattern"), c.integer("thickness"), c.flag('h')))))),
                "h");

        // //replace [from-mask] <to-pattern>: with one word that word is the target. Brigadier can not have an optional
        // argument in front of a required one, so the words are told apart here.
        Nodes.registerWithFlags(dispatcher, literal("/replace", "Replace all blocks in the selection with another").executes(Nodes.USAGE)
                .then(word("from-or-to")
                        .executes(run("worldedit.region.replace", LogMode.REGION, c ->
                                replace(c.player(), c.editSession(), c.region(), null, c.pattern("from-or-to"))))
                        .then(word("to")
                                .executes(run("worldedit.region.replace", LogMode.REGION, c ->
                                        replace(c.player(), c.editSession(), c.region(), c.mask("from-or-to"), c.pattern("to")))))),
                "f", "/re", "/rep");

        Nodes.register(dispatcher, literal("/overlay", "Set a block on top of blocks in the region").executes(Nodes.USAGE)
                .then(word("pattern")
                        .executes(run("worldedit.region.overlay", LogMode.REGION, c ->
                                overlay(c.player(), c.editSession(), c.region(), c.pattern("pattern"))))));

        Nodes.register(dispatcher, literal("/center", "Set the center block(s)").executes(Nodes.USAGE)
                .then(word("pattern")
                        .executes(run("worldedit.region.center", LogMode.REGION, c ->
                                center(c.player(), c.editSession(), c.region(), c.pattern("pattern"))))),
                "/middle");

        Nodes.register(dispatcher, literal("/naturalize", "3 layers of dirt on top then rock below")
                .executes(run("worldedit.region.naturalize", LogMode.REGION, c ->
                        naturalize(c.player(), c.editSession(), c.region()))));

        Nodes.register(dispatcher, literal("/walls", "Build the four sides of the selection").executes(Nodes.USAGE)
                .then(word("pattern")
                        .executes(run("worldedit.region.walls", LogMode.REGION, c ->
                                walls(c.player(), c.editSession(), c.region(), c.pattern("pattern"))))));

        Nodes.register(dispatcher, literal("/faces", "Build the walls, ceiling, and floor of a selection").executes(Nodes.USAGE)
                .then(word("pattern")
                        .executes(run("worldedit.region.faces", LogMode.REGION, c ->
                                faces(c.player(), c.editSession(), c.region(), c.pattern("pattern"))))),
                "/outline");

        Nodes.registerWithFlags(dispatcher, literal("/smooth", "Smooth the elevation in the selection")
                .executes(run("worldedit.region.smooth", LogMode.REGION, c ->
                        smooth(c.player(), c.editSession(), c.region(), 1, c.flag('n'))))
                .then(integer("iterations")
                        .executes(run("worldedit.region.smooth", LogMode.REGION, c ->
                                smooth(c.player(), c.editSession(), c.region(), c.integer("iterations"), c.flag('n'))))),
                "n");

        Nodes.registerWithFlags(dispatcher, literal("/move", "Move the contents of the selection")
                .executes(run("worldedit.region.move", LogMode.ORIENTATION_REGION, c ->
                        move(c.player(), c.editSession(), c.session(), c.region(), 1, c.direction("direction", "me"), c.parseBlock("air"), c.flag('s'), c.flag('a'))))
                .then(integer("count", 1)
                        .executes(run("worldedit.region.move", LogMode.ORIENTATION_REGION, c ->
                                move(c.player(), c.editSession(), c.session(), c.region(), c.integer("count"), c.direction("direction", "me"), c.parseBlock("air"), c.flag('s'), c.flag('a'))))
                        .then(word("direction")
                                .executes(run("worldedit.region.move", LogMode.ORIENTATION_REGION, c ->
                                        move(c.player(), c.editSession(), c.session(), c.region(), c.integer("count"), c.direction("direction"), c.parseBlock("air"), c.flag('s'), c.flag('a'))))
                                .then(word("leave-block")
                                        .executes(run("worldedit.region.move", LogMode.ORIENTATION_REGION, c ->
                                                move(c.player(), c.editSession(), c.session(), c.region(), c.integer("count"), c.direction("direction"), c.block("leave-block"), c.flag('s'), c.flag('a'))))))),
                "sa");

        Nodes.registerWithFlags(dispatcher, literal("/stack", "Repeat the contents of the selection")
                .executes(run("worldedit.region.stack", LogMode.ORIENTATION_REGION, c ->
                        stack(c.player(), c.editSession(), c.session(), c.region(), 1, c.direction("direction", "me"), c.flag('s'), c.flag('a'))))
                .then(integer("count", 1)
                        .executes(run("worldedit.region.stack", LogMode.ORIENTATION_REGION, c ->
                                stack(c.player(), c.editSession(), c.session(), c.region(), c.integer("count"), c.direction("direction", "me"), c.flag('s'), c.flag('a'))))
                        .then(word("direction")
                                .executes(run("worldedit.region.stack", LogMode.ORIENTATION_REGION, c ->
                                        stack(c.player(), c.editSession(), c.session(), c.region(), c.integer("count"), c.direction("direction"), c.flag('s'), c.flag('a')))))),
                "sa");

        Nodes.register(dispatcher, literal("/regen", "Regenerates the contents of the selection")
                .executes(run("worldedit.regen", LogMode.REGION, c ->
                        regenerateChunk(c.player(), c.session(), c.editSession(), c.region()))));

        Nodes.registerWithFlags(dispatcher, literal("/deform", "Deforms a selected region with an expression").executes(Nodes.USAGE)
                .then(text("expression")
                        .executes(run("worldedit.region.deform", LogMode.ALL, c ->
                                deform(c.player(), c.session(), c.editSession(), c.region(), c.string("expression"), c.flag('r'), c.flag('o'))))),
                "ro");

        Nodes.register(dispatcher, literal("/hollow", "Hollows out the object contained in this selection")
                .executes(run("worldedit.region.hollow", LogMode.REGION, c ->
                        hollow(c.player(), c.editSession(), c.region(), 0, c.parsePattern("air"))))
                .then(integer("thickness", 0)
                        .executes(run("worldedit.region.hollow", LogMode.REGION, c ->
                                hollow(c.player(), c.editSession(), c.region(), c.integer("thickness"), c.parsePattern("air"))))
                        .then(word("pattern")
                                .executes(run("worldedit.region.hollow", LogMode.REGION, c ->
                                        hollow(c.player(), c.editSession(), c.region(), c.integer("thickness"), c.pattern("pattern")))))));

        Nodes.register(dispatcher, literal("/forest", "Make a forest within the region")
                .executes(run("worldedit.region.forest", LogMode.REGION, c ->
                        forest(c.player(), c.editSession(), c.region(), TreeType.TREE, 5)))
                .then(word("type")
                        .executes(run("worldedit.region.forest", LogMode.REGION, c ->
                                forest(c.player(), c.editSession(), c.region(), c.treeType("type"), 5)))
                        .then(decimal("density", 0, 100)
                                .executes(run("worldedit.region.forest", LogMode.REGION, c ->
                                        forest(c.player(), c.editSession(), c.region(), c.treeType("type"), c.decimal("density")))))));

        Nodes.register(dispatcher, literal("/flora", "Make flora within the region")
                .executes(run("worldedit.region.flora", LogMode.REGION, c ->
                        flora(c.player(), c.editSession(), c.region(), 10)))
                .then(decimal("density", 0, 100)
                        .executes(run("worldedit.region.flora", LogMode.REGION, c ->
                                flora(c.player(), c.editSession(), c.region(), c.decimal("density"))))));

        Nodes.register(dispatcher, literal("/fixlighting", "Fix lighting in a region or around you")
                .executes(run("worldedit.light.fix", LogMode.REGION, c ->
                        fixLighting(c.player(), c.session(), c.editSession()))));

        Nodes.register(dispatcher, literal("/getlighting", "Get the emitted and sky light at your position")
                .executes(run("worldedit.light.fix", c -> getLighting(c.player()))));

        Nodes.register(dispatcher, literal("/removelight", "Remove lighting in a region or around you")
                .executes(run("worldedit.light.remove", LogMode.REGION, c ->
                        removeLighting(c.player(), c.session(), c.editSession()))),
                "/removelighting");

        Nodes.register(dispatcher, literal("/setblocklight", "Set block lighting in a selection").executes(Nodes.USAGE)
                .then(integer("level", 0, 15)
                        .executes(run("worldedit.light.set", LogMode.REGION, c ->
                                setBlockLight(c.player(), c.editSession(), c.region(), c.integer("level"))))),
                "/setlight");

        Nodes.register(dispatcher, literal("/setskylight", "Set sky lighting in a selection").executes(Nodes.USAGE)
                .then(integer("level", 0, 15)
                        .executes(run("worldedit.light.set", LogMode.REGION, c ->
                                setSkyLight(c.player(), c.editSession(), c.region(), c.integer("level"))))));
    }
    // spotless:on

    /**
     * {@code //set <pattern>}: the edit may run after the player has changed their selection, so it works on a copy.
     */
    private static void set(CommandCall call) throws Exception {
        Player player = call.player();
        Region selection = call.region();
        Pattern pattern = call.pattern("pattern");
        EditSession editSession = call.editSession();

        EditContext editContext = new EditContext();
        editContext.setDestination(editSession);
        editContext.setRegion(selection.clone());
        final Operation operation = new Apply(new Contextual<RegionFunction>() {

            @Override
            public RegionFunction createFromContext(EditContext context) {
                return new BlockReplace(context.getDestination(), pattern);
            }

            @Override
            public String toString() {
                return "replace blocks";
            }
        }).createFromContext(editContext);

        // Hand the work to the scheduler, which may spread it over several ticks. The status message has to wait until
        // it is actually done.
        WorldEdit.getInstance()
            .getOperationScheduler()
            .submit(player, editSession, operation, new OperationCompletion() {

                @Override
                public void onSuccess() {
                    List<String> messages = new ArrayList<String>();
                    operation.addStatusMessages(messages);
                    if (messages.isEmpty()) {
                        player.print("Operation completed.");
                    } else {
                        player.print("Operation completed (" + String.join(", ", messages) + ").");
                    }
                }

                @Override
                public void onFailure(WorldEditException e) {
                    reportFailure(player, e);
                }
            });
    }

    /**
     * Hand a region operation to the {@link OperationScheduler}, which may spread it over several
     * server ticks, and report the affected count once it has actually finished.
     *
     * @param editSession the edit session the operation writes to
     * @param visitor     the operation
     * @param player      the player to report to
     * @param suffix      appended to the affected count to form the completion message
     */
    private static void schedule(EditSession editSession, RegionVisitor visitor, Player player, String suffix) {
        schedule(editSession, visitor, visitor, player, "", suffix);
    }

    /**
     * Hand an operation to the {@link OperationScheduler}, which may spread it over several server
     * ticks, and report the affected count once it has actually finished.
     *
     * @param editSession the edit session the operation writes to
     * @param operation   the operation
     * @param counter     the object that knows how much the operation affected
     * @param player      the player to report to
     * @param prefix      prepended to the completion message
     * @param suffix      appended to the affected count to form the completion message
     */
    private static void schedule(EditSession editSession, Operation operation, final AffectedCounter counter,
        final Player player, final String prefix, final String suffix) {
        WorldEdit.getInstance()
            .getOperationScheduler()
            .submit(player, editSession, operation, new OperationCompletion() {

                @Override
                public void onSuccess() {
                    player.print(prefix + counter.getAffected() + suffix);
                }

                @Override
                public void onFailure(WorldEditException e) {
                    reportFailure(player, e);
                }
            });
    }

    /**
     * Shift the selection to follow a finished edit, unless the player has changed it in the meantime.
     *
     * @param player    the player
     * @param session   the player's session
     * @param selection the live selection the edit was started from
     * @param snapshot  a copy of the selection from when the edit was started
     * @param shift     how far to shift it
     */
    private static void shiftSelection(Player player, LocalSession session, Region selection, Region snapshot,
        Vector shift) {
        if (!isSameSelection(player, session, selection, snapshot)) {
            player.print("Your selection changed during the edit, so it was not moved.");
            return;
        }

        try {
            selection.shift(shift);

            session.getRegionSelector(player.getWorld())
                .learnChanges();
            session.getRegionSelector(player.getWorld())
                .explainRegionAdjust(player, session);
        } catch (RegionOperationException e) {
            player.printError(e.getMessage());
        }
    }

    private static boolean isSameSelection(Player player, LocalSession session, Region selection, Region snapshot) {
        try {
            return session.getSelection(player.getWorld()) == selection && selection.getMinimumPoint()
                .equals(snapshot.getMinimumPoint())
                && selection.getMaximumPoint()
                    .equals(snapshot.getMaximumPoint());
        } catch (IncompleteRegionException e) {
            return false;
        }
    }

    private static void reportFailure(Player player, WorldEditException e) {
        String message = e.getMessage();
        player.printError(message != null ? message : "The operation could not be completed.");
    }

    private static void line(Player player, EditSession editSession, Region region, Pattern pattern, int thickness,
        boolean shell) throws WorldEditException {

        if (!(region instanceof CuboidRegion)) {
            player.printError("//line only works with cuboid selections");
            return;
        }

        CuboidRegion cuboidregion = (CuboidRegion) region;
        Vector pos1 = cuboidregion.getPos1();
        Vector pos2 = cuboidregion.getPos2();
        int blocksChanged = editSession.drawLine(pattern, pos1, pos2, thickness, !shell);

        player.print(blocksChanged + " block(s) have been changed.");
    }

    private static void curve(Player player, EditSession editSession, Region region, Pattern pattern, int thickness,
        boolean shell) throws WorldEditException {
        if (!(region instanceof ConvexPolyhedralRegion)) {
            player.printError("//line only works with convex polyhedral selections");
            return;
        }

        ConvexPolyhedralRegion cpregion = (ConvexPolyhedralRegion) region;
        List<Vector> vectors = new ArrayList<Vector>(cpregion.getVertices());

        int blocksChanged = editSession.drawSpline(pattern, vectors, 0, 0, 0, 10, thickness, !shell);

        player.print(blocksChanged + " block(s) have been changed.");
    }

    private static void replace(Player player, EditSession editSession, Region region, Mask from, Pattern to)
        throws WorldEditException {
        if (from == null) {
            from = new ExistingBlockMask(editSession);
        }
        schedule(
            editSession,
            editSession.replaceBlocksOperation(region, from, to),
            player,
            " block(s) have been replaced.");
    }

    private static void overlay(Player player, EditSession editSession, Region region, Pattern pattern)
        throws WorldEditException {
        CountedOperation operation = editSession.overlayCuboidBlocksOperation(region, pattern);
        schedule(editSession, operation, operation, player, "", " block(s) have been overlaid.");
    }

    private static void center(Player player, EditSession editSession, Region region, Pattern pattern)
        throws WorldEditException {
        int affected = editSession.center(region, pattern);
        player.print("Center set (" + affected + " blocks changed)");
    }

    private static void naturalize(Player player, EditSession editSession, Region region) throws WorldEditException {
        int affected = editSession.naturalizeCuboidBlocks(region);
        player.print(affected + " block(s) have been made to look more natural.");
    }

    private static void walls(Player player, EditSession editSession, Region region, Pattern pattern)
        throws WorldEditException {
        schedule(
            editSession,
            editSession.makeCuboidWallsOperation(region, pattern),
            player,
            " block(s) have been changed.");
    }

    private static void faces(Player player, EditSession editSession, Region region, Pattern pattern)
        throws WorldEditException {
        schedule(
            editSession,
            editSession.makeCuboidFacesOperation(region, pattern),
            player,
            " block(s) have been changed.");
    }

    private static void smooth(Player player, EditSession editSession, Region region, int iterations,
        boolean affectNatural) throws WorldEditException {
        HeightMap heightMap = new HeightMap(editSession, region, affectNatural);
        HeightMapFilter filter = new HeightMapFilter(new GaussianKernel(5, 1.0));
        HeightMap.Applier applier = heightMap.applyFilterOperation(filter, iterations);
        schedule(editSession, applier, applier, player, "Terrain's height map smoothed. ", " block(s) changed.");
    }

    private static void move(Player player, EditSession editSession, LocalSession session, Region region, int count,
        Vector direction, BaseBlock replace, boolean moveSelection, boolean ignoreAir) throws WorldEditException {

        final Region snapshot = region.clone();
        final Vector shift = direction.multiply(count);
        final CountedOperation operation = editSession
            .moveRegionOperation(region, direction, count, !ignoreAir, replace);

        // The selection follows the edit only once the edit has actually happened.
        WorldEdit.getInstance()
            .getOperationScheduler()
            .submit(player, editSession, operation, new OperationCompletion() {

                @Override
                public void onSuccess() {
                    if (moveSelection) {
                        shiftSelection(player, session, region, snapshot, shift);
                    }

                    player.print(operation.getAffected() + " blocks moved.");
                }

                @Override
                public void onFailure(WorldEditException e) {
                    reportFailure(player, e);
                }
            });
    }

    private static void stack(Player player, EditSession editSession, LocalSession session, Region region, int count,
        Vector direction, boolean moveSelection, boolean ignoreAirBlocks) throws WorldEditException {
        final Region snapshot = region.clone();
        final Vector size = region.getMaximumPoint()
            .subtract(region.getMinimumPoint());
        final Vector shift = direction.multiply(count * (Math.abs(direction.dot(size)) + 1));
        final ForwardExtentCopy operation = editSession
            .stackCuboidRegionOperation(region, direction, count, !ignoreAirBlocks);

        // The selection follows the edit only once the edit has actually happened.
        WorldEdit.getInstance()
            .getOperationScheduler()
            .submit(player, editSession, operation, new OperationCompletion() {

                @Override
                public void onSuccess() {
                    if (moveSelection) {
                        shiftSelection(player, session, region, snapshot, shift);
                    }

                    player.print(operation.getAffected() + " blocks changed. Undo with //undo");
                }

                @Override
                public void onFailure(WorldEditException e) {
                    reportFailure(player, e);
                }
            });
    }

    private static void regenerateChunk(Player player, LocalSession session, EditSession editSession, Region region)
        throws WorldEditException {
        Operation operation = player.getWorld()
            .regenerate(region, editSession);
        if (operation == null) {
            player.printError("This world can't be regenerated.");
            return;
        }

        WorldEdit.getInstance()
            .getOperationScheduler()
            .submit(player, editSession, operation, new OperationCompletion() {

                @Override
                public void onSuccess() {
                    player.print("Region regenerated.");
                }

                @Override
                public void onFailure(WorldEditException e) {
                    reportFailure(player, e);
                }
            });
    }

    private static void deform(Player player, LocalSession session, EditSession editSession, Region region,
        String expression, boolean useRawCoords, boolean offset) throws WorldEditException {
        final Vector zero;
        Vector unit;

        if (useRawCoords) {
            zero = Vector.ZERO;
            unit = Vector.ONE;
        } else if (offset) {
            zero = session.getPlacementPosition(player);
            unit = Vector.ONE;
        } else {
            final Vector min = region.getMinimumPoint();
            final Vector max = region.getMaximumPoint();

            zero = max.add(min)
                .multiply(0.5);
            unit = max.subtract(zero);

            if (unit.getX() == 0) unit = unit.setX(1.0);
            if (unit.getY() == 0) unit = unit.setY(1.0);
            if (unit.getZ() == 0) unit = unit.setZ(1.0);
        }

        try {
            final int affected = editSession.deformRegion(region, zero, unit, expression);
            player.findFreePosition();
            player.print(affected + " block(s) have been deformed.");
        } catch (ExpressionException e) {
            player.printError(e.getMessage());
        }
    }

    private static void hollow(Player player, EditSession editSession, Region region, int thickness, Pattern pattern)
        throws WorldEditException {

        int affected = editSession.hollowOutRegion(region, thickness, pattern);
        player.print(affected + " block(s) have been changed.");
    }

    private static void forest(Player player, EditSession editSession, Region region, TreeType type, double density)
        throws WorldEditException {
        density = density / 100;
        ForestGenerator generator = new ForestGenerator(editSession, new TreeGenerator(type));
        GroundFunction ground = new GroundFunction(new ExistingBlockMask(editSession), generator);
        LayerVisitor visitor = new LayerVisitor(
            asFlatRegion(region),
            minimumBlockY(region),
            maximumBlockY(region),
            ground);
        visitor.setMask(new NoiseFilter2D(new RandomNoise(), density));
        Operations.completeLegacy(visitor);

        player.print(ground.getAffected() + " trees created.");
    }

    private static void flora(Player player, EditSession editSession, Region region, double density)
        throws WorldEditException {
        density = density / 100;
        FloraGenerator generator = new FloraGenerator(editSession);
        GroundFunction ground = new GroundFunction(new ExistingBlockMask(editSession), generator);
        LayerVisitor visitor = new LayerVisitor(
            asFlatRegion(region),
            minimumBlockY(region),
            maximumBlockY(region),
            ground);
        visitor.setMask(new NoiseFilter2D(new RandomNoise(), density));
        Operations.completeLegacy(visitor);

        player.print(ground.getAffected() + " flora created.");
    }

    private static void fixLighting(Player player, LocalSession session, EditSession editSession)
        throws WorldEditException {
        World world = player.getWorld();
        Region region;
        try {
            region = session.getSelection(world);
        } catch (IncompleteRegionException e) {
            Vector pos = player.getPosition();
            int cx = pos.getBlockX() >> 4;
            int cz = pos.getBlockZ() >> 4;
            Vector min = new Vector((cx - 8) * 16, 0, (cz - 8) * 16);
            Vector max = new Vector((cx + 8) * 16 + 15, world.getMaxY(), (cz + 8) * 16 + 15);
            region = new CuboidRegion(world, min, max);
        }

        Set<Vector2D> chunkVectors = region.getChunks();
        List<BlockVector2D> chunks = new ArrayList<BlockVector2D>(chunkVectors.size());
        for (Vector2D vector : chunkVectors) {
            chunks.add(new BlockVector2D(vector));
        }

        LightingScheduler scheduler = WorldEdit.getInstance()
            .getLightingScheduler();
        boolean scheduled = scheduler.schedule(world, chunks, player, new Runnable() {

            @Override
            public void run() {
                player.print("Lighting Fixed. You may need to reload chunks.");
            }
        });

        if (!scheduled) {
            player.print("A lighting fix is already running. Please wait for it to finish.");
            return;
        }

        if (!chunks.isEmpty()) {
            player.print(chunks.size() + " chunk(s) queued for relight.");
        }
    }

    private static void getLighting(Player player) {
        Vector pos = player.getPosition();
        World world = player.getWorld();
        int block = world.getEmittedLightLevel(pos);
        int sky = world.getSkyLightLevel(pos);
        player.print("Light: " + block + " | " + sky);
    }

    private static void removeLighting(Player player, LocalSession session, EditSession editSession)
        throws WorldEditException {
        World world = player.getWorld();
        Region region;
        try {
            region = session.getSelection(world);
        } catch (IncompleteRegionException e) {
            Vector pos = player.getPosition();
            int cx = pos.getBlockX() >> 4;
            int cz = pos.getBlockZ() >> 4;
            Vector min = new Vector((cx - 8) * 16, 0, (cz - 8) * 16);
            Vector max = new Vector((cx + 8) * 16 + 15, world.getMaxY(), (cz + 8) * 16 + 15);
            region = new CuboidRegion(world, min, max);
        }

        Set<Vector2D> chunkVectors = region.getChunks();
        for (Vector pt : region) {
            world.setEmittedLightLevel(pt, 0);
            world.setSkyLightLevel(pt, 0);
        }
        player.print(chunkVectors.size() + " chunk(s) updated.");
    }

    private static void setBlockLight(Player player, EditSession editSession, Region region, int level) {
        World world = player.getWorld();
        Set<Vector2D> chunkVectors = region.getChunks();
        for (Vector pt : region) {
            world.setEmittedLightLevel(pt, level);
        }
        player.print(chunkVectors.size() + " chunk(s) updated.");
    }

    private static void setSkyLight(Player player, EditSession editSession, Region region, int level) {
        World world = player.getWorld();
        Set<Vector2D> chunkVectors = region.getChunks();
        for (Vector pt : region) {
            world.setSkyLightLevel(pt, level);
        }
        player.print(chunkVectors.size() + " chunk(s) updated.");
    }

}
