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

package com.sk89q.worldedit.command.tool;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;

/**
 * Starts and finishes the edit sessions of tools that can change a lot of blocks, such as brushes, so
 * that their blocks are written the same way as a command's.
 */
final class ToolEdits {

    private ToolEdits() {}

    /**
     * Create an edit session that queues its blocks, so that a large edit can be written across ticks
     * and attached blocks go in after what they attach to.
     */
    static EditSession start(LocalSession session, Player player) {
        EditSession editSession = session.createEditSession(player);
        editSession.enableQueue();
        return editSession;
    }

    /**
     * Write the edit session's blocks, straight away if there are only a few, and record it for undo
     * once they are written.
     */
    static void finish(final LocalSession session, Player player, final EditSession editSession) {
        WorldEdit.getInstance()
            .getOperationScheduler()
            .whenFinished(player, editSession, new Runnable() {

                @Override
                public void run() {
                    session.remember(editSession);
                }
            });
    }
}
