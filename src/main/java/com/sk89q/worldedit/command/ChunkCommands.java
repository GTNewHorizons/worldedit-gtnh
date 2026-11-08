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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.world.storage.McRegionChunkStore;

/**
 * Commands for working with chunks.
 */
public class ChunkCommands {

    private final WorldEdit worldEdit;

    public ChunkCommands(WorldEdit worldEdit) {
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;
    }

    @Command(
        aliases = { "chunkinfo" },
        usage = "",
        desc = "Get information about the chunk that you are inside",
        min = 0,
        max = 0)
    @CommandPermissions("worldedit.chunkinfo")
    public void chunkInfo(Player player, LocalSession session, EditSession editSession, CommandContext args)
        throws WorldEditException {
        Vector pos = player.getBlockIn();
        int chunkX = (int) Math.floor(pos.getBlockX() / 16.0);
        int chunkZ = (int) Math.floor(pos.getBlockZ() / 16.0);

        player.print("Chunk: " + chunkX + ", " + chunkZ);
        player.print("Region file: region/" + McRegionChunkStore.getFilename(new Vector2D(chunkX, chunkZ)));
    }

    @Command(
        aliases = { "listchunks" },
        usage = "",
        desc = "List chunks that your selection includes",
        min = 0,
        max = 0)
    @CommandPermissions("worldedit.listchunks")
    public void listChunks(Player player, LocalSession session, EditSession editSession, CommandContext args)
        throws WorldEditException {
        Set<Vector2D> chunks = session.getSelection(player.getWorld())
            .getChunks();

        for (Vector2D chunk : chunks) {
            player.print(
                chunk.getBlockX() + ", " + chunk.getBlockZ() + " in region/" + McRegionChunkStore.getFilename(chunk));
        }
    }

}
