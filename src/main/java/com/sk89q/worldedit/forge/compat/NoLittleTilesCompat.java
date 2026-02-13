package com.sk89q.worldedit.forge.compat;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class NoLittleTilesCompat implements LittleTilesCompat{

    @Override
    public void sendDescPacket(World world, TileEntity entity) {

    }
}
