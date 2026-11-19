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

import net.minecraft.command.ICommandSender;

import com.mojang.brigadier.CommandDispatcher;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.brigadier.LogMode;
import com.sk89q.worldedit.command.brigadier.Nodes;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.util.TreeGenerator.TreeType;
import com.sk89q.worldedit.world.biome.BaseBiome;

/**
 * Commands for the generation of shapes and other objects.
 */
public final class GenerationCommands {

    private GenerationCommands() {}

    // spotless:off
    public static void register(CommandDispatcher<ICommandSender> dispatcher) {
        Nodes.register(dispatcher, literal("/hcyl", "Generates a hollow cylinder").executes(Nodes.USAGE)
                .then(word("pattern").executes(Nodes.USAGE)
                        .then(word("radius")
                                .executes(run("worldedit.generation.cylinder", LogMode.PLACEMENT, c ->
                                        hcyl(c.player(), c.session(), c.editSession(), c.pattern("pattern"), c.string("radius"), 1)))
                                .then(integer("height")
                                        .executes(run("worldedit.generation.cylinder", LogMode.PLACEMENT, c ->
                                                hcyl(c.player(), c.session(), c.editSession(), c.pattern("pattern"), c.string("radius"), c.integer("height"))))))));

        Nodes.registerWithFlags(dispatcher, literal("/cyl", "Generates a cylinder").executes(Nodes.USAGE)
                .then(word("pattern").executes(Nodes.USAGE)
                        .then(word("radius")
                                .executes(run("worldedit.generation.cylinder", LogMode.PLACEMENT, c ->
                                        cyl(c.player(), c.session(), c.editSession(), c.pattern("pattern"), c.string("radius"), 1, c.flag('h'))))
                                .then(integer("height")
                                        .executes(run("worldedit.generation.cylinder", LogMode.PLACEMENT, c ->
                                                cyl(c.player(), c.session(), c.editSession(), c.pattern("pattern"), c.string("radius"), c.integer("height"), c.flag('h'))))))),
                "h");

        Nodes.register(dispatcher, literal("/hsphere", "Generates a hollow sphere").executes(Nodes.USAGE)
                .then(word("pattern").executes(Nodes.USAGE)
                        .then(word("radius")
                                .executes(run("worldedit.generation.sphere", LogMode.PLACEMENT, c ->
                                        hsphere(c.player(), c.session(), c.editSession(), c.pattern("pattern"), c.string("radius"), false)))
                                .then(word("raised", "yes", "no")
                                        .executes(run("worldedit.generation.sphere", LogMode.PLACEMENT, c ->
                                                hsphere(c.player(), c.session(), c.editSession(), c.pattern("pattern"), c.string("radius"), c.yesNo("raised"))))))));

        Nodes.registerWithFlags(dispatcher, literal("/sphere", "Generates a filled sphere").executes(Nodes.USAGE)
                .then(word("pattern").executes(Nodes.USAGE)
                        .then(word("radius")
                                .executes(run("worldedit.generation.sphere", LogMode.PLACEMENT, c ->
                                        sphere(c.player(), c.session(), c.editSession(), c.pattern("pattern"), c.string("radius"), false, c.flag('h'))))
                                .then(word("raised", "yes", "no")
                                        .executes(run("worldedit.generation.sphere", LogMode.PLACEMENT, c ->
                                                sphere(c.player(), c.session(), c.editSession(), c.pattern("pattern"), c.string("radius"), c.yesNo("raised"), c.flag('h'))))))),
                "h");

        Nodes.register(dispatcher, literal("/forestgen", "Generate a forest")
                .executes(run("worldedit.generation.forest", LogMode.POSITION, c ->
                        forestGen(c.player(), c.session(), c.editSession(), 10, TreeType.TREE, 5)))
                .then(integer("size")
                        .executes(run("worldedit.generation.forest", LogMode.POSITION, c ->
                                forestGen(c.player(), c.session(), c.editSession(), c.integer("size"), TreeType.TREE, 5)))
                        .then(word("type")
                                .executes(run("worldedit.generation.forest", LogMode.POSITION, c ->
                                        forestGen(c.player(), c.session(), c.editSession(), c.integer("size"), c.treeType("type"), 5)))
                                .then(decimal("density")
                                        .executes(run("worldedit.generation.forest", LogMode.POSITION, c ->
                                                forestGen(c.player(), c.session(), c.editSession(), c.integer("size"), c.treeType("type"), c.decimal("density"))))))));

        Nodes.register(dispatcher, literal("/pumpkins", "Generate pumpkin patches")
                .executes(run("worldedit.generation.pumpkins", LogMode.POSITION, c ->
                        pumpkins(c.player(), c.session(), c.editSession(), 10)))
                .then(integer("size")
                        .executes(run("worldedit.generation.pumpkins", LogMode.POSITION, c ->
                                pumpkins(c.player(), c.session(), c.editSession(), c.integer("size"))))));

        Nodes.register(dispatcher, literal("/hpyramid", "Generate a hollow pyramid").executes(Nodes.USAGE)
                .then(word("pattern").executes(Nodes.USAGE)
                        .then(integer("size", 1)
                                .executes(run("worldedit.generation.pyramid", LogMode.PLACEMENT, c ->
                                        hollowPyramid(c.player(), c.session(), c.editSession(), c.pattern("pattern"), c.integer("size")))))));

        Nodes.registerWithFlags(dispatcher, literal("/pyramid", "Generate a filled pyramid").executes(Nodes.USAGE)
                .then(word("pattern").executes(Nodes.USAGE)
                        .then(integer("size", 1)
                                .executes(run("worldedit.generation.pyramid", LogMode.PLACEMENT, c ->
                                        pyramid(c.player(), c.session(), c.editSession(), c.pattern("pattern"), c.integer("size"), c.flag('h')))))),
                "h");

        Nodes.registerWithFlags(dispatcher, literal("/generate", "Generates a shape according to a formula").executes(Nodes.USAGE)
                .then(word("pattern").executes(Nodes.USAGE)
                        .then(text("expression")
                                .executes(run("worldedit.generation.shape", LogMode.ALL, c ->
                                        generate(c.player(), c.session(), c.editSession(), c.region(), c.pattern("pattern"), c.string("expression"),
                                                c.flag('h'), c.flag('r'), c.flag('o'), c.flag('c')))))),
                "hroc", "/gen", "/g");

        Nodes.registerWithFlags(dispatcher, literal("/generatebiome", "Sets biome according to a formula").executes(Nodes.USAGE)
                .then(word("biome").executes(Nodes.USAGE)
                        .then(text("expression")
                                .executes(run("worldedit.generation.shape|worldedit.biome.set", LogMode.ALL, c ->
                                        generateBiome(c.player(), c.session(), c.editSession(), c.region(), c.biome("biome"), c.string("expression"),
                                                c.flag('h'), c.flag('r'), c.flag('o'), c.flag('c')))))),
                "hroc", "/genbiome", "/gb");
    }
    // spotless:on

    private static void hcyl(Player player, LocalSession session, EditSession editSession, Pattern pattern,
        String radiusString, int height) throws WorldEditException {
        cyl(player, session, editSession, pattern, radiusString, height, true);
    }

    private static void cyl(Player player, LocalSession session, EditSession editSession, Pattern pattern,
        String radiusString, int height, boolean hollow) throws WorldEditException {
        String[] radii = radiusString.split(",");
        final double radiusX, radiusZ;
        switch (radii.length) {
            case 1:
                radiusX = radiusZ = Math.max(1, Double.parseDouble(radii[0]));
                break;

            case 2:
                radiusX = Math.max(1, Double.parseDouble(radii[0]));
                radiusZ = Math.max(1, Double.parseDouble(radii[1]));
                break;

            default:
                player.printError("You must either specify 1 or 2 radius values.");
                return;
        }

        WorldEdit.getInstance()
            .checkMaxRadius(radiusX);
        WorldEdit.getInstance()
            .checkMaxRadius(radiusZ);
        WorldEdit.getInstance()
            .checkMaxRadius(height);

        Vector pos = session.getPlacementPosition(player);
        int affected = editSession.makeCylinder(pos, pattern, radiusX, radiusZ, height, !hollow);
        player.print(affected + " block(s) have been created.");
    }

    private static void hsphere(Player player, LocalSession session, EditSession editSession, Pattern pattern,
        String radiusString, boolean raised) throws WorldEditException {
        sphere(player, session, editSession, pattern, radiusString, raised, true);
    }

    private static void sphere(Player player, LocalSession session, EditSession editSession, Pattern pattern,
        String radiusString, boolean raised, boolean hollow) throws WorldEditException {
        String[] radii = radiusString.split(",");
        final double radiusX, radiusY, radiusZ;
        switch (radii.length) {
            case 1:
                radiusX = radiusY = radiusZ = Math.max(1, Double.parseDouble(radii[0]));
                break;

            case 3:
                radiusX = Math.max(1, Double.parseDouble(radii[0]));
                radiusY = Math.max(1, Double.parseDouble(radii[1]));
                radiusZ = Math.max(1, Double.parseDouble(radii[2]));
                break;

            default:
                player.printError("You must either specify 1 or 3 radius values.");
                return;
        }

        WorldEdit.getInstance()
            .checkMaxRadius(radiusX);
        WorldEdit.getInstance()
            .checkMaxRadius(radiusY);
        WorldEdit.getInstance()
            .checkMaxRadius(radiusZ);

        Vector pos = session.getPlacementPosition(player);
        if (raised) {
            pos = pos.add(0, radiusY, 0);
        }

        int affected = editSession.makeSphere(pos, pattern, radiusX, radiusY, radiusZ, !hollow);
        player.findFreePosition();
        player.print(affected + " block(s) have been created.");
    }

    @SuppressWarnings("deprecation")
    private static void forestGen(Player player, LocalSession session, EditSession editSession, int size, TreeType type,
        double density) throws WorldEditException {
        density = density / 100;
        int affected = editSession
            .makeForest(session.getPlacementPosition(player), size, density, new TreeGenerator(type));
        player.print(affected + " trees created.");
    }

    private static void pumpkins(Player player, LocalSession session, EditSession editSession, int apothem)
        throws WorldEditException {
        int affected = editSession.makePumpkinPatches(session.getPlacementPosition(player), apothem);
        player.print(affected + " pumpkin patches created.");
    }

    private static void hollowPyramid(Player player, LocalSession session, EditSession editSession, Pattern pattern,
        int size) throws WorldEditException {
        pyramid(player, session, editSession, pattern, size, true);
    }

    private static void pyramid(Player player, LocalSession session, EditSession editSession, Pattern pattern, int size,
        boolean hollow) throws WorldEditException {
        Vector pos = session.getPlacementPosition(player);
        WorldEdit.getInstance()
            .checkMaxRadius(size);
        int affected = editSession.makePyramid(pos, pattern, size, !hollow);
        player.findFreePosition();
        player.print(affected + " block(s) have been created.");
    }

    private static void generate(Player player, LocalSession session, EditSession editSession, Region region,
        Pattern pattern, String expression, boolean hollow, boolean useRawCoords, boolean offset, boolean offsetCenter)
        throws WorldEditException {

        final Vector zero;
        Vector unit;

        if (useRawCoords) {
            zero = Vector.ZERO;
            unit = Vector.ONE;
        } else if (offset) {
            zero = session.getPlacementPosition(player);
            unit = Vector.ONE;
        } else if (offsetCenter) {
            final Vector min = region.getMinimumPoint();
            final Vector max = region.getMaximumPoint();

            zero = max.add(min)
                .multiply(0.5);
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
            final int affected = editSession.makeShape(region, zero, unit, pattern, expression, hollow);
            player.findFreePosition();
            player.print(affected + " block(s) have been created.");
        } catch (ExpressionException e) {
            player.printError(e.getMessage());
        }
    }

    private static void generateBiome(Player player, LocalSession session, EditSession editSession, Region region,
        BaseBiome target, String expression, boolean hollow, boolean useRawCoords, boolean offset, boolean offsetCenter)
        throws WorldEditException {
        final Vector zero;
        Vector unit;

        if (useRawCoords) {
            zero = Vector.ZERO;
            unit = Vector.ONE;
        } else if (offset) {
            zero = session.getPlacementPosition(player);
            unit = Vector.ONE;
        } else if (offsetCenter) {
            final Vector min = region.getMinimumPoint();
            final Vector max = region.getMaximumPoint();

            zero = max.add(min)
                .multiply(0.5);
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
            final int affected = editSession.makeBiomeShape(region, zero, unit, target, expression, hollow);
            player.findFreePosition();
            player.print("" + affected + " columns affected.");
        } catch (ExpressionException e) {
            player.printError(e.getMessage());
        }
    }

}
