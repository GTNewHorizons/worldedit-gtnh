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

import net.minecraft.command.ICommandSender;

import com.mojang.brigadier.CommandDispatcher;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.WorldVector;
import com.sk89q.worldedit.command.brigadier.CommandCall;
import com.sk89q.worldedit.command.brigadier.LogMode;
import com.sk89q.worldedit.command.brigadier.Nodes;
import com.sk89q.worldedit.entity.Player;

/**
 * Commands for moving the player around.
 *
 * <ul>
 * <li>{@code unstuck} (alias {@code !}): escape from being stuck inside a block</li>
 * <li>{@code ascend [levels]} (alias {@code asc}), {@code descend [levels]} (alias {@code desc})</li>
 * <li>{@code ceil [-fg] [clearance]}, {@code up [-fg] <distance>}</li>
 * <li>{@code thru}: pass through a wall</li>
 * <li>{@code jumpto} (alias {@code j}): teleport to the block you look at</li>
 * </ul>
 */
public final class NavigationCommands {

    private NavigationCommands() {}

    // spotless:off
    public static void register(CommandDispatcher<ICommandSender> dispatcher) {
        Nodes.register(dispatcher, literal("unstuck")
                .executes(run("worldedit.navigation.unstuck", NavigationCommands::unstuck)),
                "!");

        Nodes.register(dispatcher, literal("ascend")
                .executes(run("worldedit.navigation.ascend", c -> ascend(c.player(), 1)))
                .then(integer("levels")
                        .executes(run("worldedit.navigation.ascend", c -> ascend(c.player(), c.integer("levels"))))),
                "asc");

        Nodes.register(dispatcher, literal("descend")
                .executes(run("worldedit.navigation.descend", c -> descend(c.player(), 1)))
                .then(integer("levels")
                        .executes(run("worldedit.navigation.descend", c -> descend(c.player(), c.integer("levels"))))),
                "desc");

        Nodes.registerWithFlags(dispatcher, literal("ceil")
                .executes(run("worldedit.navigation.ceiling", LogMode.POSITION, c -> ceiling(c, 0)))
                .then(integer("clearance", 0)
                        .executes(run("worldedit.navigation.ceiling", LogMode.POSITION,
                                c -> ceiling(c, c.integer("clearance"))))),
                "fg");

        Nodes.register(dispatcher, literal("thru")
                .executes(run("worldedit.navigation.thru.command", NavigationCommands::thru)));

        Nodes.register(dispatcher, literal("jumpto")
                .executes(run("worldedit.navigation.jumpto.command", NavigationCommands::jumpTo)),
                "j");

        Nodes.registerWithFlags(dispatcher, literal("up").executes(Nodes.USAGE)
                .then(integer("distance")
                        .executes(run("worldedit.navigation.up", LogMode.POSITION, NavigationCommands::up))),
                "fg");
    }
    // spotless:on

    private static void unstuck(CommandCall call) {
        Player player = call.player();
        player.print("There you go!");
        player.findFreePosition();
    }

    private static void ascend(Player player, int levelsToAscend) {
        int ascentLevels = 1;
        while (player.ascendLevel() && levelsToAscend != ascentLevels) {
            ++ascentLevels;
        }
        if (ascentLevels == 0) {
            player.printError("No free spot above you found.");
        } else {
            player.print((ascentLevels != 1) ? "Ascended " + ascentLevels + " levels." : "Ascended a level.");
        }
    }

    private static void descend(Player player, int levelsToDescend) {
        int descentLevels = 1;
        while (player.descendLevel() && levelsToDescend != descentLevels) {
            ++descentLevels;
        }
        if (descentLevels == 0) {
            player.printError("No free spot above you found.");
        } else {
            player.print((descentLevels != 1) ? "Descended " + descentLevels + " levels." : "Descended a level.");
        }
    }

    private static void ceiling(CommandCall call, int clearance) {
        Player player = call.player();
        if (player.ascendToCeiling(clearance, alwaysGlass(call))) {
            player.print("Whoosh!");
        } else {
            player.printError("No free spot above you found.");
        }
    }

    private static void thru(CommandCall call) {
        Player player = call.player();
        if (player.passThroughForwardWall(6)) {
            player.print("Whoosh!");
        } else {
            player.printError("No free spot ahead of you found.");
        }
    }

    private static void jumpTo(CommandCall call) {
        Player player = call.player();
        WorldVector pos = player.getSolidBlockTrace(300);
        if (pos != null) {
            player.findFreePosition(pos);
            player.print("Poof!");
        } else {
            player.printError("No block in sight!");
        }
    }

    private static void up(CommandCall call) {
        Player player = call.player();
        if (player.ascendUpwards(call.integer("distance"), alwaysGlass(call))) {
            player.print("Whoosh!");
        } else {
            player.printError("You would hit something above you.");
        }
    }

    /**
     * Flags of {@code up} and {@code ceil}: -g always puts glass under the player, -f never does.
     *
     * @return true if glass should always be put under the player
     */
    private static boolean alwaysGlass(CommandCall call) {
        LocalConfiguration config = call.worldEdit()
            .getConfiguration();
        return call.flag('g') || (config.navigationUseGlass && !call.flag('f'));
    }
}
