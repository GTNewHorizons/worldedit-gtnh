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

import java.util.Set;

import net.minecraft.command.ICommandSender;

import com.mojang.brigadier.CommandDispatcher;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.command.brigadier.CommandCall;
import com.sk89q.worldedit.command.brigadier.Nodes;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.world.storage.McRegionChunkStore;

/**
 * Commands for working with chunks.
 *
 * <ul>
 * <li>{@code //chunkinfo}: get information about the chunk that you are inside</li>
 * <li>{@code //listchunks}: list the chunks that your selection includes</li>
 * </ul>
 */
public final class ChunkCommands {

    private ChunkCommands() {}

    // spotless:off
    public static void register(CommandDispatcher<ICommandSender> dispatcher) {
        Nodes.register(dispatcher, literal("/chunkinfo")
                .executes(run("worldedit.chunkinfo", ChunkCommands::chunkInfo)));

        Nodes.register(dispatcher, literal("/listchunks")
                .executes(run("worldedit.listchunks", ChunkCommands::listChunks)));
    }
    // spotless:on

    private static void chunkInfo(CommandCall call) {
        Player player = call.player();
        Vector pos = player.getBlockIn();
        int chunkX = (int) Math.floor(pos.getBlockX() / 16.0);
        int chunkZ = (int) Math.floor(pos.getBlockZ() / 16.0);

        player.print("Chunk: " + chunkX + ", " + chunkZ);
        player.print("Region file: region/" + McRegionChunkStore.getFilename(new Vector2D(chunkX, chunkZ)));
    }

    private static void listChunks(CommandCall call) throws Exception {
        Player player = call.player();
        Set<Vector2D> chunks = call.region()
            .getChunks();

        for (Vector2D chunk : chunks) {
            player.print(
                chunk.getBlockX() + ", " + chunk.getBlockZ() + " in region/" + McRegionChunkStore.getFilename(chunk));
        }
    }
}
