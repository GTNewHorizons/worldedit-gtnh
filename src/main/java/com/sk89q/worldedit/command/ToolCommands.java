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
import static com.sk89q.worldedit.command.brigadier.Nodes.word;

import net.minecraft.command.ICommandSender;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.ItemType;
import com.sk89q.worldedit.command.brigadier.CommandCall;
import com.sk89q.worldedit.command.brigadier.Nodes;
import com.sk89q.worldedit.command.tool.BlockDataCyler;
import com.sk89q.worldedit.command.tool.BlockReplacer;
import com.sk89q.worldedit.command.tool.DistanceWand;
import com.sk89q.worldedit.command.tool.FloatingTreeRemover;
import com.sk89q.worldedit.command.tool.FloodFillTool;
import com.sk89q.worldedit.command.tool.LongRangeBuildTool;
import com.sk89q.worldedit.command.tool.QueryTool;
import com.sk89q.worldedit.command.tool.Tool;
import com.sk89q.worldedit.command.tool.TreePlanter;
import com.sk89q.worldedit.command.util.CommandInput;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.util.TreeGenerator;

/**
 * Commands that bind a tool to the item in your hand.
 *
 * <p>
 * Every tool command can be typed on its own ({@code //info}) and as a part of the group ({@code //tool info}).
 * </p>
 *
 * <ul>
 * <li>{@code none}: unbind the tool from the item in your hand</li>
 * <li>{@code info}, {@code tree [type]}, {@code repl <block>}, {@code cycler}, {@code floodfill <pattern> <range>},
 * {@code deltree}, {@code farwand}, {@code lrbuild <leftclick block> <rightclick block>}</li>
 * </ul>
 */
public final class ToolCommands {

    private ToolCommands() {}

    // spotless:off
    public static void register(CommandDispatcher<ICommandSender> dispatcher) {
        LiteralCommandNode<ICommandSender> none = literal("none", "Unbind a bound tool from your current item")
                .executes(run(null, ToolCommands::none)).build();

        LiteralCommandNode<ICommandSender> info = literal("info", "Block information tool")
                .executes(run("worldedit.tool.info", ToolCommands::info)).build();

        LiteralCommandNode<ICommandSender> tree = literal("tree", "Tree generator tool")
                .executes(run("worldedit.tool.tree", c -> tree(c, null)))
                .then(word("type")
                        .executes(run("worldedit.tool.tree", c -> tree(c, c.string("type")))))
                .build();

        LiteralCommandNode<ICommandSender> repl = literal("repl", "Block replacer tool").executes(Nodes.USAGE)
                .then(word("block")
                        .executes(run("worldedit.tool.replacer", ToolCommands::repl)))
                .build();

        LiteralCommandNode<ICommandSender> cycler = literal("cycler", "Block data cycler tool")
                .executes(run("worldedit.tool.data-cycler", ToolCommands::cycler)).build();

        LiteralCommandNode<ICommandSender> floodFill = literal("floodfill", "Flood fill tool").executes(Nodes.USAGE)
                .then(word("pattern").executes(Nodes.USAGE)
                        .then(integer("range")
                                .executes(run("worldedit.tool.flood-fill", ToolCommands::floodFill))))
                .build();

        LiteralCommandNode<ICommandSender> delTree = literal("deltree", "Floating tree remover tool")
                .executes(run("worldedit.tool.deltree", ToolCommands::delTree)).build();

        LiteralCommandNode<ICommandSender> farWand = literal("farwand", "Wand at a distance tool")
                .executes(run("worldedit.tool.farwand", ToolCommands::farWand)).build();

        LiteralCommandNode<ICommandSender> longRangeBuild = literal("lrbuild", "Long-range building tool")
                .executes(Nodes.USAGE)
                .then(word("leftclick").executes(Nodes.USAGE)
                        .then(word("rightclick")
                                .executes(run("worldedit.tool.lrbuild", ToolCommands::longRangeBuild))))
                .build();

        Nodes.register(dispatcher, literal("/tool", "Bind functions to held items").executes(Nodes.USAGE)
                .then(none)
                .then(info)
                .then(tree)
                .then(repl)
                .then(cycler)
                .then(floodFill)
                .then(Nodes.alias("flood", floodFill))
                .then(delTree)
                .then(farWand)
                .then(longRangeBuild));
    }
    // spotless:on

    private static void bind(CommandCall call, Tool tool, String message) throws Exception {
        Player player = call.player();
        call.session()
            .setTool(player.getItemInHand(), tool);
        player.print(message + " bound to " + ItemType.toHeldName(player.getItemInHand()) + ".");
    }

    private static void none(CommandCall call) throws Exception {
        Player player = call.player();
        call.session()
            .setTool(player.getItemInHand(), null);
        player.print("Tool unbound from your current item.");
    }

    private static void info(CommandCall call) throws Exception {
        bind(call, new QueryTool(), "Info tool");
    }

    @SuppressWarnings("deprecation")
    private static void tree(CommandCall call, String typeName) throws Exception {
        Player player = call.player();
        TreeGenerator.TreeType type = typeName != null ? TreeGenerator.lookup(typeName) : TreeGenerator.TreeType.TREE;
        if (type == null) {
            player.printError("Tree type '" + typeName + "' is unknown.");
            return;
        }
        bind(call, new TreePlanter(new TreeGenerator(type)), "Tree tool");
    }

    private static void repl(CommandCall call) throws Exception {
        BaseBlock targetBlock = CommandInput.block(call.player(), call.string("block"), false, false);
        bind(call, new BlockReplacer(targetBlock), "Block replacer tool");
    }

    private static void cycler(CommandCall call) throws Exception {
        bind(call, new BlockDataCyler(), "Block data cycler tool");
    }

    private static void floodFill(CommandCall call) throws Exception {
        Player player = call.player();
        LocalConfiguration config = call.worldEdit()
            .getConfiguration();
        int range = call.integer("range");
        if (range > config.maxSuperPickaxeSize) {
            player.printError("Maximum range: " + config.maxSuperPickaxeSize);
            return;
        }
        Pattern pattern = CommandInput.pattern(player, call.string("pattern"));
        bind(call, new FloodFillTool(range, pattern), "Block flood fill tool");
    }

    private static void delTree(CommandCall call) throws Exception {
        bind(call, new FloatingTreeRemover(), "Floating tree remover tool");
    }

    private static void farWand(CommandCall call) throws Exception {
        bind(call, new DistanceWand(), "Far wand tool");
    }

    private static void longRangeBuild(CommandCall call) throws Exception {
        Player player = call.player();
        LocalSession session = call.session();
        BaseBlock secondary = CommandInput.block(player, call.string("leftclick"), false, false);
        BaseBlock primary = CommandInput.block(player, call.string("rightclick"), false, false);
        session.setTool(player.getItemInHand(), new LongRangeBuildTool(primary, secondary));
        player.print("Long-range building tool bound to " + ItemType.toHeldName(player.getItemInHand()) + ".");
        player.print(
            "Left-click set to " + ItemType.toName(secondary.getType())
                + "; right-click set to "
                + ItemType.toName(primary.getType())
                + ".");
    }
}
