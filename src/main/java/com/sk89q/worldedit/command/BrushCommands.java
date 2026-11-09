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

import static com.google.common.base.Objects.firstNonNull;
import static com.sk89q.worldedit.command.brigadier.CommandRunner.run;
import static com.sk89q.worldedit.command.brigadier.Nodes.decimal;
import static com.sk89q.worldedit.command.brigadier.Nodes.integer;
import static com.sk89q.worldedit.command.brigadier.Nodes.literal;
import static com.sk89q.worldedit.command.brigadier.Nodes.word;

import net.minecraft.command.ICommandSender;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.command.brigadier.CommandCall;
import com.sk89q.worldedit.command.brigadier.CommandFailure;
import com.sk89q.worldedit.command.brigadier.Nodes;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.brush.ButcherBrush;
import com.sk89q.worldedit.command.tool.brush.ClipboardBrush;
import com.sk89q.worldedit.command.tool.brush.CylinderBrush;
import com.sk89q.worldedit.command.tool.brush.GravityBrush;
import com.sk89q.worldedit.command.tool.brush.HollowCylinderBrush;
import com.sk89q.worldedit.command.tool.brush.HollowSphereBrush;
import com.sk89q.worldedit.command.tool.brush.OperationFactoryBrush;
import com.sk89q.worldedit.command.tool.brush.SmoothBrush;
import com.sk89q.worldedit.command.tool.brush.SphereBrush;
import com.sk89q.worldedit.command.util.CreatureButcher;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.NullExtent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.Contextual;
import com.sk89q.worldedit.function.EditContext;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.block.BlockReplace;
import com.sk89q.worldedit.function.factory.Apply;
import com.sk89q.worldedit.function.factory.Deform;
import com.sk89q.worldedit.function.factory.Deform.Mode;
import com.sk89q.worldedit.function.factory.Paint;
import com.sk89q.worldedit.function.generator.ForestGenerator;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.pattern.BlockPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.regions.factory.CuboidRegionFactory;
import com.sk89q.worldedit.regions.factory.CylinderRegionFactory;
import com.sk89q.worldedit.regions.factory.RegionFactory;
import com.sk89q.worldedit.regions.factory.SphereRegionFactory;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.util.TreeGenerator.TreeType;
import com.sk89q.worldedit.world.World;

/**
 * Commands to set brush shape.
 */
public final class BrushCommands {

    private BrushCommands() {}

    private static final String[] SHAPES = { "cuboid", "sphere", "cyl" };

    // spotless:off
    public static void register(CommandDispatcher<ICommandSender> dispatcher) {
        LiteralCommandNode<ICommandSender> sphere = Nodes.withFlags(literal("sphere", "Choose the sphere brush").executes(Nodes.USAGE)
                .then(word("pattern")
                        .executes(run("worldedit.brush.sphere", c -> sphereBrush(c.player(), c.session(), c.pattern("pattern"), 2, c.flag('h'))))
                        .then(decimal("radius")
                                .executes(run("worldedit.brush.sphere", c -> sphereBrush(c.player(), c.session(), c.pattern("pattern"), c.decimal("radius"), c.flag('h')))))),
                "h");

        LiteralCommandNode<ICommandSender> cylinder = Nodes.withFlags(literal("cylinder", "Choose the cylinder brush").executes(Nodes.USAGE)
                .then(word("pattern")
                        .executes(run("worldedit.brush.cylinder", c -> cylinderBrush(c.player(), c.session(), c.pattern("pattern"), 2, 1, c.flag('h'))))
                        .then(decimal("radius")
                                .executes(run("worldedit.brush.cylinder", c -> cylinderBrush(c.player(), c.session(), c.pattern("pattern"), c.decimal("radius"), 1, c.flag('h'))))
                                .then(integer("height")
                                        .executes(run("worldedit.brush.cylinder", c -> cylinderBrush(c.player(), c.session(), c.pattern("pattern"), c.decimal("radius"), c.integer("height"), c.flag('h'))))))),
                "h");

        LiteralCommandNode<ICommandSender> clipboard = Nodes.withFlags(literal("clipboard", "Choose the clipboard brush")
                .executes(run("worldedit.brush.clipboard", c -> clipboardBrush(c.player(), c.session(), c.flag('a'), c.flag('p')))),
                "ap");

        LiteralCommandNode<ICommandSender> smooth = Nodes.withFlags(literal("smooth", "Choose the terrain softener brush")
                .executes(run("worldedit.brush.smooth", c -> smoothBrush(c.player(), c.session(), 2, 4, c.flag('n'))))
                .then(decimal("size")
                        .executes(run("worldedit.brush.smooth", c -> smoothBrush(c.player(), c.session(), c.decimal("size"), 4, c.flag('n'))))
                        .then(integer("iterations")
                                .executes(run("worldedit.brush.smooth", c -> smoothBrush(c.player(), c.session(), c.decimal("size"), c.integer("iterations"), c.flag('n')))))),
                "n");

        LiteralCommandNode<ICommandSender> extinguish = literal("ex", "Shortcut fire extinguisher brush")
                .executes(run("worldedit.brush.ex", c -> extinguishBrush(c.player(), c.session(), c.editSession(), 5)))
                .then(decimal("radius")
                        .executes(run("worldedit.brush.ex", c -> extinguishBrush(c.player(), c.session(), c.editSession(), c.decimal("radius")))))
                .build();

        LiteralCommandNode<ICommandSender> gravity = Nodes.withFlags(literal("gravity", "Gravity brush")
                .executes(run("worldedit.brush.gravity", c -> gravityBrush(c.player(), c.session(), 5, c.flag('h'))))
                .then(decimal("radius")
                        .executes(run("worldedit.brush.gravity", c -> gravityBrush(c.player(), c.session(), c.decimal("radius"), c.flag('h'))))),
                "h");

        LiteralCommandNode<ICommandSender> butcher = Nodes.withFlags(Nodes.sub("brush", "butcher", "Butcher brush")
                .executes(run("worldedit.brush.butcher", c -> butcherBrush(c.player(), c.session(), c)))
                .then(decimal("radius")
                        .executes(run("worldedit.brush.butcher", c -> butcherBrush(c.player(), c.session(), c)))),
                "plangbtfr");

        // //brush set <shape> <size> <pattern>
        LiteralCommandNode<ICommandSender> set = literal("set", "Set all blocks within region").executes(Nodes.USAGE)
                .then(word("shape", SHAPES).executes(Nodes.USAGE)
                        .then(decimal("size").executes(Nodes.USAGE)
                                .then(word("pattern")
                                        .executes(run("worldedit.brush.set", c ->
                                                shaped(c, "worldedit.brush.set", new Apply(new ReplaceFunction(c.pattern("pattern")))))))))
                .build();

        // //brush apply <shape> <size> forest <tree> | item <item> | set <pattern>
        LiteralCommandNode<ICommandSender> apply = literal("apply", "Applies a function to every block").executes(Nodes.USAGE)
                .then(word("shape", SHAPES).executes(Nodes.USAGE)
                        .then(functions(decimal("size").executes(Nodes.USAGE), "worldedit.brush.apply", (c, function) -> new Apply(function))))
                .build();

        // //brush paint <shape> <size> <density> forest <tree> | item <item> | set <pattern>
        LiteralCommandNode<ICommandSender> paint = literal("paint", "Applies a function to surfaces").executes(Nodes.USAGE)
                .then(word("shape", SHAPES).executes(Nodes.USAGE)
                        .then(decimal("size").executes(Nodes.USAGE)
                                .then(functions(decimal("density").executes(Nodes.USAGE), "worldedit.brush.paint",
                                        (c, function) -> new Paint(function, c.decimal("density") / 100.0)))))
                .build();

        // //brush forest <shape> <size> <density> <tree>
        LiteralCommandNode<ICommandSender> forest = literal("forest", "Plants trees on surfaces").executes(Nodes.USAGE)
                .then(word("shape", SHAPES).executes(Nodes.USAGE)
                        .then(decimal("size").executes(Nodes.USAGE)
                                .then(decimal("density").executes(Nodes.USAGE)
                                        .then(word("tree")
                                                .executes(run("worldedit.brush.forest", c ->
                                                        shaped(c, "worldedit.brush.forest", new Paint(new TreeFunction(c.treeType("tree")), c.decimal("density") / 100.0))))))))
                .build();

        // //brush deform [-ro] <shape> <size> <expression>
        LiteralCommandNode<ICommandSender> deform = Nodes.withFlags(literal("deform", "Apply math expression to area").executes(Nodes.USAGE)
                .then(word("shape", SHAPES).executes(Nodes.USAGE)
                        .then(decimal("size").executes(Nodes.USAGE)
                                .then(word("expression")
                                        .executes(run("worldedit.brush.deform", BrushCommands::deformBrush))))),
                "ro");

        LiteralCommandNode<ICommandSender> raise = fixedDeform("raise", "Raise one block", "y-=1", "worldedit.brush.raise");
        LiteralCommandNode<ICommandSender> lower = fixedDeform("lower", "Lower one block", "y+=1", "worldedit.brush.lower");

        Nodes.register(dispatcher, literal("brush", "Brushing commands").executes(Nodes.USAGE)
                .then(sphere).then(Nodes.alias("s", sphere))
                .then(cylinder).then(Nodes.alias("cyl", cylinder)).then(Nodes.alias("c", cylinder))
                .then(clipboard).then(Nodes.alias("copy", clipboard))
                .then(smooth)
                .then(extinguish).then(Nodes.alias("extinguish", extinguish))
                .then(gravity).then(Nodes.alias("grav", gravity))
                .then(butcher).then(Nodes.alias("kill", butcher))
                .then(set)
                .then(apply)
                .then(paint)
                .then(forest)
                .then(deform)
                .then(raise)
                .then(lower),
                "br");
    }

    private static LiteralCommandNode<ICommandSender> fixedDeform(String name, String description, String expression, String permission) {
        return literal(name, description).executes(Nodes.USAGE)
                .then(word("shape", SHAPES).executes(Nodes.USAGE)
                        .then(decimal("size")
                                .executes(run(permission, c -> shaped(c, permission, new Deform(expression, Mode.RAW_COORD))))))
                .build();
    }

    /**
     * Adds the three kinds of block function a shaped brush can apply below {@code parent}.
     */
    private static <T extends ArgumentBuilder<ICommandSender, T>> T functions(T parent, String permission, BrushFactory factory) {
        return parent
                .then(literal("forest").executes(Nodes.USAGE)
                        .then(word("tree")
                                .executes(run(permission, c -> shaped(c, permission, factory.create(c, new TreeFunction(c.treeType("tree"))))))))
                .then(literal("item").executes(Nodes.USAGE)
                        .then(word("item")
                                .executes(run(permission, c -> shaped(c, permission, factory.create(c, new ItemFunction(c.item("item"))))))))
                .then(literal("set").executes(Nodes.USAGE)
                        .then(word("pattern")
                                .executes(run(permission, c -> shaped(c, permission, factory.create(c, new ReplaceFunction(c.pattern("pattern"))))))));
    }
    // spotless:on

    /**
     * Makes the operation of a shaped brush out of the block function that was typed.
     */
    private interface BrushFactory {

        Contextual<? extends Operation> create(CommandCall call, Contextual<? extends RegionFunction> function);
    }

    private static final class ReplaceFunction implements Contextual<RegionFunction> {

        private final Pattern fill;

        private ReplaceFunction(Pattern fill) {
            this.fill = fill;
        }

        @Override
        public RegionFunction createFromContext(EditContext context) {
            return new BlockReplace(
                firstNonNull(context.getDestination(), new NullExtent()),
                firstNonNull(context.getFill(), fill));
        }

        @Override
        public String toString() {
            return "replace blocks";
        }
    }

    private static final class ItemFunction implements Contextual<RegionFunction> {

        private final BaseItem item;

        private ItemFunction(BaseItem item) {
            this.item = item;
        }

        @Override
        public RegionFunction createFromContext(EditContext context) {
            World world = ((EditSession) context.getDestination()).getWorld();
            return new RegionFunction() {

                @Override
                public boolean apply(Vector position) throws WorldEditException {
                    return world.useItem(position, item, Direction.UP);
                }
            };
        }

        @Override
        public String toString() {
            return "application of the item " + item.getType() + ":" + item.getData();
        }
    }

    private static final class TreeFunction implements Contextual<ForestGenerator> {

        private final TreeType type;

        private TreeFunction(TreeType type) {
            this.type = type;
        }

        @Override
        public ForestGenerator createFromContext(EditContext context) {
            return new ForestGenerator((EditSession) context.getDestination(), new TreeGenerator(type));
        }

        @Override
        public String toString() {
            return "tree of type " + type;
        }
    }

    private static RegionFactory regionFactory(String shape) {
        if (shape.equals("cuboid")) {
            return new CuboidRegionFactory();
        } else if (shape.equals("sphere")) {
            return new SphereRegionFactory();
        } else if (shape.equals("cyl") || shape.equals("cylinder")) {
            return new CylinderRegionFactory(1); // TODO: Adjustable height
        }
        throw new CommandFailure("Unknown shape type: " + shape + " (try one of cuboid, sphere, cyl)");
    }

    /**
     * Binds a brush that runs an operation in a shape, the common part of set, apply, paint, forest, deform, raise and
     * lower.
     */
    private static void shaped(CommandCall call, String permission, Contextual<? extends Operation> factory)
        throws Exception {
        Player player = call.player();
        RegionFactory regionFactory = regionFactory(call.string("shape"));
        int radius = (int) call.decimal("size");

        WorldEdit.getInstance()
            .checkMaxBrushRadius(radius);
        BrushTool tool = call.session()
            .getBrushTool(player.getItemInHand());
        tool.setSize(radius);
        tool.setBrush(new OperationFactoryBrush(factory, regionFactory), permission);

        player.print("Set brush to " + factory);
    }

    private static void deformBrush(CommandCall call) throws Exception {
        Deform deform = new Deform(call.string("expression"));
        if (call.flag('r')) {
            deform.setMode(Mode.RAW_COORD);
        } else if (call.flag('o')) {
            deform.setMode(Mode.OFFSET);
            deform.setOffset(
                call.session()
                    .getPlacementPosition(call.player()));
        } else {
            deform.setMode(Mode.UNIT_CUBE);
        }
        shaped(call, "worldedit.brush.deform", deform);
    }

    private static void sphereBrush(Player player, LocalSession session, Pattern fill, double radius, boolean hollow)
        throws WorldEditException {
        WorldEdit.getInstance()
            .checkMaxBrushRadius(radius);

        BrushTool tool = session.getBrushTool(player.getItemInHand());
        tool.setFill(fill);
        tool.setSize(radius);

        if (hollow) {
            tool.setBrush(new HollowSphereBrush(), "worldedit.brush.sphere");
        } else {
            tool.setBrush(new SphereBrush(), "worldedit.brush.sphere");
        }

        player.print(String.format("Sphere brush shape equipped (%.0f).", radius));
    }

    private static void cylinderBrush(Player player, LocalSession session, Pattern fill, double radius, int height,
        boolean hollow) throws WorldEditException {
        WorldEdit.getInstance()
            .checkMaxBrushRadius(radius);
        WorldEdit.getInstance()
            .checkMaxBrushRadius(height);

        BrushTool tool = session.getBrushTool(player.getItemInHand());
        tool.setFill(fill);
        tool.setSize(radius);

        if (hollow) {
            tool.setBrush(new HollowCylinderBrush(height), "worldedit.brush.cylinder");
        } else {
            tool.setBrush(new CylinderBrush(height), "worldedit.brush.cylinder");
        }

        player.print(String.format("Cylinder brush shape equipped (%.0f by %d).", radius, height));
    }

    private static void clipboardBrush(Player player, LocalSession session, boolean ignoreAir, boolean usingOrigin)
        throws WorldEditException {
        ClipboardHolder holder = session.getClipboard();
        Clipboard clipboard = holder.getClipboard();

        Vector size = clipboard.getDimensions();

        WorldEdit.getInstance()
            .checkMaxBrushRadius(size.getBlockX());
        WorldEdit.getInstance()
            .checkMaxBrushRadius(size.getBlockY());
        WorldEdit.getInstance()
            .checkMaxBrushRadius(size.getBlockZ());

        BrushTool tool = session.getBrushTool(player.getItemInHand());
        tool.setBrush(new ClipboardBrush(holder, ignoreAir, usingOrigin), "worldedit.brush.clipboard");

        player.print("Clipboard brush shape equipped.");
    }

    private static void smoothBrush(Player player, LocalSession session, double radius, int iterations,
        boolean naturalBlocksOnly) throws WorldEditException {

        WorldEdit.getInstance()
            .checkMaxBrushRadius(radius);

        BrushTool tool = session.getBrushTool(player.getItemInHand());
        tool.setSize(radius);
        tool.setBrush(new SmoothBrush(iterations, naturalBlocksOnly), "worldedit.brush.smooth");

        player.print(
            String.format(
                "Smooth brush equipped (%.0f x %dx, using " + (naturalBlocksOnly ? "natural blocks only" : "any block")
                    + ").",
                radius,
                iterations));
    }

    private static void extinguishBrush(Player player, LocalSession session, EditSession editSession, double radius)
        throws WorldEditException {
        WorldEdit.getInstance()
            .checkMaxBrushRadius(radius);

        BrushTool tool = session.getBrushTool(player.getItemInHand());
        Pattern fill = new BlockPattern(new BaseBlock(0));
        tool.setFill(fill);
        tool.setSize(radius);
        tool.setMask(new BlockMask(editSession, new BaseBlock(BlockID.FIRE)));
        tool.setBrush(new SphereBrush(), "worldedit.brush.ex");

        player.print(String.format("Extinguisher equipped (%.0f).", radius));
    }

    private static void gravityBrush(Player player, LocalSession session, double radius, boolean fromMaxY)
        throws WorldEditException {
        WorldEdit.getInstance()
            .checkMaxBrushRadius(radius);

        BrushTool tool = session.getBrushTool(player.getItemInHand());
        tool.setSize(radius);
        tool.setBrush(new GravityBrush(fromMaxY), "worldedit.brush.gravity");

        player.print(String.format("Gravity brush equipped (%.0f).", radius));
    }

    private static void butcherBrush(Player player, LocalSession session, CommandCall args) throws WorldEditException {
        LocalConfiguration config = WorldEdit.getInstance()
            .getConfiguration();

        double radius = args.decimal("radius", 5);
        double maxRadius = config.maxBrushRadius;
        // hmmmm not horribly worried about this because -1 is still rather efficient,
        // the problem arises when butcherMaxRadius is some really high number but not infinite
        // - original idea taken from https://github.com/sk89q/worldedit/pull/198#issuecomment-6463108
        if (player.hasPermission("worldedit.butcher")) {
            maxRadius = Math.max(config.maxBrushRadius, config.butcherMaxRadius);
        }
        if (radius > maxRadius) {
            player.printError("Maximum allowed brush radius: " + maxRadius);
            return;
        }

        CreatureButcher flags = new CreatureButcher(player);
        flags.fromCommand(args);

        BrushTool tool = session.getBrushTool(player.getItemInHand());
        tool.setSize(radius);
        tool.setBrush(new ButcherBrush(flags), "worldedit.brush.butcher");

        player.print(String.format("Butcher brush equipped (%.0f).", radius));
    }
}
