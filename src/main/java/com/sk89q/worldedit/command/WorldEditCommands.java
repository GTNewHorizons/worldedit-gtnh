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
import static com.sk89q.worldedit.command.brigadier.Nodes.literal;
import static com.sk89q.worldedit.command.brigadier.Nodes.word;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import net.minecraft.command.ICommandSender;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.brigadier.CommandCall;
import com.sk89q.worldedit.command.brigadier.Nodes;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.platform.ConfigurationLoadEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extension.platform.PlatformManager;
import com.sk89q.worldedit.forge.command.ForgeRotationCommands;

/**
 * The commands about WorldEdit itself, typed {@code //worldedit <command>} (alias {@code //we}).
 *
 * <ul>
 * <li>{@code version} (alias {@code ver}): show the version</li>
 * <li>{@code reload}, {@code reloadmappings}: reload the configuration, or the rotation mappings</li>
 * <li>{@code cui}: complete the CUI handshake (used by the client mod)</li>
 * <li>{@code tz <timezone>}: set your timezone for snapshots</li>
 * <li>{@code help [command]}</li>
 * </ul>
 */
public final class WorldEditCommands {

    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

    private WorldEditCommands() {}

    // spotless:off
    public static void register(CommandDispatcher<ICommandSender> dispatcher) {
        LiteralCommandNode<ICommandSender> version = literal("version", "Get WorldEdit version")
                .executes(run(null, WorldEditCommands::version)).build();

        Nodes.register(dispatcher, literal("worldedit", "WorldEdit commands").executes(Nodes.USAGE)
                .then(version).then(Nodes.alias("ver", version))
                .then(literal("reload", "Reload configuration")
                        .executes(run("worldedit.reload", WorldEditCommands::reload)))
                .then(ForgeRotationCommands.node())
                .then(literal("cui", "Complete CUI handshake (internal usage)")
                        .executes(run(null, c -> cui(c.player(), c.session()))))
                .then(literal("tz", "Set your timezone for snapshots").executes(Nodes.USAGE)
                        .then(word("timezone")
                                .executes(run(null, c -> tz(c.player(), c.session(), c.string("timezone"))))))
                .then(HelpCommand.node("help")),
                "we");
    }
    // spotless:on

    private static void version(CommandCall call) {
        Actor actor = call.actor();
        actor.print("WorldEdit version " + WorldEdit.getVersion());
        actor.print("https://github.com/sk89q/worldedit/");

        PlatformManager pm = call.worldEdit()
            .getPlatformManager();

        actor.printDebug("----------- Platforms -----------");
        for (Platform platform : pm.getPlatforms()) {
            actor.printDebug(String.format("* %s (%s)", platform.getPlatformName(), platform.getPlatformVersion()));
        }

        actor.printDebug("----------- Capabilities -----------");
        for (Capability capability : Capability.values()) {
            Platform platform = pm.queryCapability(capability);
            actor.printDebug(
                String.format("%s: %s", capability.name(), platform != null ? platform.getPlatformName() : "NONE"));
        }
    }

    private static void reload(CommandCall call) {
        WorldEdit we = call.worldEdit();
        we.getPlatformManager()
            .queryCapability(Capability.USER_COMMANDS)
            .reload();
        we.getEventBus()
            .post(
                new ConfigurationLoadEvent(
                    we.getPlatformManager()
                        .queryCapability(Capability.CONFIGURATION)
                        .getConfiguration()));
        call.actor()
            .print("Configuration reloaded!");
    }

    private static void cui(Player player, LocalSession session) {
        session.setCUISupport(true);
        session.dispatchCUISetup(player);
    }

    private static void tz(Player player, LocalSession session, String timezone) {
        TimeZone tz = TimeZone.getTimeZone(timezone);
        session.setTimezone(tz);
        player.print("Timezone set for this session to: " + tz.getDisplayName());
        player.print(
            "The current time in that timezone is: " + dateFormat.format(
                Calendar.getInstance(tz)
                    .getTime()));
    }
}
