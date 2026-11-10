package com.sk89q.worldedit.extension.platform;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.platform.BlockInteractEvent;
import com.sk89q.worldedit.event.platform.InputType;
import com.sk89q.worldedit.event.platform.Interaction;
import com.sk89q.worldedit.event.platform.PlatformReadyEvent;
import com.sk89q.worldedit.event.platform.PlayerInputEvent;
import com.sk89q.worldedit.extension.platform.permission.ActorSelectorLimits;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.session.SessionManager;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.eventbus.EventBus;
import com.sk89q.worldedit.util.operation.OperationScheduler;
import com.sk89q.worldedit.world.World;

/** Which items act as wands, and that holding one is never enough without permission. */
public class WandTest {

    private static final int AXE = 271;
    private static final int COMPASS = 345;

    private final LocalConfiguration config = new LocalConfiguration() {

        @Override
        public void load() {}
    };
    private final Player player = mock(Player.class);
    private final RegionSelector selector = mock(RegionSelector.class);
    private final World world = mock(World.class);
    private PlatformManager manager;
    private Platform platform;

    @After
    public void tearDown() {
        WorldEdit.getInstance()
            .getPlatformManager()
            .unregister(platform);
    }

    @Before
    public void setUp() {
        WorldEdit worldEdit = mock(WorldEdit.class);
        when(worldEdit.getEventBus()).thenReturn(new EventBus());
        SessionManager sessions = mock(SessionManager.class);
        LocalSession session = mock(LocalSession.class);
        when(session.getRegionSelector(any(World.class))).thenReturn(selector);
        when(sessions.get(any())).thenReturn(session);
        when(worldEdit.getSessionManager()).thenReturn(sessions);
        when(worldEdit.getOperationScheduler()).thenReturn(mock(OperationScheduler.class));

        Platform platform = mock(Platform.class);
        Map<Capability, Preference> capabilities = new EnumMap<Capability, Preference>(Capability.class);
        for (Capability capability : Capability.values()) {
            capabilities.put(capability, Preference.PREFERRED);
        }
        when(platform.getCapabilities()).thenReturn(capabilities);
        when(platform.getConfiguration()).thenReturn(config);
        when(platform.getVersion()).thenReturn("test");

        manager = new PlatformManager(worldEdit);
        manager.register(platform);
        manager.handlePlatformReady(new PlatformReadyEvent());
        // Selection limits are read from the global instance.
        this.platform = platform;
        WorldEdit.getInstance()
            .getPlatformManager()
            .register(platform);
        WorldEdit.getInstance()
            .getPlatformManager()
            .handlePlatformReady(new PlatformReadyEvent());

        when(player.getWorld()).thenReturn(world);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
    }

    private void hold(int item, String wandKind) {
        when(player.getItemInHand()).thenReturn(item);
        when(player.isHoldingWand(anyString())).thenReturn(false);
        if (wandKind != null) {
            when(player.isHoldingWand(wandKind)).thenReturn(true);
        }
    }

    private void allow(String permission) {
        when(player.hasPermission(permission)).thenReturn(true);
    }

    private boolean leftClickBlock() {
        BlockInteractEvent event = new BlockInteractEvent(
            player,
            new Location(world, new Vector(1, 2, 3)),
            Interaction.HIT);
        manager.handleBlockInteract(event);
        return event.isCancelled();
    }

    private boolean leftClickAir() {
        PlayerInputEvent event = new PlayerInputEvent(player, InputType.PRIMARY);
        manager.handlePlayerInput(event);
        return event.isCancelled();
    }

    @Test
    public void aGivenWandSelects() {
        allow("worldedit.selection.pos");
        hold(AXE, Player.SELECTION_WAND);

        assertTrue(leftClickBlock());
        verify(selector).selectPrimary(any(Vector.class), any(ActorSelectorLimits.class));
    }

    @Test
    public void anOrdinaryAxeIsJustAnAxe() {
        allow("worldedit.selection.pos");
        hold(AXE, null);

        assertFalse(leftClickBlock());
        verify(selector, never()).selectPrimary(any(Vector.class), any(ActorSelectorLimits.class));
    }

    @Test
    public void aGivenWandDoesNothingWithoutPermission() {
        hold(AXE, Player.SELECTION_WAND);

        assertFalse(leftClickBlock());
        verify(selector, never()).selectPrimary(any(Vector.class), any(ActorSelectorLimits.class));
    }

    @Test
    public void anyAxeSelectsWhenMarksAreNotRequired() {
        config.requireWandTag = false;
        allow("worldedit.selection.pos");
        hold(AXE, null);

        assertTrue(leftClickBlock());
    }

    @Test
    public void anyAxeStillNeedsPermissionWhenMarksAreNotRequired() {
        config.requireWandTag = false;
        hold(AXE, null);

        assertFalse(leftClickBlock());
    }

    @Test
    public void aWandOfTheOtherKindDoesNotSelect() {
        config.wandItem = COMPASS;
        allow("worldedit.selection.pos");
        hold(COMPASS, Player.NAVIGATION_WAND);

        assertFalse(leftClickBlock());
    }

    // Jumping runs real trace code, so these watch for the permission check, which only happens once
    // the item counts as the navigation wand.

    @Test
    public void aGivenCompassIsTheNavigationWand() {
        hold(COMPASS, Player.NAVIGATION_WAND);

        leftClickAir();
        verify(player).hasPermission("worldedit.navigation.jumpto.tool");
    }

    @Test
    public void anOrdinaryCompassIsJustACompass() {
        allow("worldedit.navigation.jumpto.tool");
        hold(COMPASS, null);

        assertFalse(leftClickAir());
        verify(player, never()).hasPermission("worldedit.navigation.jumpto.tool");
    }

    @Test
    public void aGivenCompassDoesNothingWithoutPermission() {
        hold(COMPASS, Player.NAVIGATION_WAND);

        assertFalse(leftClickAir());
        verify(player, never()).printError(anyString());
    }

    @Test
    public void itemZeroTurnsTheWandOffRatherThanMakingAnEmptyHandOne() {
        config.requireWandTag = false;
        config.navigationWand = 0;
        allow("worldedit.navigation.jumpto.tool");
        hold(0, null);

        assertFalse(leftClickAir());
        verify(player, never()).hasPermission("worldedit.navigation.jumpto.tool");
    }
}
