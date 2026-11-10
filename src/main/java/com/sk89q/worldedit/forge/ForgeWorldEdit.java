/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.forge;

import static com.google.common.base.Preconditions.checkNotNull;
import static net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;

import java.io.File;
import java.util.Map;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;

import org.apache.logging.log4j.Logger;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WETags;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldVector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.command.brigadier.CommandRegistry;
import com.sk89q.worldedit.event.platform.PlatformReadyEvent;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.forge.compat.ArchitectureCraftBlockTransformHook;
import com.sk89q.worldedit.forge.compat.CarpentersBlocksBlockTransformHook;
import com.sk89q.worldedit.forge.compat.ForgeMultipartCompat;
import com.sk89q.worldedit.forge.compat.ForgeMultipartExistsCompat;
import com.sk89q.worldedit.forge.compat.ModRotationBlockTransformHook;
import com.sk89q.worldedit.forge.compat.NoForgeMultipartCompat;
import com.sk89q.worldedit.forge.compat.ServerUtilitiesPermissionsProvider;
import com.sk89q.worldedit.forge.compat.rotation.RotationMappings;
import com.sk89q.worldedit.util.lighting.LightingScheduler;
import com.sk89q.worldedit.util.operation.OperationScheduler;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.eventhandler.Event.Result;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * The Forge implementation of WorldEdit.
 */
@Mod(
    modid = ForgeWorldEdit.MOD_ID,
    name = "WorldEdit",
    version = WETags.VERSION,
    acceptableRemoteVersions = "*",
    dependencies = "required-after:gtnhlib;after:ForgeMultipart;after:ArchitectureCraft;after:CarpentersBlocks;after:serverutilities")
public class ForgeWorldEdit {

    public static Logger logger;
    public static final String MOD_ID = "worldedit";
    public static final String CUI_PLUGIN_CHANNEL = "WECUI";

    private ForgePermissionsProvider provider;

    @Instance(MOD_ID)
    public static ForgeWorldEdit inst;

    @SidedProxy(
        serverSide = "com.sk89q.worldedit.forge.CommonProxy",
        clientSide = "com.sk89q.worldedit.forge.ClientProxy")
    public static CommonProxy proxy;

    private ForgePlatform platform;
    private ForgeConfiguration config;
    private File workingDir;
    private ForgeMultipartCompat compat = new NoForgeMultipartCompat();
    private ModRotationBlockTransformHook modRotationHook;
    private ForgeLightingScheduler lightingScheduler;
    private ForgeOperationScheduler operationScheduler;
    private ClickedBlockRefresher clickedBlockRefresher;
    private OperationScheduler previousOperationScheduler;
    private LightingScheduler previousLightingScheduler;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        // Setup working directory
        workingDir = new File(event.getModConfigurationDirectory() + File.separator + "worldedit");
        workingDir.mkdir();

        config = new ForgeConfiguration(this);
        config.load();

        if (Loader.isModLoaded("ForgeMultipart")) {
            compat = new ForgeMultipartExistsCompat();
            ForgeWorldData.getInstance()
                .addBlockTransformHook((ForgeMultipartExistsCompat) compat);
        }
        if (Loader.isModLoaded("ArchitectureCraft")) {
            ForgeWorldData.getInstance()
                .addBlockTransformHook(new ArchitectureCraftBlockTransformHook());
        }
        if (Loader.isModLoaded("CarpentersBlocks")) {
            ForgeWorldData.getInstance()
                .addBlockTransformHook(new CarpentersBlocksBlockTransformHook());
        }
        modRotationHook = new ModRotationBlockTransformHook();
        ForgeWorldData.getInstance()
            .addBlockTransformHook(modRotationHook);

        FMLCommonHandler.instance()
            .bus()
            .register(ThreadSafeCache.getInstance());

        // EndlessIDs (and NEID Unofficial before it) widens block metadata to 16 bits and mixes
        // into BaseBlock to match. Without this the block parser rejects any metadata above 15.
        if (Loader.isModLoaded("endlessids") || Loader.isModLoaded("neid")) {
            BaseBlock.MAX_DATA = (1 << 16) - 1;
        }
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        proxy.registerHandlers();
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        logger.info("WorldEdit for Forge (version " + getInternalVersion() + ") is loaded");
    }

    @EventHandler
    public void serverAboutToStart(FMLServerAboutToStartEvent event) {
        if (this.platform != null) {
            logger.warn("FMLServerStartingEvent occurred when FMLServerStoppingEvent hasn't");
            WorldEdit.getInstance()
                .getPlatformManager()
                .unregister(platform);
        }

        RotationMappings.init(workingDir);
        ForgeBiomeRegistry.populate();

        this.platform = new ForgePlatform(this);

        WorldEdit.getInstance()
            .getPlatformManager()
            .register(platform);
        final ForgePlatform forgePlatform = platform;
        this.provider = Loader.isModLoaded("serverutilities")
            ? new ServerUtilitiesPermissionsProvider(forgePlatform::getConfiguration)
            : new ForgePermissionsProvider.VanillaPermissionsProvider(platform);

        // GTNHLib creates a new command dispatcher for each server, right before this. Not any later: on Thermos it
        // wraps the registered commands when the server is starting.
        CommandRegistry.registerAll(WorldEdit.getInstance());
        provider.registerNodes();

        previousLightingScheduler = WorldEdit.getInstance()
            .getLightingScheduler();
        BlockType.setPlacementRules(new ForgeBlockPlacementRules(config.placeLastBlocks, config.placeNormallyBlocks));
        BlockType
            .setModdedProperties(new ForgeBlockProperties(config.naturalTerrainBlocks, config.notNaturalTerrainBlocks));

        lightingScheduler = new ForgeLightingScheduler();
        lightingScheduler.register();
        WorldEdit.getInstance()
            .setLightingScheduler(lightingScheduler);

        previousOperationScheduler = WorldEdit.getInstance()
            .getOperationScheduler();
        operationScheduler = new ForgeOperationScheduler();
        operationScheduler.register();
        WorldEdit.getInstance()
            .setOperationScheduler(operationScheduler);

        clickedBlockRefresher = new ClickedBlockRefresher();
        clickedBlockRefresher.register();
    }

    @EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        BlockType.setPlacementRules(null);
        BlockType.setModdedProperties(null);

        if (clickedBlockRefresher != null) {
            clickedBlockRefresher.unregister();
            clickedBlockRefresher = null;
        }

        // Edits finishing here may still ask for relighting, so the lighting scheduler goes last.
        if (operationScheduler != null) {
            operationScheduler.shutdown();
            operationScheduler = null;
        }

        if (previousOperationScheduler != null) {
            WorldEdit.getInstance()
                .setOperationScheduler(previousOperationScheduler);
            previousOperationScheduler = null;
        }

        if (lightingScheduler != null) {
            lightingScheduler.shutdown();
            lightingScheduler = null;
        }

        if (previousLightingScheduler != null) {
            WorldEdit.getInstance()
                .setLightingScheduler(previousLightingScheduler);
            previousLightingScheduler = null;
        }

        WorldEdit.getInstance()
            .getPlatformManager()
            .unregister(platform);
    }

    @EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        WorldEdit.getInstance()
            .getEventBus()
            .post(new PlatformReadyEvent());
    }

    /**
     * A wand never breaks the block it is used on. Vanilla blocks don't get this far, because the click
     * itself is cancelled, but blocks that mods break on their own, such as LittleTiles in creative,
     * only ask through this event.
     */
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (platform == null || !platform.isHookingEvents()) {
            return;
        }
        if (!(event.getPlayer() instanceof EntityPlayerMP) || event.getPlayer() instanceof FakePlayer) {
            return;
        }
        if (WorldEdit.getInstance()
            .getPlatformManager()
            .isUsingWand(wrap((EntityPlayerMP) event.getPlayer()))) {
            event.setCanceled(true);
        }
    }

    /**
     * Discard what a block drops while WorldEdit replaces it. Removing its tile entity beforehand would
     * do the same, but mods whose breakBlock expects their tile entity can crash on that.
     */
    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (!event.world.isRemote && ForgeWorld.isWritingBlock()
            && (event.entity instanceof EntityItem || event.entity instanceof EntityXPOrb)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (platform == null) {
            return;
        }

        if (!platform.isHookingEvents()) return; // We have to be told to catch these events

        if (event.useItem == Result.DENY || event.entity.worldObj.isRemote) return;
        if (event.entityPlayer instanceof FakePlayer) return;

        WorldEdit we = WorldEdit.getInstance();
        if (event.entityPlayer instanceof EntityPlayerMP) {
            EntityPlayerMP playerMP = (EntityPlayerMP) event.entityPlayer; // Safe cast
            ForgePlayer player = wrap(playerMP);
            ForgeWorld world = getWorld(playerMP.worldObj);

            Action action = event.action;
            switch (action) {
                case LEFT_CLICK_BLOCK: {
                    WorldVector pos = new WorldVector(world, event.x, event.y, event.z);

                    if (we.handleBlockLeftClick(player, pos)) {
                        event.setCanceled(true);
                    }

                    if (we.handleArmSwing(player)) {
                        event.setCanceled(true);
                    }

                    if (event.isCanceled() && clickedBlockRefresher != null) {
                        clickedBlockRefresher.refresh(playerMP, event.x, event.y, event.z);
                    }

                    break;
                }
                case RIGHT_CLICK_BLOCK: {
                    WorldVector pos = new WorldVector(world, event.x, event.y, event.z);

                    if (we.handleBlockRightClick(player, pos)) {
                        event.setCanceled(true);
                    }

                    if (we.handleRightClick(player)) {
                        event.setCanceled(true);
                    }

                    break;
                }
                case RIGHT_CLICK_AIR: {
                    if (we.handleRightClick(player)) {
                        event.setCanceled(true);
                    }

                    break;
                }
            }
        }
    }

    public static ItemStack toForgeItemStack(BaseItemStack item) {
        ItemStack ret = new ItemStack(Item.getItemById(item.getType()), item.getAmount(), item.getData());
        for (Map.Entry<Integer, Integer> entry : item.getEnchantments()
            .entrySet()) {
            ret.addEnchantment(
                net.minecraft.enchantment.Enchantment.enchantmentsList[((Integer) entry.getKey())],
                (Integer) entry.getValue());
        }

        return ret;
    }

    /**
     * Get the configuration.
     *
     * @return the Forge configuration
     */
    ForgeConfiguration getConfig() {
        return this.config;
    }

    /**
     * Get the WorldEdit proxy for the given player.
     *
     * @param player the player
     * @return the WorldEdit player
     */
    public ForgePlayer wrap(EntityPlayerMP player) {
        checkNotNull(player);
        return new ForgePlayer(platform, player);
    }

    /**
     * Get the session for a player.
     *
     * @param player the player
     * @return the session
     */
    public LocalSession getSession(EntityPlayerMP player) {
        checkNotNull(player);
        return WorldEdit.getInstance()
            .getSessionManager()
            .get(wrap(player));
    }

    /**
     * Get the WorldEdit proxy for the given world.
     *
     * @param world the world
     * @return the WorldEdit world
     */
    public ForgeWorld getWorld(World world) {
        checkNotNull(world);
        return new ForgeWorld(world);
    }

    /**
     * Get the WorldEdit proxy for the platform.
     *
     * @return the WorldEdit platform
     */
    public Platform getPlatform() {
        return this.platform;
    }

    /**
     * Get the working directory where WorldEdit's files are stored.
     *
     * @return the working directory
     */
    public File getWorkingDir() {
        return this.workingDir;
    }

    public ForgeMultipartCompat getFMPCompat() {
        return compat;
    }

    /**
     * Get the rotation hook used for mod block transformations.
     */
    public ModRotationBlockTransformHook getModRotationHook() {
        return modRotationHook;
    }

    /**
     * Get the version of the WorldEdit-for-Forge implementation.
     *
     * @return a version string
     */
    String getInternalVersion() {
        return ForgeWorldEdit.class.getAnnotation(Mod.class)
            .version();
    }

    public void setPermissionsProvider(ForgePermissionsProvider provider) {
        this.provider = provider;
    }

    public ForgePermissionsProvider getPermissionsProvider() {
        return provider;
    }

}
