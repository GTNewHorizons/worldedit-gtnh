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

package com.sk89q.worldedit.command.brigadier;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.ICommandSender;

import com.gtnewhorizon.gtnhlib.brigadier.BrigadierApi;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.BiomeCommands;
import com.sk89q.worldedit.command.BrushCommands;
import com.sk89q.worldedit.command.ChunkCommands;
import com.sk89q.worldedit.command.ClipboardCommands;
import com.sk89q.worldedit.command.GeneralCommands;
import com.sk89q.worldedit.command.GenerationCommands;
import com.sk89q.worldedit.command.HelpCommand;
import com.sk89q.worldedit.command.HistoryCommands;
import com.sk89q.worldedit.command.NavigationCommands;
import com.sk89q.worldedit.command.RegionCommands;
import com.sk89q.worldedit.command.SchematicCommands;
import com.sk89q.worldedit.command.SelectionCommands;
import com.sk89q.worldedit.command.SnapshotCommands;
import com.sk89q.worldedit.command.SnapshotUtilCommands;
import com.sk89q.worldedit.command.SuperPickaxeCommands;
import com.sk89q.worldedit.command.ToolCommands;
import com.sk89q.worldedit.command.ToolUtilCommands;
import com.sk89q.worldedit.command.UtilityCommands;
import com.sk89q.worldedit.command.WorldEditCommands;

/**
 * Registers every WorldEdit command with the Brigadier dispatcher of GTNHLib.
 *
 * <p>
 * Each command group owns its commands: it declares the whole tree in a {@code register} method, so to find out how a
 * command is typed, look at the group it belongs to.
 * </p>
 */
public final class CommandRegistry {

    private CommandRegistry() {}

    /**
     * Must run after GTNHLib has created the dispatcher for the starting server.
     */
    public static void registerAll(WorldEdit worldEdit, boolean noDoubleSlash) {
        registerAll(BrigadierApi.getCommandDispatcher(), worldEdit, noDoubleSlash);
    }

    static void registerAll(CommandDispatcher<ICommandSender> dispatcher, WorldEdit worldEdit, boolean noDoubleSlash) {

        BiomeCommands.register(dispatcher);
        BrushCommands.register(dispatcher);
        ChunkCommands.register(dispatcher);
        ClipboardCommands.register(dispatcher);
        GeneralCommands.register(dispatcher);
        GenerationCommands.register(dispatcher);
        HelpCommand.register(dispatcher);
        new HistoryCommands(worldEdit).register(dispatcher);
        NavigationCommands.register(dispatcher);
        RegionCommands.register(dispatcher);
        SchematicCommands.register(dispatcher);
        SelectionCommands.register(dispatcher);
        SnapshotCommands.register(dispatcher);
        SnapshotUtilCommands.register(dispatcher);
        SuperPickaxeCommands.register(dispatcher);
        ToolCommands.register(dispatcher);
        ToolUtilCommands.register(dispatcher);
        UtilityCommands.register(dispatcher);
        WorldEditCommands.register(dispatcher);

        addSlashAliases(dispatcher, noDoubleSlash);
    }

    /**
     * Commands such as {@code //undo} are typed with two slashes, and the first one is eaten by Minecraft. So a command
     * defined without a slash can also be typed {@code //name}, and with the {@code no-double-slash} setting a command
     * defined with a slash can also be typed {@code /name}.
     */
    private static void addSlashAliases(CommandDispatcher<ICommandSender> dispatcher, boolean noDoubleSlash) {
        List<CommandNode<ICommandSender>> existing = new ArrayList<>(
            dispatcher.getRoot()
                .getChildren());
        for (CommandNode<ICommandSender> node : existing) {
            String name = node.getName();
            boolean slashed = name.startsWith("/");
            if (slashed && !noDoubleSlash) {
                continue;
            }
            String alias = slashed ? name.substring(1) : "/" + name;
            if (dispatcher.getRoot()
                .getChild(alias) != null) {
                continue;
            }
            CommandNode<ICommandSender> target = node.getRedirect() != null ? node.getRedirect() : node;
            dispatcher.getRoot()
                .addChild(
                    LiteralArgumentBuilder.<ICommandSender>literal(alias)
                        .requires(target.getRequirement())
                        .executes(target.getCommand())
                        .redirect(target)
                        .build());
        }
    }
}
