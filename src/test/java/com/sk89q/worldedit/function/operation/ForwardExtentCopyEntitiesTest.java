package com.sk89q.worldedit.function.operation;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.util.Location;

public class ForwardExtentCopyEntitiesTest {

    @Test
    public void countsEntitiesTheDestinationRefuses() throws Exception {
        CuboidRegion region = new CuboidRegion(new Vector(0, 0, 0), new Vector(1, 0, 0));
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        clipboard.createEntity(new Location(clipboard, new Vector(0.5, 0, 0)), new BaseEntity("Pig"));

        Extent destination = mock(Extent.class);
        when(destination.setBlock(any(Vector.class), any(BaseBlock.class))).thenReturn(true);
        when(destination.createEntity(any(Location.class), any(BaseEntity.class))).thenReturn(null);

        ForwardExtentCopy copy = new ForwardExtentCopy(clipboard, region, destination, new Vector(10, 0, 0));
        Operations.completeLegacy(copy);

        assertEquals(1, copy.getEntitiesNotCreated());
    }

    @Test
    public void clipboardFindsAnEntityAnywhereInItsLastBlock() {
        CuboidRegion region = new CuboidRegion(new Vector(0, 0, 0), new Vector(4, 0, 0));
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        clipboard.createEntity(new Location(clipboard, new Vector(4.5, 0.5, 0.5)), new BaseEntity("Pig"));
        clipboard.createEntity(new Location(clipboard, new Vector(5, 0, 0)), new BaseEntity("Pig"));

        assertEquals(
            1,
            clipboard.getEntities(region)
                .size());
    }
}
