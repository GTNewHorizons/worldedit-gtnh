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

import java.util.List;
import java.util.UUID;

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
import com.sk89q.worldedit.function.operation.RunContext;
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
    private void schedule(final Player player, final EditSession writer, Operation operation, final String message,
        final String command, final Runnable giveBackPlace, final boolean[] batchFailed,
        @Nullable final LocalSession held) {
        final SkippableStep step = new SkippableStep(operation, batchFailed);
        OperationScheduler scheduler = worldEdit.getOperationScheduler();
        scheduler.submit(player, writer, step, new OperationCompletion() {

            @Override
            public void onCancelled(boolean started) {
                // The history must match the world: a step that didn't finish doesn't count.
                giveBackPlace.run();
                if (started) {
                    player.printError(
                        "The " + command.substring(2)
                            + " was cancelled partway, so part of it already happened. Run "
                            + command
                            + " again to finish it.");
                }
            }

            @Override
            public void onSuccess() {
                if (step.skipped) {
                    giveBackPlace.run();
                } else {
                    player.print(message);
                }
            }

            @Override
            public void onFailure(WorldEditException e) {
                // Like a cancel: the step didn't finish, so it doesn't count, and the steps after it in
                // the same command would leave the history out of order if they went ahead.
                batchFailed[0] = true;
                giveBackPlace.run();
                String error = e.getMessage();
                player.printError(error != null ? error : "The operation could not be completed.");
                player.printError("Run " + command + " again to retry.");
            }
        });
        scheduler.whenFinished(player, writer, new Runnable() {

            @Override
            public void run() {
                if (held != null) {
                    held.releaseHistory();
                }
                worldEdit.flushBlockBag(player, writer);
            }
        });
    }

    /**
     * Whether the player can't touch their own history yet, because of their own queued edits or
     * another player's undo of it.
     */
    private boolean historyBusy(Player player, LocalSession session) {
        if (session.isHistoryHeld()) {
            player.printError(OperationScheduler.HISTORY_HELD);
            return true;
        }
        return editStillRunning(player);
    }

    /**
     * Find another player's history and check it is free: that player's own queued edits, or another
     * admin's queued undo, would change it underneath this one.
     *
     * @return the other player's session, or null if there is none or it is busy
     */
    @Nullable
    private LocalSession findFreeOther(Player player, LocalSession session, String name, String action)
        throws WorldEditException {
        LocalSession target = findOther(player, name, action);
        if (target == null || target == session) {
            return target;
        }

        UUID owner = worldEdit.getSessionManager()
            .findOwnerByName(name);
        if (target.isHistoryHeld() || (owner != null && worldEdit.getOperationScheduler()
            .hasPendingWork(owner))) {
            player.printError(name + " has edits still running. Try again once they finish.");
            return null;
        }
        return target;
    }

    /**
     * Undoing during an edit would revert the edit before it, because the running one isn't in the history until it
     * finishes.
     */
    private boolean editStillRunning(Player player) {
        if (worldEdit.getOperationScheduler()
            .hasPendingWork(player.getUniqueId())) {
            player.printError(OperationScheduler.STILL_RUNNING);
            return true;
        }
        return false;
    }

    /**
     * @param other the name of another player whose history to undo, or null for your own
     */
    void undo(Player player, LocalSession session, int requestedTimes, @Nullable String other)
        throws WorldEditException {
        if (historyBusy(player, session)) {
            return;
        }
        LocalSession target = other == null ? session : findFreeOther(player, session, other, "undo");
        if (target == null) {
            return;
        }

        int times = Math.max(1, requestedTimes);
        boolean[] batchFailed = { false };
        for (int i = 0; i < times; ++i) {
            EditSession undone = target.stepBack();
            if (undone == null) {
                player.printError("Nothing left to undo.");
                break;
            }
            LocalSession held = hold(target, session);

            final LocalSession history = target;
            EditSession writer = target.createHistorySession(undone, session.getBlockBag(player), player);
            schedule(player, writer, undone.undoOperation(writer), "Undo successful.", "//undo", new Runnable() {

                @Override
                public void run() {
                    history.cancelStepBack();
                }
            }, batchFailed, held);
        }
    }

    /**
     * @param other the name of another player whose history to redo, or null for your own
     */
    void redo(Player player, LocalSession session, int requestedTimes, @Nullable String other)
        throws WorldEditException {
        if (historyBusy(player, session)) {
            return;
        }
        LocalSession target = other == null ? session : findFreeOther(player, session, other, "redo");
        if (target == null) {
            return;
        }

        int times = Math.max(1, requestedTimes);
        boolean[] batchFailed = { false };
        for (int i = 0; i < times; ++i) {
            EditSession redone = target.stepForward();
            if (redone == null) {
                player.printError("Nothing left to redo.");
                break;
            }
            LocalSession held = hold(target, session);

            final LocalSession history = target;
            EditSession writer = target.createHistorySession(redone, session.getBlockBag(player), player);
            schedule(player, writer, redone.redoOperation(writer), "Redo successful.", "//redo", new Runnable() {

                @Override
                public void run() {
                    history.cancelStepForward();
                }
            }, batchFailed, held);
        }
    }

    /**
     * Reserve another player's history for one queued step. Your own needs no reservation, since your
     * own queue already keeps your edits in order.
     *
     * @return the reserved session, or null if none was needed
     */
    @Nullable
    private static LocalSession hold(LocalSession target, LocalSession session) {
        if (target == session) {
            return null;
        }
        target.holdHistory();
        return target;
    }

    private void clearHistory(Player player, LocalSession session) {
        if (historyBusy(player, session)) {
            return;
        }
        session.clearHistory();
        player.print("History cleared.");
    }

    /**
     * One step of an undo or redo that does nothing if an earlier step of the same command failed.
     */
    private static final class SkippableStep implements Operation {

        private final Operation original;
        private final boolean[] batchFailed;
        private Operation current;
        private boolean checked;
        private boolean skipped;

        private SkippableStep(Operation operation, boolean[] batchFailed) {
            this.original = operation;
            this.current = operation;
            this.batchFailed = batchFailed;
        }

        @Override
        public Operation resume(RunContext run) throws WorldEditException {
            if (!checked) {
                checked = true;
                if (batchFailed[0]) {
                    skipped = true;
                    current = null;
                }
            }
            if (current != null) {
                current = current.resume(run);
            }
            return current != null ? this : null;
        }

        @Override
        public void cancel() {
            if (current != null) {
                current.cancel();
            }
        }

        @Override
        public void addStatusMessages(List<String> messages) {
            original.addStatusMessages(messages);
        }
    }

}
