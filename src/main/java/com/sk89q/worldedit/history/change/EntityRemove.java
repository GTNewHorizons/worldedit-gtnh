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

package com.sk89q.worldedit.history.change;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.history.UndoContext;
import com.sk89q.worldedit.util.Location;

/**
 * Tracks the removal of an entity.
 */
public class EntityRemove implements Change {

    private final Location location;
    private final BaseEntity state;
    private final boolean withoutDrops;
    private Entity entity;

    /**
     * Create a new instance.
     *
     * @param location the location
     * @param state    the state of the created entity
     */
    public EntityRemove(Location location, BaseEntity state) {
        this(location, state, false);
    }

    /**
     * Create a new instance.
     *
     * @param location     the location
     * @param state        the state of the created entity
     * @param withoutDrops true if the entity was removed without its drops, and redo should do the same
     */
    public EntityRemove(Location location, BaseEntity state, boolean withoutDrops) {
        checkNotNull(location);
        checkNotNull(state);
        this.location = location;
        this.state = state;
        this.withoutDrops = withoutDrops;
    }

    @Override
    public void undo(UndoContext context) throws WorldEditException {
        // An undo that was cancelled partway runs again: don't restore the entity twice
        if (entity == null) {
            entity = checkNotNull(context.getExtent()).createEntity(location, state);
            if (entity == null) {
                context.addEntityNotCreated();
            }
        }
    }

    @Override
    public void redo(UndoContext context) throws WorldEditException {
        if (entity != null) {
            if (withoutDrops) {
                entity.removeWithoutDrops();
            } else {
                entity.remove();
            }
            entity = null;
        }
    }

}
