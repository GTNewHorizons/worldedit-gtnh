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
import static com.sk89q.worldedit.command.brigadier.Nodes.decimal;
import static com.sk89q.worldedit.command.brigadier.Nodes.literal;
import static com.sk89q.worldedit.command.brigadier.Nodes.word;

import net.minecraft.command.ICommandSender;

import com.mojang.brigadier.CommandDispatcher;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.brigadier.CommandCall;
import com.sk89q.worldedit.command.brigadier.Flags;
import com.sk89q.worldedit.command.brigadier.LogMode;
import com.sk89q.worldedit.command.brigadier.Nodes;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.block.BlockReplace;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.operation.OperationCompletion;
import com.sk89q.worldedit.world.World;

/**
 * Clipboard commands.
 */
public final class ClipboardCommands {

    private ClipboardCommands() {}

    // spotless:off
    public static void register(CommandDispatcher<ICommandSender> dispatcher) {
        Nodes.registerWithFlags(dispatcher, literal("/copy", "Copy the selection to the clipboard")
                .executes(run("worldedit.clipboard.copy", c ->
                        copy(c.worldEdit(), c.player(), c.session(), c.queuedEditSession(), c.region(), c.flag('e'), c.maskFlag('m')))),
                Flags.of("e").value('m', "mask"));

        Nodes.registerWithFlags(dispatcher, literal("/cut", "Cut the selection to the clipboard")
                .executes(run("worldedit.clipboard.cut", LogMode.REGION, c ->
                        cut(c.worldEdit(), c.player(), c.session(), c.queuedEditSession(), c.region(), c.parsePattern("air"), c.flag('e'), c.maskFlag('m'))))
                .then(word("leave-pattern")
                        .executes(run("worldedit.clipboard.cut", LogMode.REGION, c ->
                                cut(c.worldEdit(), c.player(), c.session(), c.queuedEditSession(), c.region(), c.pattern("leave-pattern"), c.flag('e'), c.maskFlag('m'))))),
                Flags.of("e").value('m', "mask"));

        Nodes.registerWithFlags(dispatcher, literal("/paste", "Paste the clipboard's contents")
                .executes(run("worldedit.clipboard.paste", LogMode.PLACEMENT, c ->
                        paste(c.worldEdit(), c.player(), c.session(), c.queuedEditSession(), c.flag('a'), c.flag('o'), c.flag('s')))),
                "sao");

        Nodes.register(dispatcher, literal("/rotate", "Rotate the contents of the clipboard").executes(Nodes.USAGE)
                .then(decimal("y-axis")
                        .executes(run("worldedit.clipboard.rotate", ClipboardCommands::rotate))
                        .then(decimal("x-axis")
                                .executes(run("worldedit.clipboard.rotate", ClipboardCommands::rotate))
                                .then(decimal("z-axis")
                                        .executes(run("worldedit.clipboard.rotate", ClipboardCommands::rotate))))));

        Nodes.register(dispatcher, literal("/flip", "Flip the contents of the clipboard")
                .executes(run("worldedit.clipboard.flip", c -> flip(c.player(), c.session(), c.direction("direction", "me"))))
                .then(word("direction")
                        .executes(run("worldedit.clipboard.flip", c -> flip(c.player(), c.session(), c.direction("direction"))))));

        Nodes.register(dispatcher, literal("clearclipboard", "Clear your clipboard")
                .executes(run("worldedit.clipboard.clear", c -> clearClipboard(c.player(), c.session()))));
    }
    // spotless:on

    static void copy(WorldEdit worldEdit, Player player, LocalSession session, EditSession editSession, Region region,
        boolean copyEntities, Mask mask) throws WorldEditException {
        copyToClipboard(worldEdit, player, session, editSession, region, null, mask);
    }

    static void cut(WorldEdit worldEdit, Player player, LocalSession session, EditSession editSession, Region region,
        Pattern leavePattern, boolean copyEntities, Mask mask) throws WorldEditException {
        copyToClipboard(worldEdit, player, session, editSession, region, leavePattern, mask);
    }

    /**
     * Copy a region into a new clipboard on the scheduler, leaving the given pattern behind if there is one.
     *
     * <p>
     * The new clipboard becomes the player's clipboard straight away, empty, so that a //paste queued behind
     * this pastes it: the copy fills it in before the paste gets to run. If the copy doesn't finish, the
     * previous clipboard comes back.
     * </p>
     */
    private static void copyToClipboard(WorldEdit worldEdit, final Player player, final LocalSession session,
        EditSession editSession, Region selection, Pattern leavePattern, Mask mask) throws WorldEditException {
        final Region region = selection.clone();
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        clipboard.setOrigin(session.getPlacementPosition(player));
        ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, region.getMinimumPoint());
        if (leavePattern != null) {
            copy.setSourceFunction(new BlockReplace(editSession, leavePattern));
        }
        if (mask != null) {
            copy.setSourceMask(mask);
        }

        final ClipboardHolder previous = currentClipboard(session);
        final ClipboardHolder holder = new ClipboardHolder(
            clipboard,
            editSession.getWorld()
                .getWorldData());
        session.setClipboard(holder);

        worldEdit.getOperationScheduler()
            .submit(player, editSession, copy, new OperationCompletion() {

                @Override
                public void onSuccess() {
                    player.print(region.getArea() + " block(s) were copied.");
                }

                @Override
                public void onFailure(WorldEditException e) {
                    putBack();
                    String message = e.getMessage();
                    player.printError(message != null ? message : "The operation could not be completed.");
                }

                @Override
                public void onCancelled(boolean started) {
                    putBack();
                }

                private void putBack() {
                    if (currentClipboard(session) == holder) {
                        session.setClipboard(previous);
                    }
                }
            });
    }

    private static ClipboardHolder currentClipboard(LocalSession session) {
        try {
            return session.getClipboard();
        } catch (EmptyClipboardException e) {
            return null;
        }
    }

    public static void paste(WorldEdit worldEdit, Player player, LocalSession session, EditSession editSession,
        boolean ignoreAirBlocks, boolean atOrigin, boolean selectPasted) throws WorldEditException {

        ClipboardHolder holder = session.getClipboard();
        Clipboard clipboard = holder.getClipboard();
        Region region = clipboard.getRegion();

        Vector to = atOrigin ? clipboard.getOrigin() : session.getPlacementPosition(player);
        Operation operation = holder.createPaste(
            editSession,
            editSession.getWorld()
                .getWorldData())
            .to(to)
            .ignoreAirBlocks(ignoreAirBlocks)
            .build();
        final World world = player.getWorld();

        // The selection and the message have to wait until the paste has actually happened.
        worldEdit.getOperationScheduler()
            .submit(player, editSession, operation, new OperationCompletion() {

                @Override
                public void onSuccess() {
                    if (selectPasted) {
                        Vector max = to.add(
                            region.getMaximumPoint()
                                .subtract(region.getMinimumPoint()));
                        RegionSelector selector = new CuboidRegionSelector(world, to, max);
                        session.setRegionSelector(world, selector);
                        selector.learnChanges();
                        selector.explainRegionAdjust(player, session);
                    }

                    player.print("The clipboard has been pasted at " + to);
                }

                @Override
                public void onFailure(WorldEditException e) {
                    String message = e.getMessage();
                    player.printError(message != null ? message : "The operation could not be completed.");
                }
            });
    }

    private static void rotate(CommandCall call) throws WorldEditException {
        Player player = call.player();
        LocalSession session = call.session();
        double yRotate = call.decimal("y-axis");
        double xRotate = call.decimal("x-axis", 0);
        double zRotate = call.decimal("z-axis", 0);
        if (Math.abs(yRotate % 90) > 0.001 || Math.abs(xRotate % 90) > 0.001 || Math.abs(zRotate % 90) > 0.001) {
            player.printDebug(
                "Note: Interpolation is not yet supported, so angles that are multiples of 90 is recommended.");
        }

        ClipboardHolder holder = session.getClipboard();
        AffineTransform transform = new AffineTransform();
        transform = transform.rotateY(-yRotate);
        transform = transform.rotateX(-xRotate);
        transform = transform.rotateZ(-zRotate);
        holder.setTransform(
            holder.getTransform()
                .combine(transform));
        player.print("The clipboard copy has been rotated.");
    }

    private static void flip(Player player, LocalSession session, Vector direction) throws WorldEditException {
        ClipboardHolder holder = session.getClipboard();
        Clipboard clipboard = holder.getClipboard();
        AffineTransform transform = new AffineTransform();
        transform = transform.scale(
            direction.positive()
                .multiply(-2)
                .add(1, 1, 1));
        holder.setTransform(
            holder.getTransform()
                .combine(transform));
        player.print("The clipboard copy has been flipped.");
    }

    private static void clearClipboard(Player player, LocalSession session) throws WorldEditException {
        session.setClipboard(null);
        player.print("Clipboard cleared.");
    }
}
