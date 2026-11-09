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

package com.sk89q.worldedit.forge.compat;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldSettings.GameType;

import com.sk89q.worldedit.extension.platform.permission.PermissionNodes;
import com.sk89q.worldedit.forge.ForgeConfiguration;
import com.sk89q.worldedit.forge.ForgePermissionsProvider;

import serverutils.lib.util.permission.DefaultPermissionLevel;
import serverutils.lib.util.permission.PermissionAPI;

/**
 * Checks permission nodes through ServerUtilities, so that ranks can grant or deny each one. A node nobody
 * changed is for operators only, as without ServerUtilities.
 */
public class ServerUtilitiesPermissionsProvider implements ForgePermissionsProvider {

    private final Supplier<ForgeConfiguration> configuration;
    private final Set<String> registered = new HashSet<String>();

    public ServerUtilitiesPermissionsProvider(Supplier<ForgeConfiguration> configuration) {
        this.configuration = configuration;
    }

    @Override
    public void registerNodes() {
        for (String node : PermissionNodes.all()) {
            register(node);
        }
    }

    private void register(String node) {
        if (registered.add(node)) {
            PermissionAPI.registerNode(node, DefaultPermissionLevel.OP, "WorldEdit");
        }
    }

    @Override
    public boolean hasPermission(EntityPlayerMP player, String permission) {
        ForgeConfiguration configuration = this.configuration.get();
        if (configuration.cheatMode
            || (configuration.creativeEnable && player.theItemInWorldManager.getGameType() == GameType.CREATIVE)) {
            return true;
        }

        // An unregistered node would be denied even to operators.
        register(permission);
        return PermissionAPI.hasPermission(player, permission);
    }
}
