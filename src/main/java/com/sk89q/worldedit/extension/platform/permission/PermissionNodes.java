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

package com.sk89q.worldedit.extension.platform.permission;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Every permission node WorldEdit checks, so that a permission system can list them before anyone uses
 * them. Commands declare theirs when they are registered; the rest are checked outside commands and listed
 * here.
 */
public final class PermissionNodes {

    private static final Set<String> NODES = new LinkedHashSet<String>(
        Arrays.asList(
            "worldedit.anyblock",
            "worldedit.butcher.ambient",
            "worldedit.butcher.animals",
            "worldedit.butcher.armorstands",
            "worldedit.butcher.golems",
            "worldedit.butcher.lightning",
            "worldedit.butcher.npcs",
            "worldedit.butcher.pets",
            "worldedit.butcher.tagged",
            "worldedit.history.undo.other",
            "worldedit.history.redo.other",
            OverridePermissions.NO_LIMITS,
            "worldedit.navigation.jumpto.tool",
            "worldedit.navigation.thru.tool",
            "worldedit.override.bedrock"));

    private PermissionNodes() {}

    /**
     * Declare one or more nodes.
     *
     * @param permissions one node, or several separated by |
     */
    public static synchronized void declare(String permissions) {
        for (String node : permissions.split("\\|")) {
            if (!node.isEmpty()) {
                NODES.add(node);
            }
        }
    }

    /**
     * Get every node declared so far.
     *
     * @return a copy of the nodes
     */
    public static synchronized List<String> all() {
        return new ArrayList<String>(NODES);
    }
}
