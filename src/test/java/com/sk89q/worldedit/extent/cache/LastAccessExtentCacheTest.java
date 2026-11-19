package com.sk89q.worldedit.extent.cache;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;

public class LastAccessExtentCacheTest {

    private final Extent extent = mock(Extent.class);
    private final LastAccessExtentCache cache = new LastAccessExtentCache(extent);

    @Test
    public void readingTheSamePositionTwiceReadsOnce() {
        when(extent.getLazyBlock(any(Vector.class))).thenReturn(new BaseBlock(1));

        cache.getLazyBlock(new Vector(1, 2, 3));
        cache.getLazyBlock(new Vector(1.2, 2.2, 3.2));

        verify(extent, times(1)).getLazyBlock(any(Vector.class));
    }

    @Test
    public void aWriteToTheCachedPositionIsNotHiddenByTheCache() throws Exception {
        Vector position = new Vector(1, 2, 3);
        when(extent.getLazyBlock(position)).thenReturn(new BaseBlock(1), new BaseBlock(5));

        cache.getLazyBlock(position);
        cache.setBlock(position, new BaseBlock(5));

        assertEquals(
            5,
            cache.getLazyBlock(position)
                .getType());
    }

    @Test
    public void anotherPositionIsReadAgain() {
        when(extent.getLazyBlock(any(Vector.class))).thenReturn(new BaseBlock(1));

        cache.getLazyBlock(new Vector(1, 2, 3));
        cache.getLazyBlock(new Vector(1, 2, 4));

        verify(extent, times(2)).getLazyBlock(any(Vector.class));
    }
}
