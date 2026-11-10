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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sk89q.worldedit.DisallowedItemException;
import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.InvalidItemException;
import com.sk89q.worldedit.MaxBrushRadiusException;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.MaxRadiusException;
import com.sk89q.worldedit.UnknownDirectionException;
import com.sk89q.worldedit.UnknownItemException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.ItemType;
import com.sk89q.worldedit.command.tool.InvalidToolBindException;
import com.sk89q.worldedit.util.io.file.FileSelectionAbortedException;
import com.sk89q.worldedit.util.io.file.FilenameResolutionException;
import com.sk89q.worldedit.util.io.file.InvalidFilenameException;

/**
 * Turns the exceptions WorldEdit throws into messages a player can understand.
 */
final class ErrorMessages {

    private static final Pattern NUMBER_FORMAT = Pattern.compile("^For input string: \"(.*)\"$");

    private ErrorMessages() {}

    /**
     * @return the message for the player, or null if this is not an error the player caused
     */
    static String describe(Exception e, WorldEdit worldEdit) {
        if (e instanceof NumberFormatException) {
            Matcher matcher = NUMBER_FORMAT.matcher(String.valueOf(e.getMessage()));
            return matcher.matches() ? "Number expected; string \"" + matcher.group(1) + "\" given."
                : "Number expected; string given.";
        } else if (e instanceof IncompleteRegionException) {
            return "Make a region selection first.";
        } else if (e instanceof UnknownItemException) {
            return "Block name '" + ((UnknownItemException) e).getID() + "' was not recognized.";
        } else if (e instanceof InvalidItemException) {
            return e.getMessage();
        } else if (e instanceof DisallowedItemException) {
            return "Block '" + ((DisallowedItemException) e).getID() + "' not allowed (see WorldEdit configuration).";
        } else if (e instanceof MaxChangedBlocksException) {
            return "Max blocks changed in an operation reached (" + ((MaxChangedBlocksException) e).getBlockLimit()
                + ").";
        } else if (e instanceof MaxBrushRadiusException) {
            return "Maximum brush radius (in configuration): " + worldEdit.getConfiguration().maxBrushRadius;
        } else if (e instanceof MaxRadiusException) {
            return "Maximum radius (in configuration): " + worldEdit.getConfiguration().maxRadius;
        } else if (e instanceof UnknownDirectionException) {
            return "Unknown direction: " + ((UnknownDirectionException) e).getDirection();
        } else if (e instanceof EmptyClipboardException) {
            return "Your clipboard is empty. Use //copy first.";
        } else if (e instanceof InvalidFilenameException) {
            return "Filename '" + ((InvalidFilenameException) e).getFilename() + "' invalid: " + e.getMessage();
        } else if (e instanceof FilenameResolutionException) {
            return "File '" + ((FilenameResolutionException) e).getFilename() + "' resolution error: " + e.getMessage();
        } else if (e instanceof InvalidToolBindException) {
            return "Can't bind tool to " + ItemType.toHeldName(((InvalidToolBindException) e).getItemId())
                + ": "
                + e.getMessage();
        } else if (e instanceof FileSelectionAbortedException) {
            return "File selection aborted.";
        } else if (e instanceof WorldEditException) {
            // InsufficientArgumentsException, RegionOperationException and ExpressionException all end up here
            return e.getMessage() != null ? e.getMessage() : null;
        } else if (e instanceof com.sk89q.worldedit.internal.expression.ExpressionException) {
            return e.getMessage();
        }
        return null;
    }
}
