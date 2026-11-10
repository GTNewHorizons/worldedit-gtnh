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
import static com.sk89q.worldedit.command.brigadier.Nodes.word;

import net.minecraft.command.ICommandSender;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.command.brigadier.CommandCall;
import com.sk89q.worldedit.command.brigadier.Nodes;
import com.sk89q.worldedit.command.tool.AreaPickaxe;
import com.sk89q.worldedit.command.tool.RecursivePickaxe;
import com.sk89q.worldedit.command.tool.SinglePickaxe;
import com.sk89q.worldedit.entity.Player;

/**
 * The super pickaxe modes, typed {@code //superpickaxe <mode>} (aliases {@code //pickaxe} and {@code //sp}).
 *
 * <ul>
 * <li>{@code single}: break one block</li>
 * <li>{@code area <radius>}: break everything in a cube</li>
 * <li>{@code recursive <radius>} (alias {@code recur}): break connected blocks of the same kind</li>
 * </ul>
 */
public final class SuperPickaxeCommands {

    private static final String ENABLED = "Mode changed. Left click with a pickaxe. // to disable.";

    private SuperPickaxeCommands() {}

    // spotless:off
    public static void register(CommandDispatcher<ICommandSender> dispatcher) {
        LiteralCommandNode<ICommandSender> recursive = recursiveNode("recursive");

        Nodes.register(dispatcher, literal("/superpickaxe", "Toggle the super pickaxe, or choose how it works")
                .executes(run("worldedit.superpickaxe", c -> ToolUtilCommands.togglePickaxe(c.player(), c.session(), null)))
                .then(word("state", "on", "off")
                        .executes(run("worldedit.superpickaxe",
                                c -> ToolUtilCommands.togglePickaxe(c.player(), c.session(), c.string("state")))))
                .then(literal("single", "Enable the single block super pickaxe mode")
                        .executes(run("worldedit.superpickaxe", SuperPickaxeCommands::single)))
                .then(literal("area", "Enable the area super pickaxe mode").executes(Nodes.USAGE)
                        .then(integer("radius")
                                .executes(run("worldedit.superpickaxe.area", SuperPickaxeCommands::area))))
                .then(recursive).then(Nodes.alias("recur", recursive)),
                "/pickaxe", "/sp", "/");
    }

    private static LiteralCommandNode<ICommandSender> recursiveNode(String name) {
        return literal(name, "Enable the recursive super pickaxe mode").executes(Nodes.USAGE)
                .then(decimal("radius")
                        .executes(run("worldedit.superpickaxe.recursive", SuperPickaxeCommands::recursive)))
                .build();
    }
    // spotless:on

    private static void single(CommandCall call) {
        Player player = call.player();
        call.session()
            .setSuperPickaxe(new SinglePickaxe());
        call.session()
            .enableSuperPickAxe();
        player.print(ENABLED);
    }

    private static void area(CommandCall call) {
        Player player = call.player();
        LocalConfiguration config = call.worldEdit()
            .getConfiguration();
        int range = call.integer("radius");
        if (range > config.maxSuperPickaxeSize) {
            player.printError("Maximum range: " + config.maxSuperPickaxeSize);
            return;
        }
        call.session()
            .setSuperPickaxe(new AreaPickaxe(range));
        call.session()
            .enableSuperPickAxe();
        player.print(ENABLED);
    }

    private static void recursive(CommandCall call) {
        Player player = call.player();
        LocalConfiguration config = call.worldEdit()
            .getConfiguration();
        double range = call.decimal("radius");
        if (range > config.maxSuperPickaxeSize) {
            player.printError("Maximum range: " + config.maxSuperPickaxeSize);
            return;
        }
        call.session()
            .setSuperPickaxe(new RecursivePickaxe(range));
        call.session()
            .enableSuperPickAxe();
        player.print(ENABLED);
    }
}
