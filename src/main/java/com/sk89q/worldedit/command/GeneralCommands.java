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
import static com.sk89q.worldedit.command.brigadier.Nodes.integer;
import static com.sk89q.worldedit.command.brigadier.Nodes.literal;
import static com.sk89q.worldedit.command.brigadier.Nodes.word;

import java.util.List;

import net.minecraft.command.ICommandSender;

import com.mojang.brigadier.CommandDispatcher;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.ItemType;
import com.sk89q.worldedit.command.brigadier.Nodes;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.function.mask.Mask;

/**
 * General WorldEdit commands.
 *
 * <ul>
 * <li>{@code //limit <limit>}: modify the block change limit</li>
 * <li>{@code //cancel}: cancel your running and queued edits</li>
 * <li>{@code //fast [on|off]}: toggle fast mode</li>
 * <li>{@code //gmask [mask]}: set or clear the global mask</li>
 * <li>{@code //toggleplace}: switch between your position and pos1 for placement</li>
 * <li>{@code //searchitem [-bi] <query>}: search for a block or item</li>
 * </ul>
 */
public final class GeneralCommands {

    private GeneralCommands() {}

    // spotless:off
    public static void register(CommandDispatcher<ICommandSender> dispatcher) {
        Nodes.register(dispatcher, literal("/limit").executes(Nodes.USAGE)
                .then(integer("limit")
                        .executes(run("worldedit.limit", c -> limit(c.worldEdit(), c.player(), c.session(), c.integer("limit"))))));

        Nodes.register(dispatcher, literal("/cancel")
                .executes(run("worldedit.cancel", c -> cancel(c.worldEdit(), c.player()))));

        Nodes.register(dispatcher, literal("/fast")
                .executes(run("worldedit.fast", c -> fast(c.player(), c.session(), null)))
                .then(word("state", "on", "off")
                        .executes(run("worldedit.fast", c -> fast(c.player(), c.session(), c.string("state"))))));

        Nodes.register(dispatcher, literal("/gmask")
                .executes(run("worldedit.global-mask", c -> gmask(c.player(), c.session(), null)))
                .then(word("mask")
                        .executes(run("worldedit.global-mask", c -> gmask(c.player(), c.session(), c.mask("mask"))))),
                "gmask");

        Nodes.register(dispatcher, literal("/toggleplace")
                .executes(run(null, c -> togglePlace(c.player(), c.session()))),
                "toggleplace");

        Nodes.registerWithFlags(dispatcher, literal("/searchitem").executes(Nodes.USAGE)
                .then(word("query")
                        .executes(run(null, c -> searchItem(c.actor(), c.string("query"), c.flag('b'), c.flag('i'))))),
                "bi", "/l", "/search", "searchitem");
    }
    // spotless:on

    private static void limit(WorldEdit worldEdit, Player player, LocalSession session, int requested) {
        LocalConfiguration config = worldEdit.getConfiguration();
        boolean mayDisable = player.hasPermission("worldedit.limit.unrestricted");

        int limit = Math.max(-1, requested);
        if (!mayDisable && config.maxChangeLimit > -1) {
            if (limit > config.maxChangeLimit) {
                player.printError("Your maximum allowable limit is " + config.maxChangeLimit + ".");
                return;
            }
        }

        session.setBlockChangeLimit(limit);

        if (limit != -1) {
            player.print("Block change limit set to " + limit + ". (Use //limit -1 to go back to the default.)");
        } else {
            player.print("Block change limit set to " + limit + ".");
        }
    }

    private static void cancel(WorldEdit worldEdit, Player player) {
        List<EditSession> cancelled = worldEdit.getOperationScheduler()
            .cancel(player.getUniqueId());

        if (cancelled.isEmpty()) {
            player.printError("You have no edits running.");
            return;
        }

        player.print("Cancelled " + cancelled.size() + " edit(s).");
        for (EditSession editSession : cancelled) {
            // A queued edit that never started records nothing, and //undo would hit an older edit.
            if (editSession.size() > 0) {
                player.print("Blocks already changed stay changed. Use //undo to revert them.");
                break;
            }
        }
    }

    private static void fast(Player player, LocalSession session, String newState) {
        if (session.hasFastMode()) {
            if ("on".equals(newState)) {
                player.printError("Fast mode already enabled.");
                return;
            }

            session.setFastMode(false);
            player.print("Fast mode disabled.");
        } else {
            if ("off".equals(newState)) {
                player.printError("Fast mode already disabled.");
                return;
            }

            session.setFastMode(true);
            player.print(
                "Fast mode enabled. Lighting in the affected chunks may be wrong and/or you may need to rejoin to see changes.");
        }
    }

    private static void gmask(Player player, LocalSession session, Mask mask) {
        if (mask == null) {
            session.setMask((Mask) null);
            player.print("Global mask disabled.");
        } else {
            session.setMask(mask);
            player.print("Global mask set.");
        }
    }

    private static void togglePlace(Player player, LocalSession session) {
        if (session.togglePlacementPosition()) {
            player.print("Now placing at pos #1.");
        } else {
            player.print("Now placing at the block you stand in.");
        }
    }

    private static void searchItem(Actor actor, String rawQuery, boolean blocksOnly, boolean itemsOnly) {
        String query = rawQuery.trim()
            .toLowerCase();

        try {
            int id = Integer.parseInt(query);

            ItemType type = ItemType.fromID(id);

            if (type != null) {
                actor.print("#" + type.getID() + " (" + type.getName() + ")");
            } else {
                actor.printError("No item found by ID " + id);
            }

            return;
        } catch (NumberFormatException ignored) {}

        if (query.length() <= 2) {
            actor.printError("Enter a longer search string (len > 2).");
            return;
        }

        if (!blocksOnly && !itemsOnly) {
            actor.print("Searching for: " + query);
        } else if (blocksOnly && itemsOnly) {
            actor.printError("You cannot use both the 'b' and 'i' flags simultaneously.");
            return;
        } else if (blocksOnly) {
            actor.print("Searching for blocks: " + query);
        } else {
            actor.print("Searching for items: " + query);
        }

        int found = 0;

        for (ItemType type : ItemType.values()) {
            if (found >= 15) {
                actor.print("Too many results!");
                break;
            }

            if (blocksOnly && type.getID() > 255) {
                continue;
            }

            if (itemsOnly && type.getID() <= 255) {
                continue;
            }

            for (String alias : type.getAliases()) {
                if (alias.contains(query)) {
                    actor.print("#" + type.getID() + " (" + type.getName() + ")");
                    ++found;
                    break;
                }
            }
        }

        if (found == 0) {
            actor.printError("No items found.");
        }
    }
}
