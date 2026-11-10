package com.sk89q.worldedit.blocks;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;

/**
 * EndlessIDs replaces {@link BaseBlock#getData()} so it reads the stored value as unsigned, which turns
 * the wildcard value of -1 into 65535. {@link EndlessIdsBlock} behaves the same way, because the mixin
 * isn't applied in tests.
 */
public class BaseBlockWildcardTest {

    private static final class EndlessIdsBlock extends BaseBlock {

        private EndlessIdsBlock(int id, int data) {
            super(id, data);
        }

        @Override
        public int getData() {
            return super.getData() & 0xFFFF;
        }
    }

    @Test
    public void wildcardMatchesAnyDataWithEndlessIds() {
        BaseBlock wildcard = new EndlessIdsBlock(1, -1);

        assertTrue(wildcard.hasWildcardData());
        assertTrue(Blocks.containsFuzzy(Collections.singleton(wildcard), new EndlessIdsBlock(1, 0)));
        assertTrue(Blocks.containsFuzzy(Collections.singleton(wildcard), new EndlessIdsBlock(1, 5)));
    }

    @Test
    public void wildcardWorksInBothDirections() {
        assertTrue(new EndlessIdsBlock(1, 5).equalsFuzzy(new EndlessIdsBlock(1, -1)));
    }

    @Test
    public void wildcardStillNeedsTheSameId() {
        assertFalse(Blocks.containsFuzzy(Collections.singleton(new EndlessIdsBlock(1, -1)), new EndlessIdsBlock(2, 0)));
    }

    @Test
    public void exactDataOnlyMatchesItself() {
        BaseBlock block = new EndlessIdsBlock(1, 3);

        assertFalse(block.hasWildcardData());
        assertTrue(block.equalsFuzzy(new EndlessIdsBlock(1, 3)));
        assertFalse(block.equalsFuzzy(new EndlessIdsBlock(1, 4)));
    }

    @Test
    public void worksWithoutEndlessIdsToo() {
        assertTrue(new BaseBlock(1, -1).hasWildcardData());
        assertTrue(new BaseBlock(1, -1).equalsFuzzy(new BaseBlock(1, 7)));
        assertFalse(new BaseBlock(1, 2).equalsFuzzy(new BaseBlock(1, 7)));
    }
}
