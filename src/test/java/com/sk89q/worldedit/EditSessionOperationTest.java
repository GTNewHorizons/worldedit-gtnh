package com.sk89q.worldedit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.operation.CountedOperation;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.BlockPattern;
import com.sk89q.worldedit.function.visitor.RegionVisitor;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;

/**
 * An edit built for the scheduler may run long after the command returned, while the region it was
 * given is the player's live selection. These change that selection after building each operation.
 */
public class EditSessionOperationTest {

    private static final int BLOCKS = 8;

    private final BlockPattern pattern = new BlockPattern(new BaseBlock(1));
    private final CuboidRegion selection = new CuboidRegion(new Vector(0, 0, 0), new Vector(1, 1, 1));
    private EditSession session;

    @Before
    public void setUp() throws Exception {
        session = mock(EditSession.class, CALLS_REAL_METHODS);
        doReturn(true).when(session)
            .setBlock(any(Vector.class), any(BaseBlock.class));
        doReturn(new BaseBlock(1)).when(session)
            .getBlock(any(Vector.class));
        doReturn(new BaseBlock(1)).when(session)
            .getLazyBlock(any(Vector.class));
        doReturn(Collections.emptyList()).when(session)
            .getEntities(any(Region.class));
    }

    private void changeSelection() {
        selection.setPos2(new Vector(20, 20, 20));
    }

    @Test
    public void setBlocksKeepsTheRegionItWasBuiltWith() throws Exception {
        RegionVisitor operation = session.setBlocksOperation(selection, pattern);
        changeSelection();

        Operations.complete(operation);

        assertEquals(BLOCKS, operation.getAffected());
    }

    @Test
    public void replaceBlocksKeepsTheRegionItWasBuiltWith() throws Exception {
        RegionVisitor operation = session.replaceBlocksOperation(selection, Masks.alwaysTrue(), pattern);
        changeSelection();

        Operations.complete(operation);

        assertEquals(BLOCKS, operation.getAffected());
    }

    @Test
    public void stackKeepsTheRegionItWasBuiltWith() throws Exception {
        ForwardExtentCopy operation = session.stackCuboidRegionOperation(selection, new Vector(1, 0, 0), 1, true);
        changeSelection();

        Operations.complete(operation);

        assertEquals(BLOCKS, operation.getAffected());
    }

    @Test
    public void moveKeepsTheRegionItWasBuiltWith() throws Exception {
        CountedOperation operation = session
            .moveRegionOperation(selection, new Vector(1, 0, 0), 1, true, new BaseBlock(0));
        changeSelection();

        Operations.complete(operation);

        assertEquals(BLOCKS, operation.getAffected());
    }

    @Test
    public void hittingTheChangeLimitExplainsItself() {
        String message = new MaxChangedBlocksException(5).getMessage();

        assertTrue(message, message != null && message.contains("5"));
    }
}
