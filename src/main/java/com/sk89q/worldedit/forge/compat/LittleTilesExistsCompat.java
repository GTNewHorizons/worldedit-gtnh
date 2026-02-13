package com.sk89q.worldedit.forge.compat;

import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class LittleTilesExistsCompat implements LittleTilesCompat{

    @Override
    public void sendDescPacket(World world, TileEntity entity) {
        if (entity instanceof TileEntityLittleTiles littleTile) {
            littleTile.needFullUpdate = true;
            world.markBlockForUpdate(littleTile.xCoord, littleTile.yCoord, littleTile.zCoord);
            littleTile.markDirty();
        }

    }
}
