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

package com.sk89q.worldedit.util.operation;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;

/**
 * Runs operations to completion on the calling thread, which is how WorldEdit behaved before
 * {@link OperationScheduler} existed. Used when the platform has not installed a scheduler of its
 * own.
 */
public class DirectOperationScheduler implements OperationScheduler {

    @Override
    public boolean submit(Actor owner, EditSession editSession, Operation operation, OperationCompletion completion) {
        try {
            Operations.complete(operation);
        } catch (WorldEditException e) {
            completion.onFailure(e);
            return false;
        }

        completion.onSuccess();
        return false;
    }

    @Override
    public boolean isPending(EditSession editSession) {
        return false;
    }

    @Override
    public void whenFinished(Actor owner, EditSession editSession, Runnable task) {
        editSession.flushQueue();
        task.run();
    }

    @Override
    public List<EditSession> cancel(UUID owner) {
        return Collections.emptyList();
    }

    @Override
    public boolean hasPendingWork(UUID owner) {
        return false;
    }

    @Override
    public void shutdown() {}
}
