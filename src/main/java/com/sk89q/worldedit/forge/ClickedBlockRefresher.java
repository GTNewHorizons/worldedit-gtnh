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
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.tileentity.TileEntity;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Sends a block's tile entity back to a player whose click on it WorldEdit cancelled, such as a wand
 * click in creative.
 *
 * <p>
 * A creative client breaks the block straight away. The server then refuses and resends the block,
 * but not its tile entity, so the client is left with an empty one. That can crash it, for example
 * when opening an Et Futurum shulker box. The tile entity goes out at the end of the tick, after the
 * server's own block resend.
 * </p>
 */
public final class ClickedBlockRefresher {

    private final List<Refresh> pending = new ArrayList<Refresh>();
    private boolean registered;

    void register() {
        if (!registered) {
            FMLCommonHandler.instance()
                .bus()
                .register(this);
            registered = true;
        }
    }

    void unregister() {
        if (registered) {
            FMLCommonHandler.instance()
                .bus()
                .unregister(this);
            registered = false;
        }
        pending.clear();
    }

    /**
     * Resend the block at the given position to the player at the end of this tick, if it has a tile
     * entity.
     */
    void refresh(EntityPlayerMP player, int x, int y, int z) {
        if (player.worldObj.getTileEntity(x, y, z) != null) {
            pending.add(new Refresh(player, x, y, z));
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pending.isEmpty()) {
            return;
        }

        List<Refresh> due = new ArrayList<Refresh>(pending);
        pending.clear();
        for (Refresh refresh : due) {
            EntityPlayerMP player = refresh.player;
            if (player.playerNetServerHandler == null) {
                continue;
            }
            TileEntity tile = player.worldObj.getTileEntity(refresh.x, refresh.y, refresh.z);
            if (tile == null) {
                continue;
            }
            player.playerNetServerHandler
                .sendPacket(new S23PacketBlockChange(refresh.x, refresh.y, refresh.z, player.worldObj));
            Packet description = tile.getDescriptionPacket();
            if (description != null) {
                player.playerNetServerHandler.sendPacket(description);
            }
        }
    }

    private static final class Refresh {

        private final EntityPlayerMP player;
        private final int x;
        private final int y;
        private final int z;

        private Refresh(EntityPlayerMP player, int x, int y, int z) {
            this.player = player;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
