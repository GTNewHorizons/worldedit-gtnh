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

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
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

    private static final int MULTIPART = 2000;

    private final BlockPlacementRules multipartRules = new BlockPlacementRules() {

        @Override
        public boolean isAttached(int id) {
            return false;
        }

        @Override
        public boolean dependsOnContents(int id) {
            return id == MULTIPART;
        }

        @Override
        public boolean isAttached(BaseBlock block) {
            for (CompoundTag part : block.getNbtData()
                .getList("parts", CompoundTag.class)) {
                if (!part.getString("id")
                    .startsWith("mcr_")) {
                    return true;
                }
            }
            return false;
        }
    };

    private static BaseBlock multipart(String... partIds) {
        List<Tag> parts = new ArrayList<Tag>();
        for (String id : partIds) {
            Map<String, Tag> part = new HashMap<String, Tag>();
            part.put("id", new StringTag(id));
            parts.add(new CompoundTag(part));
        }
        Map<String, Tag> tags = new HashMap<String, Tag>();
        tags.put("id", new StringTag("savedMultipart"));
        tags.put("parts", new ListTag(CompoundTag.class, parts));
        return new BaseBlock(MULTIPART, 0, new CompoundTag(tags));
    }

    @Test
    public void multipartsWithWiresGoInAfterMicroblocksTheySitOn() throws Exception {
        BlockType.setPlacementRules(multipartRules);

        final List<String> written = new ArrayList<String>();
        Extent extent = mock(Extent.class);
        when(extent.getLazyBlock(any(Vector.class))).thenReturn(new BaseBlock(0));
        when(extent.setBlock(any(Vector.class), any(BaseBlock.class))).thenAnswer(new Answer<Boolean>() {

            @Override
            public Boolean answer(InvocationOnMock call) {
                written.add(
                    call.<Vector>getArgument(0)
                        .getBlockX() + "");
                return true;
            }
        });

        MultiStageReorder queue = new MultiStageReorder(extent, true);
        queue.setBlock(new Vector(0, 0, 0), multipart("pr_insulated", "mcr_face"));
        queue.setBlock(new Vector(1, 0, 0), multipart("mcr_face", "mcr_face"));
        queue.setBlock(new Vector(2, 0, 0), new BaseBlock(BlockID.STONE));

        Operations.complete(queue.commit());

        assertEquals(Arrays.asList("1", "2", "0"), written);
    }

    @Test
    public void multipartsWithWiresAreRemovedBeforeTheMicroblocksTheySitOn() throws Exception {
        BlockType.setPlacementRules(multipartRules);

        final List<String> written = new ArrayList<String>();
        Extent extent = mock(Extent.class);
        when(extent.getLazyBlock(new Vector(0, 0, 0))).thenReturn(multipart("mcr_face"));
        when(extent.getLazyBlock(new Vector(1, 0, 0))).thenReturn(multipart("pr_redwire"));
        when(extent.setBlock(any(Vector.class), any(BaseBlock.class))).thenAnswer(new Answer<Boolean>() {

            @Override
            public Boolean answer(InvocationOnMock call) {
                written.add(
                    call.<Vector>getArgument(0)
                        .getBlockX() + ":"
                        + call.<BaseBlock>getArgument(1)
                            .getType());
                return true;
            }
        });

        MultiStageReorder queue = new MultiStageReorder(extent, true);
        queue.setBlock(new Vector(0, 0, 0), new BaseBlock(0));
        queue.setBlock(new Vector(1, 0, 0), new BaseBlock(0));

        Operations.complete(queue.commit());

        assertEquals("1:0", written.get(0));
    }

}
