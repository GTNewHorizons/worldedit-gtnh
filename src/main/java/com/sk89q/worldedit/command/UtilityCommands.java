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

import java.util.List;
import java.util.Set;

import net.minecraft.command.ICommandSender;

import com.mojang.brigadier.CommandDispatcher;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.command.brigadier.CommandCall;
import com.sk89q.worldedit.command.brigadier.LogMode;
import com.sk89q.worldedit.command.brigadier.Nodes;
import com.sk89q.worldedit.command.util.CommandInput;
import com.sk89q.worldedit.command.util.CreatureButcher;
import com.sk89q.worldedit.command.util.EntityRemover;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.EntityVisitor;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.internal.expression.runtime.EvaluationException;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.CylinderRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;

/**
 * Utility commands that work around the player's position.
 *
 * <ul>
 * <li>{@code //fill <block> <radius> [depth]}, {@code //fillr <block> <radius> [depth]}, {@code //drain <radius>}</li>
 * <li>{@code //fixlava <radius>}, {@code //fixwater <radius>}</li>
 * <li>{@code //removeabove [size] [height]}, {@code //removebelow [size] [height]},
 * {@code //removenear <block> [size]}, {@code //replacenear [-f] <size> [from-block] <to-pattern>}</li>
 * <li>{@code //snow [radius]}, {@code //thaw [radius]}, {@code //green [-f] [radius]},
 * {@code //ex [radius]}</li>
 * <li>{@code butcher [-plangbtfr] [radius]}, {@code remove <type> <radius>}</li>
 * <li>{@code //calc <expression>}</li>
 * </ul>
 * For {@code //help} see {@link HelpCommand}.
 */
public final class UtilityCommands {

    private UtilityCommands() {}

    // spotless:off
    public static void register(CommandDispatcher<ICommandSender> dispatcher) {
        Nodes.register(dispatcher, literal("/fill", "Fill a hole").executes(Nodes.USAGE)
                .then(word("block").executes(Nodes.USAGE)
                        .then(decimal("radius")
                                .executes(run("worldedit.fill", LogMode.PLACEMENT, c -> fill(c, 1)))
                                .then(integer("depth")
                                        .executes(run("worldedit.fill", LogMode.PLACEMENT,
                                                c -> fill(c, Math.max(1, c.integer("depth")))))))));

        Nodes.register(dispatcher, literal("/fillr", "Fill a hole recursively").executes(Nodes.USAGE)
                .then(word("block").executes(Nodes.USAGE)
                        .then(decimal("radius")
                                .executes(run("worldedit.fill.recursive", LogMode.PLACEMENT,
                                        c -> fillRecursive(c, Integer.MAX_VALUE)))
                                .then(integer("depth")
                                        .executes(run("worldedit.fill.recursive", LogMode.PLACEMENT,
                                                c -> fillRecursive(c, Math.max(1, c.integer("depth")))))))));

        Nodes.register(dispatcher, literal("/drain", "Drain a pool").executes(Nodes.USAGE)
                .then(decimal("radius")
                        .executes(run("worldedit.drain", LogMode.PLACEMENT, UtilityCommands::drain))));

        Nodes.register(dispatcher, literal("/fixlava", "Fix lava to be stationary").executes(Nodes.USAGE)
                .then(decimal("radius")
                        .executes(run("worldedit.fixlava", LogMode.PLACEMENT,
                                c -> fixLiquid(c, 10, 11)))),
                "fixlava");

        Nodes.register(dispatcher, literal("/fixwater", "Fix water to be stationary").executes(Nodes.USAGE)
                .then(decimal("radius")
                        .executes(run("worldedit.fixwater", LogMode.PLACEMENT,
                                c -> fixLiquid(c, 8, 9)))),
                "fixwater");

        Nodes.register(dispatcher, literal("/removeabove", "Remove blocks above your head")
                .executes(run("worldedit.removeabove", LogMode.PLACEMENT, c -> removeAbove(c, 1, null)))
                .then(integer("size")
                        .executes(run("worldedit.removeabove", LogMode.PLACEMENT,
                                c -> removeAbove(c, c.integer("size"), null)))
                        .then(integer("height")
                                .executes(run("worldedit.removeabove", LogMode.PLACEMENT,
                                        c -> removeAbove(c, c.integer("size"), c.integer("height")))))),
                "removeabove");

        Nodes.register(dispatcher, literal("/removebelow", "Remove blocks below you")
                .executes(run("worldedit.removebelow", LogMode.PLACEMENT, c -> removeBelow(c, 1, null)))
                .then(integer("size")
                        .executes(run("worldedit.removebelow", LogMode.PLACEMENT,
                                c -> removeBelow(c, c.integer("size"), null)))
                        .then(integer("height")
                                .executes(run("worldedit.removebelow", LogMode.PLACEMENT,
                                        c -> removeBelow(c, c.integer("size"), c.integer("height")))))),
                "removebelow");

        Nodes.register(dispatcher, literal("/removenear", "Remove blocks near you").executes(Nodes.USAGE)
                .then(word("block")
                        .executes(run("worldedit.removenear", LogMode.PLACEMENT, c -> removeNear(c, 50)))
                        .then(integer("size")
                                .executes(run("worldedit.removenear", LogMode.PLACEMENT,
                                        c -> removeNear(c, c.integer("size")))))),
                "removenear");

        // //replacenear <size> [from-block] <to-pattern>: with two words the first one is the target
        Nodes.registerWithFlags(dispatcher, literal("/replacenear", "Replace nearby blocks").executes(Nodes.USAGE)
                .then(integer("size").executes(Nodes.USAGE)
                        .then(word("from-or-to")
                                .executes(run("worldedit.replacenear", LogMode.PLACEMENT,
                                        c -> replaceNear(c, null, c.string("from-or-to"))))
                                .then(word("to")
                                        .executes(run("worldedit.replacenear", LogMode.PLACEMENT,
                                                c -> replaceNear(c, c.string("from-or-to"), c.string("to")))))
                        )),
                "f", "replacenear");

        Nodes.register(dispatcher, literal("/snow", "Simulates snow")
                .executes(run("worldedit.snow", LogMode.PLACEMENT, c -> snow(c, 10)))
                .then(decimal("radius")
                        .executes(run("worldedit.snow", LogMode.PLACEMENT, c -> snow(c, c.decimal("radius"))))),
                "snow");

        Nodes.register(dispatcher, literal("/thaw", "Thaws the area")
                .executes(run("worldedit.thaw", LogMode.PLACEMENT, c -> thaw(c, 10)))
                .then(decimal("radius")
                        .executes(run("worldedit.thaw", LogMode.PLACEMENT, c -> thaw(c, c.decimal("radius"))))),
                "thaw");

        Nodes.registerWithFlags(dispatcher, literal("/green", "Greens the area")
                .executes(run("worldedit.green", LogMode.PLACEMENT, c -> green(c, 10)))
                .then(decimal("radius")
                        .executes(run("worldedit.green", LogMode.PLACEMENT, c -> green(c, c.decimal("radius"))))),
                "f", "green");

        Nodes.register(dispatcher, literal("/ex", "Extinguish nearby fire")
                .executes(run("worldedit.extinguish", LogMode.PLACEMENT, c -> extinguish(c, null)))
                .then(integer("radius")
                        .executes(run("worldedit.extinguish", LogMode.PLACEMENT,
                                c -> extinguish(c, c.integer("radius"))))),
                "/ext", "/extinguish", "ex", "ext", "extinguish");

        Nodes.registerWithFlags(dispatcher, literal("butcher", "Kill all or nearby mobs")
                .executes(run("worldedit.butcher", LogMode.PLACEMENT, c -> butcher(c, null)))
                .then(integer("radius")
                        .executes(run("worldedit.butcher", LogMode.PLACEMENT, c -> butcher(c, c.integer("radius"))))),
                "plangbtfr");

        Nodes.register(dispatcher, literal("remove", "Remove all entities of a type").executes(Nodes.USAGE)
                .then(word("type").executes(Nodes.USAGE)
                        .then(integer("radius")
                                .executes(run("worldedit.remove", LogMode.PLACEMENT, UtilityCommands::remove)))),
                "rem", "rement");

        Nodes.register(dispatcher, literal("/calc", "Evaluate a mathematical expression").executes(Nodes.USAGE)
                .then(text("expression")
                        .executes(run("worldedit.calc", UtilityCommands::calc))),
                "/calculate", "/eval", "/evaluate", "/solve");
    }
    // spotless:on

    private static void fill(CommandCall call, int depth) throws Exception {
        Player player = call.player();
        Pattern pattern = CommandInput.pattern(player, call.string("block"));
        double radius = Math.max(1, call.decimal("radius"));
        call.worldEdit()
            .checkMaxRadius(radius);

        Vector pos = call.session()
            .getPlacementPosition(player);
        int affected = call.editSession()
            .fillXZ(pos, pattern, radius, depth, false);
        player.print(affected + " block(s) have been created.");
    }

    private static void fillRecursive(CommandCall call, int depth) throws Exception {
        Player player = call.player();
        Pattern pattern = CommandInput.pattern(player, call.string("block"));
        double radius = Math.max(1, call.decimal("radius"));
        call.worldEdit()
            .checkMaxRadius(radius);

        Vector pos = call.session()
            .getPlacementPosition(player);
        int affected = call.editSession()
            .fillXZ(pos, pattern, radius, depth, true);
        player.print(affected + " block(s) have been created.");
    }

    private static void drain(CommandCall call) throws Exception {
        Player player = call.player();
        double radius = Math.max(0, call.decimal("radius"));
        call.worldEdit()
            .checkMaxRadius(radius);
        int affected = call.editSession()
            .drainArea(
                call.session()
                    .getPlacementPosition(player),
                radius);
        player.print(affected + " block(s) have been changed.");
    }

    private static void fixLiquid(CommandCall call, int flowingId, int stationaryId) throws Exception {
        Player player = call.player();
        double radius = Math.max(0, call.decimal("radius"));
        call.worldEdit()
            .checkMaxRadius(radius);
        int affected = call.editSession()
            .fixLiquid(
                call.session()
                    .getPlacementPosition(player),
                radius,
                flowingId,
                stationaryId);
        player.print(affected + " block(s) have been changed.");
    }

    private static void removeAbove(CommandCall call, int requestedSize, Integer requestedHeight) throws Exception {
        Player player = call.player();
        int size = Math.max(1, requestedSize);
        call.worldEdit()
            .checkMaxRadius(size);
        World world = player.getWorld();
        int height = requestedHeight != null ? Math.min((world.getMaxY() + 1), requestedHeight + 2)
            : (world.getMaxY() + 1);

        int affected = call.editSession()
            .removeAbove(
                call.session()
                    .getPlacementPosition(player),
                size,
                height);
        player.print(affected + " block(s) have been removed.");
    }

    private static void removeBelow(CommandCall call, int requestedSize, Integer requestedHeight) throws Exception {
        Player player = call.player();
        int size = Math.max(1, requestedSize);
        call.worldEdit()
            .checkMaxRadius(size);
        World world = player.getWorld();
        int height = requestedHeight != null ? Math.min((world.getMaxY() + 1), requestedHeight + 2)
            : (world.getMaxY() + 1);

        int affected = call.editSession()
            .removeBelow(
                call.session()
                    .getPlacementPosition(player),
                size,
                height);
        player.print(affected + " block(s) have been removed.");
    }

    private static void removeNear(CommandCall call, int requestedSize) throws Exception {
        Player player = call.player();
        BaseBlock block = CommandInput.block(player, call.string("block"), true, false);
        int size = Math.max(1, requestedSize);
        call.worldEdit()
            .checkMaxRadius(size);

        int affected = call.editSession()
            .removeNear(
                call.session()
                    .getPlacementPosition(player),
                block.getType(),
                size);
        player.print(affected + " block(s) have been removed.");
    }

    /**
     * @param fromBlocks the comma separated blocks to replace, or null to replace everything
     * @param to         the pattern to replace them with
     */
    private static void replaceNear(CommandCall call, String fromBlocks, String to) throws Exception {
        Player player = call.player();
        int size = Math.max(1, call.integer("size"));
        Set<BaseBlock> from = null;
        if (fromBlocks != null) {
            from = CommandInput.blocks(player, fromBlocks, true, !call.flag('f'));
        }
        Pattern pattern = CommandInput.pattern(player, to);

        Vector base = call.session()
            .getPlacementPosition(player);
        Vector min = base.subtract(size, size, size);
        Vector max = base.add(size, size, size);
        Region region = new CuboidRegion(player.getWorld(), min, max);

        int affected = call.editSession()
            .replaceBlocks(region, from, pattern);
        player.print(affected + " block(s) have been replaced.");
    }

    private static void snow(CommandCall call, double requestedSize) throws Exception {
        Player player = call.player();
        double size = Math.max(1, requestedSize);
        int affected = call.editSession()
            .simulateSnow(
                call.session()
                    .getPlacementPosition(player),
                size);
        player.print(affected + " surfaces covered. Let it snow~");
    }

    private static void thaw(CommandCall call, double requestedSize) throws Exception {
        Player player = call.player();
        double size = Math.max(1, requestedSize);
        int affected = call.editSession()
            .thaw(
                call.session()
                    .getPlacementPosition(player),
                size);
        player.print(affected + " surfaces thawed.");
    }

    private static void green(CommandCall call, double requestedSize) throws Exception {
        Player player = call.player();
        double size = Math.max(1, requestedSize);
        boolean onlyNormalDirt = !call.flag('f');

        int affected = call.editSession()
            .green(
                call.session()
                    .getPlacementPosition(player),
                size,
                onlyNormalDirt);
        player.print(affected + " surfaces greened.");
    }

    private static void extinguish(CommandCall call, Integer requestedRadius) throws Exception {
        Player player = call.player();
        LocalConfiguration config = call.worldEdit()
            .getConfiguration();

        int defaultRadius = config.maxRadius != -1 ? Math.min(40, config.maxRadius) : 40;
        int size = requestedRadius != null ? Math.max(1, requestedRadius) : defaultRadius;
        call.worldEdit()
            .checkMaxRadius(size);

        int affected = call.editSession()
            .removeNear(
                call.session()
                    .getPlacementPosition(player),
                51,
                size);
        player.print(affected + " block(s) have been removed.");
    }

    /**
     * Entities within a radius of the player, or all loaded ones if the radius is negative.
     */
    private static List<? extends Entity> entitiesAround(EditSession editSession, Vector center, int radius) {
        if (radius >= 0) {
            CylinderRegion region = CylinderRegion.createRadius(editSession, center, radius);
            return editSession.getEntities(region);
        }
        return editSession.getEntities();
    }

    private static void butcher(CommandCall call, Integer requestedRadius) throws Exception {
        WorldEdit worldEdit = call.worldEdit();
        LocalConfiguration config = worldEdit.getConfiguration();
        Player player = call.player();

        // technically the default can be larger than the max, but that's not my problem
        int radius = config.butcherDefaultRadius;

        if (requestedRadius != null) { // user inputted radius, override the default
            radius = requestedRadius;
            if (config.butcherMaxRadius != -1) { // clamp if there is a max
                if (radius == -1) {
                    radius = config.butcherMaxRadius;
                } else { // Math.min does not work if radius is -1 (actually highest possible value)
                    radius = Math.min(radius, config.butcherMaxRadius);
                }
            }
        }

        CreatureButcher flags = new CreatureButcher(player);
        flags.fromCommand(call);

        LocalSession session = call.session();
        EditSession editSession = session.createEditSession(player);
        EntityVisitor visitor = new EntityVisitor(
            entitiesAround(editSession, session.getPlacementPosition(player), radius).iterator(),
            flags.createFunction(
                editSession.getWorld()
                    .getWorldData()
                    .getEntityRegistry()));

        Operations.completeLegacy(visitor);
        int killed = visitor.getAffected();

        player.print(
            "Killed " + killed
                + (killed != 1 ? " mobs" : " mob")
                + (radius < 0 ? "" : " in a radius of " + radius)
                + ".");

        session.remember(editSession);
        editSession.flushQueue();
    }

    private static void remove(CommandCall call) throws Exception {
        Player player = call.player();
        int radius = call.integer("radius");

        if (radius < -1) {
            player.printError("Use -1 to remove all entities in loaded chunks");
            return;
        }

        EntityRemover remover = new EntityRemover();
        remover.fromString(call.string("type"));

        LocalSession session = call.session();
        EditSession editSession = session.createEditSession(player);
        EntityVisitor visitor = new EntityVisitor(
            entitiesAround(editSession, session.getPlacementPosition(player), radius).iterator(),
            remover.createFunction(
                editSession.getWorld()
                    .getWorldData()
                    .getEntityRegistry()));

        Operations.completeLegacy(visitor);
        int removed = visitor.getAffected();

        player.print("Marked " + removed + (removed != 1 ? " entities" : " entity") + " for removal.");

        session.remember(editSession);
        editSession.flushQueue();
    }

    private static void calc(CommandCall call) {
        Actor actor = call.actor();
        String input = call.string("expression");
        try {
            Expression expression = Expression.compile(input);
            actor.print("= " + expression.evaluate());
        } catch (EvaluationException e) {
            actor.printError(String.format("'%s' could not be parsed as a valid expression", input));
        } catch (ExpressionException e) {
            actor.printError(String.format("'%s' could not be evaluated (error: %s)", input, e.getMessage()));
        }
    }
}
