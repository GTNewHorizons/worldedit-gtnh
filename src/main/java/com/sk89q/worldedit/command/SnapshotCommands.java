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

// $Id$

package com.sk89q.worldedit.command;

import static com.sk89q.worldedit.command.brigadier.CommandRunner.run;
import static com.sk89q.worldedit.command.brigadier.Nodes.integer;
import static com.sk89q.worldedit.command.brigadier.Nodes.literal;
import static com.sk89q.worldedit.command.brigadier.Nodes.text;
import static com.sk89q.worldedit.command.brigadier.Nodes.word;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;

import net.minecraft.command.ICommandSender;

import com.mojang.brigadier.CommandDispatcher;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.brigadier.Nodes;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.world.snapshot.InvalidSnapshotException;
import com.sk89q.worldedit.world.snapshot.Snapshot;
import com.sk89q.worldedit.world.storage.MissingWorldException;

/**
 * Snapshot commands.
 */
public final class SnapshotCommands {

    private SnapshotCommands() {}

    // spotless:off
    public static void register(CommandDispatcher<ICommandSender> dispatcher) {
        Nodes.register(dispatcher, literal("/snapshot", "Commands for choosing the snapshot to restore from").executes(Nodes.USAGE)
                .then(Nodes.sub("snapshot", "list", "List snapshots")
                        .executes(run("worldedit.snapshots.list", c -> list(c.player(), 5)))
                        .then(integer("num")
                                .executes(run("worldedit.snapshots.list", c -> list(c.player(), c.integer("num"))))))
                .then(literal("use", "Choose a snapshot to use").executes(Nodes.USAGE)
                        .then(word("snapshot")
                                .executes(run("worldedit.snapshots.restore", c -> use(c.player(), c.session(), c.string("snapshot"))))))
                .then(literal("sel", "Choose the snapshot based on the list id").executes(Nodes.USAGE)
                        .then(integer("index")
                                .executes(run("worldedit.snapshots.restore", c -> sel(c.player(), c.session(), c.integer("index"))))))
                .then(literal("before", "Choose the nearest snapshot before a date").executes(Nodes.USAGE)
                        .then(text("date")
                                .executes(run("worldedit.snapshots.restore", c -> before(c.player(), c.session(), c.string("date"))))))
                .then(literal("after", "Choose the nearest snapshot after a date").executes(Nodes.USAGE)
                        .then(text("date")
                                .executes(run("worldedit.snapshots.restore", c -> after(c.player(), c.session(), c.string("date")))))),
                "/snap");
    }
    // spotless:on

    private static final Logger logger = Logger.getLogger("Minecraft.WorldEdit");
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

    private static void list(Player player, int requested) throws WorldEditException {

        LocalConfiguration config = WorldEdit.getInstance()
            .getConfiguration();

        if (config.snapshotRepo == null) {
            player.printError("Snapshot/backup restore is not configured.");
            return;
        }

        try {
            List<Snapshot> snapshots = config.snapshotRepo.getSnapshots(
                true,
                player.getWorld()
                    .getDirectoryName());

            if (!snapshots.isEmpty()) {

                int num = Math.min(40, Math.max(5, requested));

                player.print(
                    "Snapshots for world: '" + player.getWorld()
                        .getDirectoryName() + "'");
                for (byte i = 0; i < Math.min(num, snapshots.size()); i++) {
                    player.print(
                        (i + 1) + ". "
                            + snapshots.get(i)
                                .getName());
                }

                player.print("Use //snapshot use [snapshot] or //snapshot use latest.");
            } else {
                player.printError("No snapshots are available. See console for details.");

                // Okay, let's toss some debugging information!
                File dir = config.snapshotRepo.getDirectory();

                try {
                    logger.info("WorldEdit found no snapshots: looked in: " + dir.getCanonicalPath());
                } catch (IOException e) {
                    logger.info(
                        "WorldEdit found no snapshots: looked in " + "(NON-RESOLVABLE PATH - does it exist?): "
                            + dir.getPath());
                }
            }
        } catch (MissingWorldException ex) {
            player.printError("No snapshots were found for this world.");
        }
    }

    private static void use(Player player, LocalSession session, String name) throws WorldEditException {

        LocalConfiguration config = WorldEdit.getInstance()
            .getConfiguration();

        if (config.snapshotRepo == null) {
            player.printError("Snapshot/backup restore is not configured.");
            return;
        }

        // Want the latest snapshot?
        if (name.equalsIgnoreCase("latest")) {
            try {
                Snapshot snapshot = config.snapshotRepo.getDefaultSnapshot(
                    player.getWorld()
                        .getDirectoryName());

                if (snapshot != null) {
                    session.setSnapshot(null);
                    player.print("Now using newest snapshot.");
                } else {
                    player.printError("No snapshots were found.");
                }
            } catch (MissingWorldException ex) {
                player.printError("No snapshots were found for this world.");
            }
        } else {
            try {
                session.setSnapshot(config.snapshotRepo.getSnapshot(name));
                player.print("Snapshot set to: " + name);
            } catch (InvalidSnapshotException e) {
                player.printError("That snapshot does not exist or is not available.");
            }
        }
    }

    private static void sel(Player player, LocalSession session, int index) throws WorldEditException {
        LocalConfiguration config = WorldEdit.getInstance()
            .getConfiguration();

        if (config.snapshotRepo == null) {
            player.printError("Snapshot/backup restore is not configured.");
            return;
        }

        if (index < 1) {
            player.printError("Invalid index, must be equal or higher then 1.");
            return;
        }

        try {
            List<Snapshot> snapshots = config.snapshotRepo.getSnapshots(
                true,
                player.getWorld()
                    .getDirectoryName());
            if (snapshots.size() < index) {
                player.printError("Invalid index, must be between 1 and " + snapshots.size() + ".");
                return;
            }
            Snapshot snapshot = snapshots.get(index - 1);
            if (snapshot == null) {
                player.printError("That snapshot does not exist or is not available.");
                return;
            }
            session.setSnapshot(snapshot);
            player.print("Snapshot set to: " + snapshot.getName());
        } catch (MissingWorldException e) {
            player.printError("No snapshots were found for this world.");
        }
    }

    private static void before(Player player, LocalSession session, String dateText) throws WorldEditException {

        LocalConfiguration config = WorldEdit.getInstance()
            .getConfiguration();

        if (config.snapshotRepo == null) {
            player.printError("Snapshot/backup restore is not configured.");
            return;
        }

        Calendar date = session.detectDate(dateText);

        if (date == null) {
            player.printError("Could not detect the date inputted.");
        } else {
            try {
                Snapshot snapshot = config.snapshotRepo.getSnapshotBefore(
                    date,
                    player.getWorld()
                        .getDirectoryName());

                if (snapshot == null) {
                    dateFormat.setTimeZone(session.getTimeZone());
                    player.printError("Couldn't find a snapshot before " + dateFormat.format(date.getTime()) + ".");
                } else {
                    session.setSnapshot(snapshot);
                    player.print("Snapshot set to: " + snapshot.getName());
                }
            } catch (MissingWorldException ex) {
                player.printError("No snapshots were found for this world.");
            }
        }
    }

    private static void after(Player player, LocalSession session, String dateText) throws WorldEditException {

        LocalConfiguration config = WorldEdit.getInstance()
            .getConfiguration();

        if (config.snapshotRepo == null) {
            player.printError("Snapshot/backup restore is not configured.");
            return;
        }

        Calendar date = session.detectDate(dateText);

        if (date == null) {
            player.printError("Could not detect the date inputted.");
        } else {
            try {
                Snapshot snapshot = config.snapshotRepo.getSnapshotAfter(
                    date,
                    player.getWorld()
                        .getDirectoryName());
                if (snapshot == null) {
                    dateFormat.setTimeZone(session.getTimeZone());
                    player.printError("Couldn't find a snapshot after " + dateFormat.format(date.getTime()) + ".");
                } else {
                    session.setSnapshot(snapshot);
                    player.print("Snapshot set to: " + snapshot.getName());
                }
            } catch (MissingWorldException ex) {
                player.printError("No snapshots were found for this world.");
            }
        }
    }

}
