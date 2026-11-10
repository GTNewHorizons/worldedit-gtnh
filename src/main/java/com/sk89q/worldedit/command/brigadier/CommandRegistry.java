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

import net.minecraft.command.ICommandSender;

import com.gtnewhorizon.gtnhlib.brigadier.BrigadierApi;
import com.mojang.brigadier.CommandDispatcher;
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
    public static void registerAll(WorldEdit worldEdit) {
        registerAll(BrigadierApi.getCommandDispatcher(), worldEdit);
    }

    static void registerAll(CommandDispatcher<ICommandSender> dispatcher, WorldEdit worldEdit) {

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
        UtilityCommands.register(dispatcher);
        WorldEditCommands.register(dispatcher);

    }
}
