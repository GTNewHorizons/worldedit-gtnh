package com.sk89q.worldedit.command;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.BlockPattern;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.registry.WorldData;

public class ClipboardCommandsTest {

    private static final int AIR = 0;
    private static final int STONE = 1;
    private static final int DIRT = 3;
    private static final int GLASS = 20;

    private static final Vector STONE_POSITION = new Vector(0, 0, 0);
    private static final Vector DIRT_POSITION = new Vector(1, 0, 0);

    private final Map<Vector, Integer> world = new HashMap<>();
    private final Player player = mock(Player.class);
    private final LocalSession session = mock(LocalSession.class);
    private final Region region = new CuboidRegion(STONE_POSITION, DIRT_POSITION);
    private EditSession editSession;

    private static Vector key(Vector position) {
        return new Vector(position.getBlockX(), position.getBlockY(), position.getBlockZ());
    }

    private int typeAt(Vector position) {
        return world.getOrDefault(position, AIR);
    }

    @Before
    public void setUp() throws Exception {
        world.put(STONE_POSITION, STONE);
        world.put(DIRT_POSITION, DIRT);

        editSession = mock(EditSession.class, CALLS_REAL_METHODS);
        Answer<BaseBlock> read = call -> new BaseBlock(world.getOrDefault(key(call.getArgument(0)), AIR));
        doAnswer(read).when(editSession)
            .getBlock(any(Vector.class));
        doAnswer(read).when(editSession)
            .getLazyBlock(any(Vector.class));
        doAnswer(call -> {
            world.put(key(call.getArgument(0)), ((BaseBlock) call.getArgument(1)).getType());
            return true;
        }).when(editSession)
            .setBlock(any(Vector.class), any(BaseBlock.class));
        doReturn(Collections.emptyList()).when(editSession)
            .getEntities(any(Region.class));

        World editedWorld = mock(World.class);
        when(editedWorld.getWorldData()).thenReturn(mock(WorldData.class));
        doReturn(editedWorld).when(editSession)
            .getWorld();
        when(session.getPlacementPosition(player)).thenReturn(STONE_POSITION);
    }

    private Mask onlyStone() {
        return new BlockMask(editSession, new BaseBlock(STONE, -1));
    }

    private Clipboard cut(int leave, Mask mask) throws Exception {
        ClipboardCommands
            .cut(player, session, editSession, region, new BlockPattern(new BaseBlock(leave)), false, mask);
        ArgumentCaptor<ClipboardHolder> holder = ArgumentCaptor.forClass(ClipboardHolder.class);
        verify(session).setClipboard(holder.capture());
        return holder.getValue()
            .getClipboard();
    }

    @Test
    public void cutRemovesEverythingWithoutAMask() throws Exception {
        Clipboard clipboard = cut(AIR, null);

        assertEquals(AIR, typeAt(STONE_POSITION));
        assertEquals(AIR, typeAt(DIRT_POSITION));
        assertEquals(
            STONE,
            clipboard.getBlock(STONE_POSITION)
                .getType());
        assertEquals(
            DIRT,
            clipboard.getBlock(DIRT_POSITION)
                .getType());
    }

    @Test
    public void cutWithAMaskLeavesOtherBlocksInTheWorld() throws Exception {
        Clipboard clipboard = cut(AIR, onlyStone());

        assertEquals(AIR, typeAt(STONE_POSITION));
        assertEquals(DIRT, typeAt(DIRT_POSITION));
        assertEquals(
            STONE,
            clipboard.getBlock(STONE_POSITION)
                .getType());
        assertEquals(
            AIR,
            clipboard.getBlock(DIRT_POSITION)
                .getType());
    }

    @Test
    public void cutWithAMaskOnlyLeavesThePatternWhereABlockWasCut() throws Exception {
        cut(GLASS, onlyStone());

        assertEquals(GLASS, typeAt(STONE_POSITION));
        assertEquals(DIRT, typeAt(DIRT_POSITION));
    }
}
