package com.sk89q.worldedit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

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
    private static final int STONE = 1;
    private static final int DIRT = 3;
    private static final int COBBLESTONE = 4;
    private static final int SAND = 12;

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

    /**
     * Makes the session read and write a small world that is air except for the given blocks.
     */
    private Map<Vector, Integer> worldWith(Object... positionsAndTypes) throws Exception {
        Map<Vector, Integer> world = new HashMap<>();
        for (int i = 0; i < positionsAndTypes.length; i += 2) {
            world.put(new Vector((Integer) positionsAndTypes[i], 0, 0), (Integer) positionsAndTypes[i + 1]);
        }
        Answer<BaseBlock> read = call -> new BaseBlock(world.getOrDefault(key(call.getArgument(0)), 0));
        doAnswer(read).when(session)
            .getBlock(any(Vector.class));
        doAnswer(read).when(session)
            .getLazyBlock(any(Vector.class));
        doAnswer(call -> {
            world.put(key(call.getArgument(0)), ((BaseBlock) call.getArgument(1)).getType());
            return true;
        }).when(session)
            .setBlock(any(Vector.class), any(BaseBlock.class));
        return world;
    }

    private static Vector key(Vector position) {
        return new Vector(position.getBlockX(), position.getBlockY(), position.getBlockZ());
    }

    private static int typeAt(Map<Vector, Integer> world, int x) {
        return world.getOrDefault(new Vector(x, 0, 0), 0);
    }

    private CountedOperation moveAlongX(int toX, int distance, boolean copyAir, BaseBlock replacement) {
        return session.moveRegionOperation(
            new CuboidRegion(new Vector(0, 0, 0), new Vector(toX, 0, 0)),
            new Vector(1, 0, 0),
            distance,
            copyAir,
            replacement);
    }

    @Test
    public void moveCopiesAirOverTheDestinationByDefault() throws Exception {
        Map<Vector, Integer> world = worldWith(0, STONE, 6, DIRT);

        Operations.complete(moveAlongX(1, 5, true, null));

        assertEquals(STONE, typeAt(world, 5));
        assertEquals(0, typeAt(world, 6));
    }

    @Test
    public void moveWithoutAirKeepsWhatIsAtTheDestination() throws Exception {
        Map<Vector, Integer> world = worldWith(0, STONE, 6, DIRT);

        Operations.complete(moveAlongX(1, 5, false, null));

        assertEquals(0, typeAt(world, 0));
        assertEquals(STONE, typeAt(world, 5));
        assertEquals(DIRT, typeAt(world, 6));
    }

    @Test
    public void moveWithoutAirOnlyLeavesTheReplacementWhereABlockMoved() throws Exception {
        Map<Vector, Integer> world = worldWith(0, STONE);

        CountedOperation operation = moveAlongX(1, 5, false, new BaseBlock(COBBLESTONE));
        Operations.complete(operation);

        assertEquals(COBBLESTONE, typeAt(world, 0));
        assertEquals(0, typeAt(world, 1));
        assertEquals(1, operation.getAffected());
    }

    @Test
    public void moveWithoutAirWorksWhenTheDestinationOverlapsTheSelection() throws Exception {
        Map<Vector, Integer> world = worldWith(0, STONE, 1, SAND, 3, DIRT);

        Operations.complete(moveAlongX(2, 1, false, null));

        assertEquals(0, typeAt(world, 0));
        assertEquals(STONE, typeAt(world, 1));
        assertEquals(SAND, typeAt(world, 2));
        assertEquals(DIRT, typeAt(world, 3));
    }

    @Test
    public void hittingTheChangeLimitExplainsItself() {
        String message = new MaxChangedBlocksException(5).getMessage();

        assertTrue(message, message != null && message.contains("5"));
    }
}
