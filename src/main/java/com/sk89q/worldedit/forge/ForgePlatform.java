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

package com.sk89q.worldedit.forge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.AbstractPlatform;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.MultiUserPlatform;
import com.sk89q.worldedit.extension.platform.Preference;
import com.sk89q.worldedit.util.command.CommandMapping;
import com.sk89q.worldedit.util.command.Dispatcher;
import com.sk89q.worldedit.world.World;

import cpw.mods.fml.common.FMLCommonHandler;

class ForgePlatform extends AbstractPlatform implements MultiUserPlatform {

    private final ForgeWorldEdit mod;
    private final MinecraftServer server;
    private boolean hookingEvents = false;

    ForgePlatform(ForgeWorldEdit mod) {
        this.mod = mod;
        this.server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
    }

    boolean isHookingEvents() {
        return hookingEvents;
    }

    @Override
    public int resolveItem(String name) {
        if (name == null) return 0;

        int index = name.indexOf(':');

        if (index != -1 && index != 0 && index != name.length() - 1) {
            Block block = Block.getBlockFromName(name);
            if (block != null) {
                return Block.getIdFromBlock(block);
            }
        }

        for (Object itemObj : Item.itemRegistry) {
            Item item = (Item) itemObj;
            if (item == null) continue;
            if (item.getUnlocalizedName() == null) continue;
            if (item.getUnlocalizedName()
                .startsWith("item.")) {
                if (item.getUnlocalizedName()
                    .equalsIgnoreCase("item." + name)) return Item.getIdFromItem(item);
            }
            if (item.getUnlocalizedName()
                .startsWith("tile.")) {
                if (item.getUnlocalizedName()
                    .equalsIgnoreCase("tile." + name)) return Item.getIdFromItem(item);
            }
            if (item.getUnlocalizedName()
                .equalsIgnoreCase(name)) return Item.getIdFromItem(item);
        }
        return 0;
    }

    @Override
    public boolean isValidMobType(String type) {
        return EntityList.stringToClassMapping.containsKey(type);
    }

    @Override
    public void reload() {
        getConfiguration().load();
    }

    @Override
    public int schedule(long delay, long period, Runnable task) {
        return -1;
    }

    @Override
    public List<? extends com.sk89q.worldedit.world.World> getWorlds() {
        List<WorldServer> worlds = Arrays.asList(DimensionManager.getWorlds());
        List<com.sk89q.worldedit.world.World> ret = new ArrayList<com.sk89q.worldedit.world.World>(worlds.size());
        for (WorldServer world : worlds) {
            ret.add(new ForgeWorld(world));
        }
        return ret;
    }

    @Nullable
    @Override
    public Player matchPlayer(Player player) {
        if (player instanceof ForgePlayer) {
            return player;
        } else {
            EntityPlayerMP entity = server.getConfigurationManager()
                .func_152612_a(player.getName());
            return entity != null ? new ForgePlayer(this, entity) : null;
        }
    }

    @Nullable
    @Override
    public World matchWorld(World world) {
        if (world instanceof ForgeWorld) {
            return world;
        } else {
            for (WorldServer ws : DimensionManager.getWorlds()) {
                if (ws.getWorldInfo()
                    .getWorldName()
                    .equals(world.getName())) {
                    return new ForgeWorld(ws);
                }
            }

            return null;
        }
    }

    @Override
    public void registerCommands(Dispatcher dispatcher) {
        if (server == null) return;
        ServerCommandManager mcMan = (ServerCommandManager) server.getCommandManager();

        for (final CommandMapping command : dispatcher.getCommands()) {
            CommandWrapper wrapper = new CommandWrapper(command);
            mcMan.registerCommand(wrapper);
            if (command.getDescription()
                .getPermissions()
                .size() > 0) {
                ForgeWorldEdit.inst.getPermissionsProvider()
                    .registerPermission(
                        wrapper,
                        command.getDescription()
                            .getPermissions()
                            .get(0));
                for (int i = 1; i < command.getDescription()
                    .getPermissions()
                    .size(); i++) {
                    ForgeWorldEdit.inst.getPermissionsProvider()
                        .registerPermission(
                            null,
                            command.getDescription()
                                .getPermissions()
                                .get(i));
                }
            }
        }
    }

    @Override
    public void registerGameHooks() {
        // We registered the events already anyway, so we just 'turn them on'
        hookingEvents = true;
    }

    @Override
    public ForgeConfiguration getConfiguration() {
        return mod.getConfig();
    }

    @Override
    public String getVersion() {
        return mod.getInternalVersion();
    }

    @Override
    public String getPlatformName() {
        return "Forge-Official";
    }

    @Override
    public String getPlatformVersion() {
        return mod.getInternalVersion();
    }

    @Override
    public Map<Capability, Preference> getCapabilities() {
        Map<Capability, Preference> capabilities = new EnumMap<Capability, Preference>(Capability.class);
        capabilities.put(Capability.CONFIGURATION, Preference.PREFER_OTHERS);
        capabilities.put(Capability.WORLDEDIT_CUI, Preference.NORMAL);
        capabilities.put(Capability.GAME_HOOKS, Preference.NORMAL);
        capabilities.put(Capability.PERMISSIONS, Preference.NORMAL);
        capabilities.put(Capability.USER_COMMANDS, Preference.NORMAL);
        capabilities.put(Capability.WORLD_EDITING, Preference.PREFERRED);
        return capabilities;
    }

    @Override
    public Collection<Actor> getConnectedUsers() {
        List<Actor> users = new ArrayList<Actor>();
        ServerConfigurationManager scm = server.getConfigurationManager();
        for (String name : scm.getAllUsernames()) {
            EntityPlayerMP entity = scm.func_152612_a(name);
            if (entity != null) {
                users.add(new ForgePlayer(this, entity));
            }
        }
        return users;
    }
}
