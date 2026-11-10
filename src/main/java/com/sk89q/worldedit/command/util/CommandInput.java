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

package com.sk89q.worldedit.command.util;

import java.util.HashSet;
import java.util.Set;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;

/**
 * Parses blocks and patterns typed by a player as command arguments.
 */
public final class CommandInput {

    private CommandInput() {}

    private static ParserContext context(Player player) {
        ParserContext context = new ParserContext();
        context.setActor(player);
        context.setWorld(player.getWorld());
        context.setSession(
            WorldEdit.getInstance()
                .getSessionManager()
                .get(player));
        return context;
    }

    /**
     * Parse a block.
     *
     * @param player      the player
     * @param input       the input
     * @param allAllowed  true to skip the disallowed blocks check
     * @param allowNoData true to read a missing data value as a wildcard
     * @return the block
     * @throws WorldEditException if the input can't be parsed
     */
    public static BaseBlock block(Player player, String input, boolean allAllowed, boolean allowNoData)
        throws WorldEditException {
        ParserContext context = context(player);
        context.setRestricted(!allAllowed);
        context.setPreferringWildcard(allowNoData);
        return WorldEdit.getInstance()
            .getBlockFactory()
            .parseFromInput(input, context);
    }

    /**
     * Parse a comma-separated list of blocks.
     *
     * @param player      the player
     * @param list        the input
     * @param allAllowed  true to skip the disallowed blocks check
     * @param allowNoData true to read a missing data value as a wildcard
     * @return the blocks
     * @throws WorldEditException if the input can't be parsed
     */
    public static Set<BaseBlock> blocks(Player player, String list, boolean allAllowed, boolean allowNoData)
        throws WorldEditException {
        Set<BaseBlock> blocks = new HashSet<BaseBlock>();
        for (String id : list.split(",")) {
            blocks.add(block(player, id, allAllowed, allowNoData));
        }
        return blocks;
    }

    /**
     * Parse a comma-separated list of blocks and keep only their IDs.
     *
     * @param player     the player
     * @param list       the input
     * @param allAllowed true to skip the disallowed blocks check
     * @return the block IDs
     * @throws WorldEditException if the input can't be parsed
     */
    public static Set<Integer> blockIds(Player player, String list, boolean allAllowed) throws WorldEditException {
        Set<Integer> blocks = new HashSet<Integer>();
        for (String id : list.split(",")) {
            blocks.add(block(player, id, allAllowed, false).getType());
        }
        return blocks;
    }

    /**
     * Parse a pattern.
     *
     * @param player the player
     * @param input  the input
     * @return the pattern
     * @throws WorldEditException if the input can't be parsed
     */
    public static Pattern pattern(Player player, String input) throws WorldEditException {
        return WorldEdit.getInstance()
            .getPatternFactory()
            .parseFromInput(input, context(player));
    }
}
