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
import static com.sk89q.worldedit.command.brigadier.CommandRunner.run;
import static com.sk89q.worldedit.command.brigadier.Nodes.integer;
import static com.sk89q.worldedit.command.brigadier.Nodes.literal;
import static com.sk89q.worldedit.command.brigadier.Nodes.word;

import javax.annotation.Nullable;

import net.minecraft.command.ICommandSender;

import com.mojang.brigadier.CommandDispatcher;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.brigadier.Nodes;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.util.operation.OperationCompletion;
import com.sk89q.worldedit.util.operation.OperationScheduler;

/**
 * Commands to undo, redo, and clear history.
 *
 * <ul>
 * <li>{@code //undo [times] [player]}</li>
 * <li>{@code //redo [times] [player]}</li>
 * <li>{@code //clearhistory}</li>
 * </ul>
 */
public class HistoryCommands {

    private final WorldEdit worldEdit;

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public HistoryCommands(WorldEdit worldEdit) {
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;
    }

    // spotless:off
    public void register(CommandDispatcher<ICommandSender> dispatcher) {
        Nodes.register(dispatcher, literal("/undo")
                .executes(run("worldedit.history.undo", c -> undo(c.player(), c.session(), 1, null)))
                .then(integer("times")
                        .executes(run("worldedit.history.undo", c -> undo(c.player(), c.session(), c.integer("times"), null)))
                        .then(word("player")
                                .executes(run("worldedit.history.undo", c ->
                                        undo(c.player(), c.session(), c.integer("times"), c.string("player")))))),
                "undo");

        Nodes.register(dispatcher, literal("/redo")
                .executes(run("worldedit.history.redo", c -> redo(c.player(), c.session(), 1, null)))
                .then(integer("times")
                        .executes(run("worldedit.history.redo", c -> redo(c.player(), c.session(), c.integer("times"), null)))
                        .then(word("player")
                                .executes(run("worldedit.history.redo", c ->
                                        redo(c.player(), c.session(), c.integer("times"), c.string("player")))))),
                "redo");

        Nodes.register(dispatcher, literal("/clearhistory")
                .executes(run("worldedit.history.clear", c -> clearHistory(c.player(), c.session()))),
                "clearhistory");
    }
    // spotless:on

    @Nullable
    private LocalSession findOther(Player player, String name, String action) throws WorldEditException {
        player.checkPermission("worldedit.history." + action + ".other");
        LocalSession other = worldEdit.getSessionManager()
            .findByName(name);
        if (other == null) {
            player.printError("Unable to find session for " + name);
        }
        return other;
    }

    /**
     * Hand a history step to the scheduler, which may spread it over several server ticks.
     */
    private void schedule(final Player player, final EditSession writer, Operation operation, final String message) {
        OperationScheduler scheduler = worldEdit.getOperationScheduler();
        scheduler.submit(player, writer, operation, new OperationCompletion() {

            @Override
            public void onSuccess() {
                player.print(message);
            }

            @Override
            public void onFailure(WorldEditException e) {
                String error = e.getMessage();
                player.printError(error != null ? error : "The operation could not be completed.");
            }
        });
        scheduler.whenFinished(writer, new Runnable() {

            @Override
            public void run() {
                worldEdit.flushBlockBag(player, writer);
            }
        });
    }

    /**
     * Undoing during an edit would revert the edit before it, because the running one isn't in the history until it
     * finishes.
     */
    private boolean editStillRunning(Player player) {
        if (worldEdit.getOperationScheduler()
            .hasPendingWork(player.getUniqueId())) {
            player.printError("You have an edit still running. Wait for it to finish, or use //cancel.");
            return true;
        }
        return false;
    }

    /**
     * @param other the name of another player whose history to undo, or null for your own
     */
    void undo(Player player, LocalSession session, int requestedTimes, @Nullable String other)
        throws WorldEditException {
        if (editStillRunning(player)) {
            return;
        }

        int times = Math.max(1, requestedTimes);
        for (int i = 0; i < times; ++i) {
            LocalSession target = other == null ? session : findOther(player, other, "undo");
            if (target == null) {
                break;
            }

            EditSession undone = target.stepBack();
            if (undone == null) {
                player.printError("Nothing left to undo.");
                break;
            }

            EditSession writer = target.createHistorySession(undone, session.getBlockBag(player), player);
            schedule(player, writer, undone.undoOperation(writer), "Undo successful.");
        }
    }

    /**
     * @param other the name of another player whose history to redo, or null for your own
     */
    void redo(Player player, LocalSession session, int requestedTimes, @Nullable String other)
        throws WorldEditException {
        if (editStillRunning(player)) {
            return;
        }

        int times = Math.max(1, requestedTimes);
        for (int i = 0; i < times; ++i) {
            LocalSession target = other == null ? session : findOther(player, other, "redo");
            if (target == null) {
                break;
            }

            EditSession redone = target.stepForward();
            if (redone == null) {
                player.printError("Nothing left to redo.");
                break;
            }

            EditSession writer = target.createHistorySession(redone, session.getBlockBag(player), player);
            schedule(player, writer, redone.redoOperation(writer), "Redo successful.");
        }
    }

    private void clearHistory(Player player, LocalSession session) {
        session.clearHistory();
        player.print("History cleared.");
    }
}
