package com.sk89q.worldedit.blocks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Test;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.LiquidBlockMask;

public class ModdedBlockPropertiesTest {

    private static final int MODDED_STONE = 1000;
    private static final int MODDED_FLOWER = 1001;
    private static final int MODDED_OIL = 1002;

    private final Map<Integer, Integer> asked = new HashMap<Integer, Integer>();

    private final ModdedBlockProperties properties = new ModdedBlockProperties() {

        @Override
        public boolean canPassThrough(int id) {
            asked.merge(id, 1, Integer::sum);
            return id != MODDED_STONE;
        }

        @Override
        public boolean isLiquid(int id) {
            return id == MODDED_OIL;
        }

        @Override
        public boolean isTranslucent(int id) {
            return id != MODDED_STONE;
        }

        @Override
        public boolean isNaturalTerrain(int id) {
            return id == MODDED_STONE;
        }
    };

    @After
    public void clear() {
        BlockType.setModdedProperties(null);
    }

    @Test
    public void withoutPropertiesModdedBlocksAreSolidAndNotTerrain() {
        assertFalse(BlockType.canPassThrough(MODDED_FLOWER, 0));
        assertFalse(BlockType.isLiquid(MODDED_OIL));
        assertFalse(BlockType.isNaturalTerrainBlock(MODDED_STONE, 0));
    }

    @Test
    public void moddedBlocksFollowTheirProperties() {
        BlockType.setModdedProperties(properties);

        assertTrue(BlockType.canPassThrough(MODDED_FLOWER, 3));
        assertFalse(BlockType.canPassThrough(MODDED_STONE, 0));
        assertTrue(BlockType.isLiquid(MODDED_OIL));
        assertFalse(BlockType.isLiquid(MODDED_STONE));
        assertTrue(BlockType.isNaturalTerrainBlock(MODDED_STONE, 2));
        assertFalse(BlockType.isNaturalTerrainBlock(MODDED_FLOWER, 0));
        assertTrue(BlockType.isTranslucent(MODDED_FLOWER));
        assertFalse(BlockType.isTranslucent(MODDED_STONE));
    }

    @Test
    public void vanillaBlocksKeepTheirTables() {
        BlockType.setModdedProperties(properties);

        assertFalse(BlockType.canPassThrough(BlockID.STONE, 0));
        assertTrue(BlockType.canPassThrough(BlockID.TORCH, 0));
        assertTrue(BlockType.isLiquid(BlockID.STATIONARY_WATER));
        assertTrue(BlockType.isNaturalTerrainBlock(BlockID.DIRT, 0));
        assertFalse(BlockType.isNaturalTerrainBlock(BlockID.COBBLESTONE, 0));
    }

    @Test
    public void eachBlockIsAskedAboutOnce() {
        BlockType.setModdedProperties(properties);

        for (int i = 0; i < 10; i++) {
            BlockType.canPassThrough(MODDED_FLOWER, 0);
            BlockType.isNaturalTerrainBlock(MODDED_FLOWER, 0);
        }

        assertEquals(Integer.valueOf(1), asked.get(MODDED_FLOWER));
    }

    @Test
    public void theLiquidMaskMatchesModdedLiquids() {
        BlockType.setModdedProperties(properties);
        Extent extent = mock(Extent.class);
        when(extent.getLazyBlock(any(Vector.class)))
            .thenReturn(new BaseBlock(MODDED_OIL), new BaseBlock(BlockID.LAVA), new BaseBlock(MODDED_STONE));
        LiquidBlockMask mask = new LiquidBlockMask(extent);

        assertTrue(mask.test(Vector.ZERO));
        assertTrue(mask.test(Vector.ZERO));
        assertFalse(mask.test(Vector.ZERO));
    }
}
