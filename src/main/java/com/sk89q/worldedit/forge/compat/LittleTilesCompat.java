package com.sk89q.worldedit.forge.compat;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public interface LittleTilesCompat {

    void sendDescPacket(World world, TileEntity entity);

}
