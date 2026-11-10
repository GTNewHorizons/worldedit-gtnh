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

import com.mojang.brigadier.tree.LiteralCommandNode;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.command.brigadier.CommandCall;
import com.sk89q.worldedit.command.brigadier.Nodes;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;

/**
 * Commands that change how the tools work.
 *
 * <ul>
 * <li>{@code //} (alias {@code ,}) {@code [on|off]}: toggle the super pickaxe</li>
 * <li>{@code mask [mask]}, {@code mat <pattern>} (alias {@code material}), {@code range <range>},
 * {@code size <radius>}: change the brush held in your hand</li>
 * </ul>
 */
public final class ToolUtilCommands {

    private ToolUtilCommands() {}

    // spotless:off
    static LiteralCommandNode<ICommandSender> maskNode() {
        return literal("mask", "Set the brush mask")
                .executes(run("worldedit.brush.options.mask", c -> mask(c.player(), c.session(), null)))
                .then(word("mask")
                        .executes(run("worldedit.brush.options.mask",
                                c -> mask(c.player(), c.session(), c.mask("mask")))))
                .build();
    }

    static LiteralCommandNode<ICommandSender> materialNode() {
        return literal("material", "Set the brush material").executes(Nodes.USAGE)
                .then(word("pattern")
                        .executes(run("worldedit.brush.options.material",
                                c -> material(c.player(), c.session(), c.pattern("pattern")))))
                .build();
    }

    static LiteralCommandNode<ICommandSender> rangeNode() {
        return literal("range", "Set the brush range").executes(Nodes.USAGE)
                .then(integer("range")
                        .executes(run("worldedit.brush.options.range", ToolUtilCommands::range)))
                .build();
    }

    static LiteralCommandNode<ICommandSender> sizeNode() {
        return literal("size", "Set the brush size").executes(Nodes.USAGE)
                .then(integer("radius")
                        .executes(run("worldedit.brush.options.size", ToolUtilCommands::size)))
                .build();
    }
    // spotless:on

    static void togglePickaxe(Player player, LocalSession session, String newState) {
        if (session.hasSuperPickAxe()) {
            if ("on".equals(newState)) {
                player.printError("Super pick axe already enabled.");
                return;
            }

            session.disableSuperPickAxe();
            player.print("Super pick axe disabled.");
        } else {
            if ("off".equals(newState)) {
                player.printError("Super pick axe already disabled.");
                return;
            }

            session.enableSuperPickAxe();
            player.print("Super pick axe enabled.");
        }
    }

    private static void mask(Player player, LocalSession session, Mask mask) throws Exception {
        session.getBrushTool(player.getItemInHand())
            .setMask(mask);
        player.print(mask == null ? "Brush mask disabled." : "Brush mask set.");
    }

    private static void material(Player player, LocalSession session, Pattern pattern) throws Exception {
        session.getBrushTool(player.getItemInHand())
            .setFill(pattern);
        player.print("Brush material set.");
    }

    private static void range(CommandCall call) throws Exception {
        Player player = call.player();
        call.session()
            .getBrushTool(player.getItemInHand())
            .setRange(call.integer("range"));
        player.print("Brush range set.");
    }

    private static void size(CommandCall call) throws Exception {
        Player player = call.player();
        int radius = call.integer("radius");
        call.worldEdit()
            .checkMaxBrushRadius(radius);
        call.session()
            .getBrushTool(player.getItemInHand())
            .setSize(radius);
        player.print("Brush size set.");
    }
}
