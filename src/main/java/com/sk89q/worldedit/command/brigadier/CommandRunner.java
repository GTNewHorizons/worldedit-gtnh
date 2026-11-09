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

import java.util.logging.Level;
import java.util.logging.Logger;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.CommandManager;
import com.sk89q.worldedit.forge.ForgeWorldEdit;
import com.sk89q.worldedit.session.request.Request;
import com.sk89q.worldedit.util.operation.OperationScheduler;

/**
 * Turns a {@link Handler} into a Brigadier {@link Command}.
 *
 * <p>
 * Around every command it checks the permission, writes the command log, reports errors to the player, and when the
 * command made an edit it records it for undo once the work is finished.
 * </p>
 */
public final class CommandRunner {

    private static final Logger log = Logger.getLogger(CommandRunner.class.getCanonicalName());

    private CommandRunner() {}

    /**
     * The body of a command.
     */
    @FunctionalInterface
    public interface Handler {

        void run(CommandCall call) throws Exception;
    }

    /**
     * A command that is logged with just the typed command.
     *
     * @param permission the permission the player needs, or null for none. Several permissions can be given separated
     *                   by {@code |}, and then the player needs at least one of them.
     * @param handler    what the command does
     */
    public static Command<ICommandSender> run(String permission, Handler handler) {
        return run(permission, LogMode.NONE, handler);
    }

    /**
     * @param permission the permission the player needs, or null for none. Several permissions can be given separated
     *                   by {@code |}, and then the player needs at least one of them.
     * @param logMode    what to log besides the typed command
     * @param handler    what the command does
     */
    public static Command<ICommandSender> run(String permission, LogMode logMode, Handler handler) {
        return ctx -> execute(ctx, permission, logMode, handler);
    }

    private static int execute(CommandContext<ICommandSender> ctx, String permission, LogMode logMode,
        Handler handler) {
        Request.reset();

        ICommandSender sender = FlaggedSender.unwrap(ctx.getSource());
        if (!(sender instanceof EntityPlayerMP)) {
            sender.addChatMessage(error("WorldEdit commands can only be used by players."));
            return 0;
        }

        WorldEdit worldEdit = WorldEdit.getInstance();
        Actor actor = worldEdit.getPlatformManager()
            .createProxyActor(ForgeWorldEdit.inst.wrap((EntityPlayerMP) sender));
        LocalSession session = worldEdit.getSessionManager()
            .get(actor);

        CommandCall call = new CommandCall(worldEdit, ctx, actor);
        long start = System.currentTimeMillis();
        boolean succeeded = false;

        try {
            if (permission != null && !hasAnyPermission(actor, permission)) {
                actor.printError("You are not permitted to do that. Are you in the right mode?");
                return 0;
            }
            logCommand(call, logMode);
            handler.run(call);
            succeeded = true;
        } catch (CommandFailure e) {
            actor.printError(e.getMessage());
        } catch (Exception e) {
            reportUnexpected(actor, e);
        } finally {
            EditSession editSession = call.editSessionIfCreated();
            if (editSession != null) {
                // The scheduler flushes and then runs this, either now or once the work it has
                // queued for this session finishes.
                worldEdit.getOperationScheduler()
                    .whenFinished(
                        actor,
                        editSession,
                        new SessionFinalizer(
                            worldEdit,
                            actor,
                            session,
                            editSession,
                            worldEdit.getConfiguration(),
                            start));
            }
        }
        return succeeded ? Command.SINGLE_SUCCESS : 0;
    }

    /**
     * @param permissions one permission, or several separated by | of which the player needs at least one
     */
    private static boolean hasAnyPermission(Actor actor, String permissions) {
        for (String permission : permissions.split("\\|")) {
            if (actor.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    private static void reportUnexpected(Actor actor, Exception e) {
        String message = ErrorMessages.describe(e, WorldEdit.getInstance());
        if (message != null) {
            actor.printError(message);
        } else {
            actor.printError("Please report this error: [See console]");
            actor.printRaw(
                e.getClass()
                    .getName() + ": "
                    + e.getMessage());
            log.log(Level.SEVERE, "An unexpected error while handling a WorldEdit command", e);
        }
    }

    private static ChatComponentText error(String message) {
        ChatComponentText text = new ChatComponentText(message);
        text.getChatStyle()
            .setColor(EnumChatFormatting.RED);
        return text;
    }

    private static void logCommand(CommandCall call, LogMode logMode) {
        Logger commandLog = CommandManager.getLogger();
        if (!commandLog.isLoggable(Level.INFO) || !(call.actor() instanceof Player)) {
            return;
        }
        Player player = (Player) call.actor();
        StringBuilder builder = new StringBuilder();
        builder.append("WorldEdit: ")
            .append(player.getName())
            .append(" (in \"")
            .append(
                player.getWorld()
                    .getName())
            .append("\"): /")
            .append(call.input());

        LocalSession session = call.session();
        Vector position = player.getPosition();
        switch (logMode) {
            case NONE:
                break;
            case PLACEMENT:
                try {
                    position = session.getPlacementPosition(player);
                } catch (IncompleteRegionException e) {
                    break;
                }
                builder.append(" - Position: ")
                    .append(position);
                break;
            case POSITION:
                builder.append(" - Position: ")
                    .append(position);
                break;
            case ALL:
                builder.append(" - Position: ")
                    .append(position);
                appendOrientation(builder, player);
                appendRegion(builder, session, player);
                break;
            case ORIENTATION_REGION:
                appendOrientation(builder, player);
                appendRegion(builder, session, player);
                break;
            case REGION:
                appendRegion(builder, session, player);
                break;
        }
        commandLog.info(builder.toString());
    }

    private static void appendOrientation(StringBuilder builder, Player player) {
        builder.append(" - Orientation: ")
            .append(
                player.getCardinalDirection()
                    .name());
    }

    private static void appendRegion(StringBuilder builder, LocalSession session, Player player) {
        try {
            builder.append(" - Region: ")
                .append(session.getSelection(player.getWorld()));
        } catch (IncompleteRegionException ignored) {
            // no selection to log
        }
    }

    /**
     * Wraps up an edit session once its work is done: records it for undo, reports timings and returns anything left
     * in the block bag.
     *
     * <p>
     * This does not flush the session. The {@link OperationScheduler} is responsible for that, because a flush has to
     * wait for any work it has queued.
     * </p>
     */
    private static final class SessionFinalizer implements Runnable {

        private final WorldEdit worldEdit;
        private final Actor actor;
        private final LocalSession session;
        private final EditSession editSession;
        private final LocalConfiguration config;
        private final long start;

        private SessionFinalizer(WorldEdit worldEdit, Actor actor, LocalSession session, EditSession editSession,
            LocalConfiguration config, long start) {
            this.worldEdit = worldEdit;
            this.actor = actor;
            this.session = session;
            this.editSession = editSession;
            this.config = config;
            this.start = start;
        }

        @Override
        public void run() {
            session.remember(editSession);

            if (config.profile) {
                long time = System.currentTimeMillis() - start;
                int changed = editSession.getBlockChangeCount();
                if (time > 0) {
                    double throughput = changed / (time / 1000.0);
                    actor.printDebug(
                        (time / 1000.0) + "s elapsed (history: "
                            + changed
                            + " changed; "
                            + Math.round(throughput)
                            + " blocks/sec).");
                } else {
                    actor.printDebug((time / 1000.0) + "s elapsed.");
                }
            }

            worldEdit.flushBlockBag(actor, editSession);
        }
    }
}
