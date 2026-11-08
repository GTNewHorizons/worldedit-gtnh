package com.sk89q.worldedit.blocks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.reorder.MultiStageReorder;
import com.sk89q.worldedit.function.operation.Operations;

public class BlockPlacementRulesTest {

    private static final int MODDED_SOLID = 1000;
    private static final int MODDED_TORCH = 3000;

    private final Map<Integer, Integer> asked = new HashMap<Integer, Integer>();

    private final BlockPlacementRules rules = new BlockPlacementRules() {

        @Override
        public boolean isAttached(int id) {
            asked.merge(id, 1, Integer::sum);
            return id == MODDED_TORCH;
        }
    };

    @After
    public void clearRules() {
        BlockType.setPlacementRules(null);
    }

    @Test
    public void withoutRulesOnlyVanillaBlocksAreSpecial() {
        assertTrue(BlockType.shouldPlaceLast(BlockID.TORCH));
        assertFalse(BlockType.shouldPlaceLast(MODDED_TORCH));
    }

    @Test
    public void moddedBlocksFollowTheRules() {
        BlockType.setPlacementRules(rules);

        assertTrue(BlockType.shouldPlaceLast(MODDED_TORCH));
        assertFalse(BlockType.shouldPlaceLast(MODDED_SOLID));
    }

    @Test
    public void eachBlockIsOnlyAskedAboutOnce() {
        BlockType.setPlacementRules(rules);

        for (int i = 0; i < 1000; i++) {
            BlockType.shouldPlaceLast(MODDED_TORCH);
            BlockType.shouldPlaceLast(MODDED_SOLID);
        }

        assertEquals(Integer.valueOf(1), asked.get(MODDED_TORCH));
        assertEquals(Integer.valueOf(1), asked.get(MODDED_SOLID));
    }

    @Test
    public void vanillaBlocksNeverConsultTheRules() {
        BlockType.setPlacementRules(rules);

        assertTrue(BlockType.shouldPlaceLast(BlockID.TORCH));
        assertFalse(BlockType.shouldPlaceLast(BlockID.STONE));
        assertTrue(BlockType.shouldPlaceFinal(BlockID.SIGN_POST));

        assertTrue(asked.isEmpty());
    }

    @Test
    public void settingRulesForgetsOldAnswers() {
        BlockType.setPlacementRules(rules);
        assertTrue(BlockType.shouldPlaceLast(MODDED_TORCH));

        // IDs are only fixed per world, so a new server must not reuse them.
        BlockType.setPlacementRules(new BlockPlacementRules() {

            @Override
            public boolean isAttached(int id) {
                return false;
            }
        });

        assertFalse(BlockType.shouldPlaceLast(MODDED_TORCH));
    }

    @Test
    public void attachedBlocksAreWrittenLastWhateverOrderTheyArrive() throws Exception {
        BlockType.setPlacementRules(rules);

        final List<Integer> written = new ArrayList<Integer>();
        Extent extent = mock(Extent.class);
        when(extent.getLazyBlock(any(Vector.class))).thenReturn(new BaseBlock(0));
        when(extent.setBlock(any(Vector.class), any(BaseBlock.class))).thenAnswer(new Answer<Boolean>() {

            @Override
            public Boolean answer(InvocationOnMock call) {
                written.add(
                    call.<BaseBlock>getArgument(1)
                        .getType());
                return true;
            }
        });

        MultiStageReorder queue = new MultiStageReorder(extent, true);
        queue.setBlock(new Vector(0, 0, 0), new BaseBlock(MODDED_TORCH));
        queue.setBlock(new Vector(1, 0, 0), new BaseBlock(MODDED_SOLID));
        queue.setBlock(new Vector(2, 0, 0), new BaseBlock(BlockID.TORCH));
        queue.setBlock(new Vector(3, 0, 0), new BaseBlock(BlockID.STONE));

        Operations.complete(queue.commit());

        assertEquals(Arrays.asList(MODDED_SOLID, BlockID.STONE, MODDED_TORCH, BlockID.TORCH), written);
    }
}
