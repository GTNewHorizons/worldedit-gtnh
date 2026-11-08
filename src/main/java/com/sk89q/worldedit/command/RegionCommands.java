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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.sk89q.minecraft.util.commands.Logging.LogMode.ALL;
import static com.sk89q.minecraft.util.commands.Logging.LogMode.ORIENTATION_REGION;
import static com.sk89q.minecraft.util.commands.Logging.LogMode.REGION;
import static com.sk89q.worldedit.regions.Regions.asFlatRegion;
import static com.sk89q.worldedit.regions.Regions.maximumBlockY;
import static com.sk89q.worldedit.regions.Regions.minimumBlockY;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.Logging;
import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.function.GroundFunction;
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
import com.sk89q.worldedit.function.pattern.Patterns;
import com.sk89q.worldedit.function.visitor.LayerVisitor;
import com.sk89q.worldedit.function.visitor.RegionVisitor;
import com.sk89q.worldedit.internal.annotation.Direction;
import com.sk89q.worldedit.internal.annotation.Selection;
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
import com.sk89q.worldedit.util.command.binding.Range;
import com.sk89q.worldedit.util.command.binding.Switch;
import com.sk89q.worldedit.util.command.binding.Text;
import com.sk89q.worldedit.util.command.parametric.Optional;
import com.sk89q.worldedit.util.lighting.LightingScheduler;
import com.sk89q.worldedit.util.operation.OperationCompletion;
import com.sk89q.worldedit.util.operation.OperationScheduler;
import com.sk89q.worldedit.world.World;

/**
 * Commands that operate on regions.
 */
public class RegionCommands {

    private final WorldEdit worldEdit;

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public RegionCommands(WorldEdit worldEdit) {
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;
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

    @Command(
        aliases = { "/line" },
        usage = "<block> [thickness]",
        desc = "Draws a line segment between cuboid selection corners",
        help = "Draws a line segment between cuboid selection corners.\n" + "Can only be used with cuboid selections.\n"
            + "Flags:\n"
            + "  -h generates only a shell",
        flags = "h",
        min = 1,
        max = 2)
    @CommandPermissions("worldedit.region.line")
    @Logging(REGION)
    public void line(Player player, EditSession editSession, @Selection Region region, Pattern pattern,
        @Optional("0") @Range(min = 0) int thickness, @Switch('h') boolean shell) throws WorldEditException {

        if (!(region instanceof CuboidRegion)) {
            player.printError("//line only works with cuboid selections");
            return;
        }

        CuboidRegion cuboidregion = (CuboidRegion) region;
        Vector pos1 = cuboidregion.getPos1();
        Vector pos2 = cuboidregion.getPos2();
        int blocksChanged = editSession.drawLine(Patterns.wrap(pattern), pos1, pos2, thickness, !shell);

        player.print(blocksChanged + " block(s) have been changed.");
    }

    @Command(
        aliases = { "/curve" },
        usage = "<block> [thickness]",
        desc = "Draws a spline through selected points",
        help = "Draws a spline through selected points.\n" + "Can only be used with convex polyhedral selections.\n"
            + "Flags:\n"
            + "  -h generates only a shell",
        flags = "h",
        min = 1,
        max = 2)
    @CommandPermissions("worldedit.region.curve")
    @Logging(REGION)
    public void curve(Player player, EditSession editSession, @Selection Region region, Pattern pattern,
        @Optional("0") @Range(min = 0) int thickness, @Switch('h') boolean shell) throws WorldEditException {
        if (!(region instanceof ConvexPolyhedralRegion)) {
            player.printError("//line only works with convex polyhedral selections");
            return;
        }

        ConvexPolyhedralRegion cpregion = (ConvexPolyhedralRegion) region;
        List<Vector> vectors = new ArrayList<Vector>(cpregion.getVertices());

        int blocksChanged = editSession.drawSpline(Patterns.wrap(pattern), vectors, 0, 0, 0, 10, thickness, !shell);

        player.print(blocksChanged + " block(s) have been changed.");
    }

    @Command(
        aliases = { "/replace", "/re", "/rep" },
        usage = "[from-block] <to-block>",
        desc = "Replace all blocks in the selection with another",
        flags = "f",
        min = 1,
        max = 2)
    @CommandPermissions("worldedit.region.replace")
    @Logging(REGION)
    public void replace(Player player, EditSession editSession, @Selection Region region, @Optional Mask from,
        Pattern to) throws WorldEditException {
        if (from == null) {
            from = new ExistingBlockMask(editSession);
        }
        schedule(
            editSession,
            editSession.replaceBlocksOperation(region, from, Patterns.wrap(to)),
            player,
            " block(s) have been replaced.");
    }

    @Command(
        aliases = { "/overlay" },
        usage = "<block>",
        desc = "Set a block on top of blocks in the region",
        min = 1,
        max = 1)
    @CommandPermissions("worldedit.region.overlay")
    @Logging(REGION)
    public void overlay(Player player, EditSession editSession, @Selection Region region, Pattern pattern)
        throws WorldEditException {
        CountedOperation operation = editSession.overlayCuboidBlocksOperation(region, Patterns.wrap(pattern));
        schedule(editSession, operation, operation, player, "", " block(s) have been overlaid.");
    }

    @Command(aliases = { "/center", "/middle" }, usage = "<block>", desc = "Set the center block(s)", min = 1, max = 1)
    @Logging(REGION)
    @CommandPermissions("worldedit.region.center")
    public void center(Player player, EditSession editSession, @Selection Region region, Pattern pattern)
        throws WorldEditException {
        int affected = editSession.center(region, Patterns.wrap(pattern));
        player.print("Center set (" + affected + " blocks changed)");
    }

    @Command(
        aliases = { "/naturalize" },
        usage = "",
        desc = "3 layers of dirt on top then rock below",
        min = 0,
        max = 0)
    @CommandPermissions("worldedit.region.naturalize")
    @Logging(REGION)
    public void naturalize(Player player, EditSession editSession, @Selection Region region) throws WorldEditException {
        int affected = editSession.naturalizeCuboidBlocks(region);
        player.print(affected + " block(s) have been made to look more natural.");
    }

    @Command(
        aliases = { "/walls" },
        usage = "<block>",
        desc = "Build the four sides of the selection",
        min = 1,
        max = 1)
    @CommandPermissions("worldedit.region.walls")
    @Logging(REGION)
    public void walls(Player player, EditSession editSession, @Selection Region region, Pattern pattern)
        throws WorldEditException {
        schedule(
            editSession,
            editSession.makeCuboidWallsOperation(region, Patterns.wrap(pattern)),
            player,
            " block(s) have been changed.");
    }

    @Command(
        aliases = { "/faces", "/outline" },
        usage = "<block>",
        desc = "Build the walls, ceiling, and floor of a selection",
        min = 1,
        max = 1)
    @CommandPermissions("worldedit.region.faces")
    @Logging(REGION)
    public void faces(Player player, EditSession editSession, @Selection Region region, Pattern pattern)
        throws WorldEditException {
        schedule(
            editSession,
            editSession.makeCuboidFacesOperation(region, Patterns.wrap(pattern)),
            player,
            " block(s) have been changed.");
    }

    @Command(
        aliases = { "/smooth" },
        usage = "[iterations]",
        flags = "n",
        desc = "Smooth the elevation in the selection",
        help = "Smooths the elevation in the selection.\n"
            + "The -n flag makes it only consider naturally occuring blocks.",
        min = 0,
        max = 1)
    @CommandPermissions("worldedit.region.smooth")
    @Logging(REGION)
    public void smooth(Player player, EditSession editSession, @Selection Region region, @Optional("1") int iterations,
        @Switch('n') boolean affectNatural) throws WorldEditException {
        HeightMap heightMap = new HeightMap(editSession, region, affectNatural);
        HeightMapFilter filter = new HeightMapFilter(new GaussianKernel(5, 1.0));
        HeightMap.Applier applier = heightMap.applyFilterOperation(filter, iterations);
        schedule(editSession, applier, applier, player, "Terrain's height map smoothed. ", " block(s) changed.");
    }

    @Command(
        aliases = { "/move" },
        usage = "[count] [direction] [leave-id]",
        flags = "s",
        desc = "Move the contents of the selection",
        help = "Moves the contents of the selection.\n" + "The -s flag shifts the selection to the target location.\n"
            + "Optionally fills the old location with <leave-id>.",
        min = 0,
        max = 3)
    @CommandPermissions("worldedit.region.move")
    @Logging(ORIENTATION_REGION)
    public void move(Player player, EditSession editSession, LocalSession session, @Selection Region region,
        @Optional("1") @Range(min = 1) int count, @Optional(Direction.AIM) @Direction Vector direction,
        @Optional("air") BaseBlock replace, @Switch('s') boolean moveSelection) throws WorldEditException {

        final Region snapshot = region.clone();
        final Vector shift = direction.multiply(count);
        final CountedOperation operation = editSession.moveRegionOperation(region, direction, count, true, replace);

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

    @Command(
        aliases = { "/stack" },
        usage = "[count] [direction]",
        flags = "sa",
        desc = "Repeat the contents of the selection",
        help = "Repeats the contents of the selection.\n" + "Flags:\n"
            + "  -s shifts the selection to the last stacked copy\n"
            + "  -a skips air blocks",
        min = 0,
        max = 2)
    @CommandPermissions("worldedit.region.stack")
    @Logging(ORIENTATION_REGION)
    public void stack(Player player, EditSession editSession, LocalSession session, @Selection Region region,
        @Optional("1") @Range(min = 1) int count, @Optional(Direction.AIM) @Direction Vector direction,
        @Switch('s') boolean moveSelection, @Switch('a') boolean ignoreAirBlocks) throws WorldEditException {
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

    @Command(
        aliases = { "/regen" },
        usage = "",
        desc = "Regenerates the contents of the selection",
        help = "Regenerates the contents of the current selection.\n"
            + "This command might affect things outside the selection,\n"
            + "if they are within the same chunk.",
        min = 0,
        max = 0)
    @CommandPermissions("worldedit.regen")
    @Logging(REGION)
    public void regenerateChunk(Player player, LocalSession session, EditSession editSession, @Selection Region region)
        throws WorldEditException {
        Mask mask = session.getMask();
        try {
            session.setMask((Mask) null);
            player.getWorld()
                .regenerate(region, editSession);
        } finally {
            session.setMask(mask);
        }
        player.print("Region regenerated.");
    }

    @Command(
        aliases = { "/deform" },
        usage = "<expression>",
        desc = "Deforms a selected region with an expression",
        help = "Deforms a selected region with an expression\n"
            + "The expression is executed for each block and is expected\n"
            + "to modify the variables x, y and z to point to a new block\n"
            + "to fetch. See also tinyurl.com/wesyntax.",
        flags = "ro",
        min = 1,
        max = -1)
    @CommandPermissions("worldedit.region.deform")
    @Logging(ALL)
    public void deform(Player player, LocalSession session, EditSession editSession, @Selection Region region,
        @Text String expression, @Switch('r') boolean useRawCoords, @Switch('o') boolean offset)
        throws WorldEditException {
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

    @Command(
        aliases = { "/hollow" },
        usage = "[<thickness>[ <block>]]",
        desc = "Hollows out the object contained in this selection",
        help = "Hollows out the object contained in this selection.\n"
            + "Optionally fills the hollowed out part with the given block.\n"
            + "Thickness is measured in manhattan distance.",
        min = 0,
        max = 2)
    @CommandPermissions("worldedit.region.hollow")
    @Logging(REGION)
    public void hollow(Player player, EditSession editSession, @Selection Region region,
        @Optional("0") @Range(min = 0) int thickness, @Optional("air") Pattern pattern) throws WorldEditException {

        int affected = editSession.hollowOutRegion(region, thickness, Patterns.wrap(pattern));
        player.print(affected + " block(s) have been changed.");
    }

    @Command(
        aliases = { "/forest" },
        usage = "[type] [density]",
        desc = "Make a forest within the region",
        min = 0,
        max = 2)
    @CommandPermissions("worldedit.region.forest")
    @Logging(REGION)
    public void forest(Player player, EditSession editSession, @Selection Region region,
        @Optional("tree") TreeType type, @Optional("5") @Range(min = 0, max = 100) double density)
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

    @Command(aliases = { "/flora" }, usage = "[density]", desc = "Make flora within the region", min = 0, max = 1)
    @CommandPermissions("worldedit.region.flora")
    @Logging(REGION)
    public void flora(Player player, EditSession editSession, @Selection Region region,
        @Optional("10") @Range(min = 0, max = 100) double density) throws WorldEditException {
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

    @Command(
        aliases = { "/fixlighting" },
        usage = "",
        desc = "Fix lighting in a region or around you",
        min = 0,
        max = 0)
    @CommandPermissions("worldedit.light.fix")
    @Logging(REGION)
    public void fixLighting(Player player, LocalSession session, EditSession editSession) throws WorldEditException {
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

    @Command(
        aliases = { "/getlighting" },
        usage = "",
        desc = "Get the emitted and sky light at your position",
        min = 0,
        max = 0)
    @CommandPermissions("worldedit.light.fix")
    public void getLighting(Player player) {
        Vector pos = player.getPosition();
        World world = player.getWorld();
        int block = world.getEmittedLightLevel(pos);
        int sky = world.getSkyLightLevel(pos);
        player.print("Light: " + block + " | " + sky);
    }

    @Command(
        aliases = { "/removelight", "/removelighting" },
        usage = "",
        desc = "Remove lighting in a region or around you",
        min = 0,
        max = 0)
    @CommandPermissions("worldedit.light.remove")
    @Logging(REGION)
    public void removeLighting(Player player, LocalSession session, EditSession editSession) throws WorldEditException {
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

    @Command(
        aliases = { "/setblocklight", "/setlight" },
        usage = "<level>",
        desc = "Set block lighting in a selection",
        min = 1,
        max = 1)
    @CommandPermissions("worldedit.light.set")
    @Logging(REGION)
    public void setBlockLight(Player player, EditSession editSession, @Selection Region region,
        @Range(min = 0, max = 15) int level) {
        World world = player.getWorld();
        Set<Vector2D> chunkVectors = region.getChunks();
        for (Vector pt : region) {
            world.setEmittedLightLevel(pt, level);
        }
        player.print(chunkVectors.size() + " chunk(s) updated.");
    }

    @Command(
        aliases = { "/setskylight" },
        usage = "<level>",
        desc = "Set sky lighting in a selection",
        min = 1,
        max = 1)
    @CommandPermissions("worldedit.light.set")
    @Logging(REGION)
    public void setSkyLight(Player player, EditSession editSession, @Selection Region region,
        @Range(min = 0, max = 15) int level) {
        World world = player.getWorld();
        Set<Vector2D> chunkVectors = region.getChunks();
        for (Vector pt : region) {
            world.setSkyLightLevel(pt, level);
        }
        player.print(chunkVectors.size() + " chunk(s) updated.");
    }

}
