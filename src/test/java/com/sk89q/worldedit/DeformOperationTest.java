package com.sk89q.worldedit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.TimeBudgetRunContext;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.World;

public class DeformOperationTest {

    private static final int LENGTH = 600;

    private final Map<String, BaseBlock> blocks = new HashMap<String, BaseBlock>();
    private final CuboidRegion region = new CuboidRegion(new Vector(0, 0, 0), new Vector(LENGTH - 1, 0, 0));
    private World world;

    @Before
    public void setUp() throws Exception {
        world = mock(World.class, withSettings().stubOnly());
        when(world.isValidBlockType(anyInt())).thenReturn(true);
        when(world.getMaxY()).thenReturn(255);
        Answer<BaseBlock> read = new Answer<BaseBlock>() {

            @Override
            public BaseBlock answer(InvocationOnMock call) {
                return get(call.<Vector>getArgument(0));
            }
        };
        when(world.getBlock(any(Vector.class))).thenAnswer(read);
        when(world.getLazyBlock(any(Vector.class))).thenAnswer(read);
        when(world.getBlockType(any(Vector.class))).thenAnswer(new Answer<Integer>() {

            @Override
            public Integer answer(InvocationOnMock call) {
                return get(call.<Vector>getArgument(0)).getType();
            }
        });
        when(world.getBlockData(any(Vector.class))).thenReturn(0);
        Answer<Boolean> write = new Answer<Boolean>() {

            @Override
            public Boolean answer(InvocationOnMock call) {
                Vector pos = call.getArgument(0);
                blocks.put(
                    pos.getBlockX() + "," + pos.getBlockY() + "," + pos.getBlockZ(),
                    call.<BaseBlock>getArgument(1));
                return true;
            }
        };
        when(world.setBlock(any(Vector.class), any(BaseBlock.class))).thenAnswer(write);
        when(world.setBlock(any(Vector.class), any(BaseBlock.class), anyBoolean())).thenAnswer(write);

        for (int x = 0; x < LENGTH; x++) {
            blocks.put(x + ",0,0", new BaseBlock(1 + x % 7));
        }
    }

    private BaseBlock get(Vector pos) {
        BaseBlock block = blocks.get(pos.getBlockX() + "," + pos.getBlockY() + "," + pos.getBlockZ());
        return block != null ? block : new BaseBlock(0);
    }

    private void assertMirrored() {
        for (int x = 0; x < LENGTH; x++) {
            assertEquals("x=" + x, 1 + (LENGTH - 1 - x) % 7, get(new Vector(x, 0, 0)).getType());
        }
    }

    private static Vector unit() {
        return Vector.ONE;
    }

    private static Vector zero() {
        return new Vector((LENGTH - 1) / 2.0, 0, 0);
    }

    @Test
    public void mirrorsInOneGo() throws Exception {
        EditSession editSession = TestEditSessions.create(world);
        editSession.deformRegion(region, zero(), unit(), "x=-x");
        editSession.flushQueue();

        assertMirrored();
    }

    @Test
    public void mirrorsCorrectlyWhenPausedAndWritingStraightAway() throws Exception {
        EditSession editSession = TestEditSessions.create(world);
        editSession.disableQueue();
        Operation next = editSession.deformOperation(region, zero(), unit(), "x=-x");

        int resumes = 0;
        while (next != null) {
            next = next.resume(new TimeBudgetRunContext(0));
            resumes++;
        }

        assertMirrored();
        assertEquals(true, resumes > 2);
    }

    @Test
    public void laterChangesToTheSelectionDoNotMatter() throws Exception {
        EditSession editSession = TestEditSessions.create(world);
        EditSession.Deform deform = editSession.deformOperation(region, zero(), unit(), "x=-x");
        region.setPos2(new Vector(10, 0, 0));

        Operation next = deform;
        while (next != null) {
            next = next.resume(new TimeBudgetRunContext(0));
        }
        editSession.flushQueue();

        assertMirrored();
        assertNotNull(deform);
    }
}
