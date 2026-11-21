package com.sk89q.worldedit.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.UUID;

import org.junit.Test;
import org.mockito.MockedStatic;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.TestEditSessions;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.command.brigadier.CommandCall;
import com.sk89q.worldedit.command.util.CommandInput;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.forge.ForgeOperationScheduler;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.World;

import cpw.mods.fml.common.gameevent.TickEvent;

public class ScheduledCountTest {

    @Test
    public void countReportsOnlyOnCompletionAndCanBeCancelled() throws Exception {
        UUID owner = UUID.randomUUID();
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(owner);
        World world = mock(World.class);
        when(world.getLazyBlock(any())).thenReturn(new BaseBlock(1, 3));
        EditSession edit = TestEditSessions.create(world);
        CuboidRegion region = new CuboidRegion(new Vector(0, 0, 0), new Vector(2, 0, 0));
        CommandCall call = mock(CommandCall.class);
        when(call.player()).thenReturn(player);
        when(call.queuedEditSession()).thenReturn(edit);
        when(call.region()).thenReturn(region);
        when(call.string("block")).thenReturn("stone");
        ForgeOperationScheduler scheduler = new ForgeOperationScheduler();
        WorldEdit worldEdit = mock(WorldEdit.class);
        when(worldEdit.getOperationScheduler()).thenReturn(scheduler);
        try (MockedStatic<WorldEdit> singleton = mockStatic(WorldEdit.class);
            MockedStatic<CommandInput> input = mockStatic(CommandInput.class)) {
            singleton.when(WorldEdit::getInstance)
                .thenReturn(worldEdit);
            input.when(() -> CommandInput.blockIds(player, "stone", true))
                .thenReturn(Collections.singleton(1));

            SelectionCommands.count(call);
            assertTrue(scheduler.hasPendingWork(owner));
            assertEquals(Collections.singletonList(edit), scheduler.cancel(owner));
            assertFalse(scheduler.hasPendingWork(owner));
            verify(world, never()).getLazyBlock(any());
            verify(player, never()).print(anyString());

            SelectionCommands.count(call);
            region.setPos2(new Vector(20, 0, 0));
            verify(player, never()).print(anyString());
            while (scheduler.hasPendingWork(owner)) {
                scheduler.onServerTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
            }
            verify(player).print("Counted: 3");
        }
    }
}
