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

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import net.minecraft.command.ICommandSender;

import com.mojang.brigadier.CommandDispatcher;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.brigadier.LogMode;
import com.sk89q.worldedit.command.brigadier.Nodes;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.operation.OperationCompletion;
import com.sk89q.worldedit.world.DataException;
import com.sk89q.worldedit.world.snapshot.InvalidSnapshotException;
import com.sk89q.worldedit.world.snapshot.Snapshot;
import com.sk89q.worldedit.world.snapshot.SnapshotRestore;
import com.sk89q.worldedit.world.storage.ChunkStore;
import com.sk89q.worldedit.world.storage.MissingWorldException;

public final class SnapshotUtilCommands {

    private SnapshotUtilCommands() {}

    // spotless:off
    public static void register(CommandDispatcher<ICommandSender> dispatcher) {
        Nodes.register(dispatcher, literal("restore", "Restore the selection from a snapshot")
                .executes(run("worldedit.snapshots.restore", LogMode.REGION,
                        c -> restore(c.worldEdit(), c.player(), c.session(), c.queuedEditSession(), null)))
                .then(word("snapshot")
                        .executes(run("worldedit.snapshots.restore", LogMode.REGION,
                                c -> restore(c.worldEdit(), c.player(), c.session(), c.queuedEditSession(), c.string("snapshot"))))),
                "/restore");
    }
    // spotless:on

    private static final Logger logger = Logger.getLogger("Minecraft.WorldEdit");

    private static void restore(WorldEdit worldEdit, final Player player, LocalSession session, EditSession editSession,
        String snapshotName) throws WorldEditException {

        LocalConfiguration config = WorldEdit.getInstance()
            .getConfiguration();

        if (config.snapshotRepo == null) {
            player.printError("Snapshot/backup restore is not configured.");
            return;
        }

        Region region = session.getSelection(player.getWorld());
        Snapshot snapshot;

        if (snapshotName != null) {
            try {
                snapshot = config.snapshotRepo.getSnapshot(snapshotName);
            } catch (InvalidSnapshotException e) {
                player.printError("That snapshot does not exist or is not available.");
                return;
            }
        } else {
            snapshot = session.getSnapshot();
        }

        // No snapshot set?
        if (snapshot == null) {
            try {
                snapshot = config.snapshotRepo.getDefaultSnapshot(
                    player.getWorld()
                        .getDirectoryName());

                if (snapshot == null) {
                    player.printError("No snapshots were found. See console for details.");

                    // Okay, let's toss some debugging information!
                    File dir = config.snapshotRepo.getDirectory();

                    try {
                        logger.info("WorldEdit found no snapshots: looked in: " + dir.getCanonicalPath());
                    } catch (IOException e) {
                        logger.info(
                            "WorldEdit found no snapshots: looked in " + "(NON-RESOLVABLE PATH - does it exist?): "
                                + dir.getPath());
                    }

                    return;
                }
            } catch (MissingWorldException ex) {
                player.printError("No snapshots were found for this world.");
                return;
            }
        }

        ChunkStore chunkStore = null;

        // Load chunk store
        try {
            chunkStore = snapshot.getChunkStore();
            player.print("Snapshot '" + snapshot.getName() + "' loaded; now restoring...");
        } catch (DataException e) {
            player.printError("Failed to load snapshot: " + e.getMessage());
            return;
        } catch (IOException e) {
            player.printError("Failed to load snapshot: " + e.getMessage());
            return;
        }

        // Each chunk is read from the snapshot in a step of its own, so this can take a while.
        final ChunkStore store = chunkStore;
        final SnapshotRestore restore = new SnapshotRestore(chunkStore, editSession, region);
        worldEdit.getOperationScheduler()
            .submit(player, editSession, restore, new OperationCompletion() {

                @Override
                public void onSuccess() {
                    close(store);
                    if (restore.hadTotalFailure()) {
                        String error = restore.getLastErrorMessage();
                        if (error != null) {
                            player.printError("Errors prevented any blocks from being restored.");
                            player.printError("Last error: " + error);
                        } else {
                            player.printError("No chunks could be loaded. (Bad archive?)");
                        }
                    } else {
                        player.print(
                            String.format(
                                "Restored; %d missing chunks and %d other errors.",
                                restore.getMissingChunks()
                                    .size(),
                                restore.getErrorChunks()
                                    .size()));
                    }
                }

                @Override
                public void onFailure(WorldEditException e) {
                    close(store);
                    String message = e.getMessage();
                    player.printError(message != null ? message : "The operation could not be completed.");
                }

                @Override
                public void onCancelled(boolean started) {
                    close(store);
                }
            });
    }

    private static void close(ChunkStore store) {
        try {
            store.close();
        } catch (IOException ignored) {}
    }
}
