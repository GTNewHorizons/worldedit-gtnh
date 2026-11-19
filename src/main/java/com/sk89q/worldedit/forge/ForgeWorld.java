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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.feature.WorldGenBigMushroom;
import net.minecraft.world.gen.feature.WorldGenBigTree;
import net.minecraft.world.gen.feature.WorldGenCanopyTree;
import net.minecraft.world.gen.feature.WorldGenForest;
import net.minecraft.world.gen.feature.WorldGenMegaJungle;
import net.minecraft.world.gen.feature.WorldGenMegaPineTree;
import net.minecraft.world.gen.feature.WorldGenSavannaTree;
import net.minecraft.world.gen.feature.WorldGenShrub;
import net.minecraft.world.gen.feature.WorldGenSwamp;
import net.minecraft.world.gen.feature.WorldGenTaiga1;
import net.minecraft.world.gen.feature.WorldGenTaiga2;
import net.minecraft.world.gen.feature.WorldGenTrees;
import net.minecraft.world.gen.feature.WorldGenerator;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.blocks.LazyBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.internal.Constants;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.TreeGenerator.TreeType;
import com.sk89q.worldedit.world.AbstractWorld;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.registry.WorldData;

/**
 * An adapter to Minecraft worlds for WorldEdit.
 */
public class ForgeWorld extends AbstractWorld {

    private static final Logger logger = Logger.getLogger(ForgeWorld.class.getCanonicalName());
    private static final Random random = new Random();
    private final WeakReference<World> worldRef;

    /**
     * Construct a new world.
     *
     * @param world the world
     */
    ForgeWorld(World world) {
        checkNotNull(world);
        this.worldRef = new WeakReference<World>(world);
    }

    /**
     * Get the underlying handle to the world.
     *
     * @return the world
     * @throws WorldEditException thrown if a reference to the world was lost (i.e. world was unloaded)
     */
    public World getWorldChecked() throws WorldEditException {
        World world = worldRef.get();
        if (world != null) {
            return world;
        } else {
            throw new WorldReferenceLostException(
                "The reference to the world was lost (i.e. the world may have been unloaded)");
        }
    }

    /**
     * Get the underlying handle to the world.
     *
     * @return the world
     * @throws RuntimeException thrown if a reference to the world was lost (i.e. world was unloaded)
     */
    public World getWorld() {
        World world = worldRef.get();
        if (world != null) {
            return world;
        } else {
            throw new RuntimeException("The reference to the world was lost (i.e. the world may have been unloaded)");
        }
    }

    @Override
    public String getName() {
        return getWorld().getWorldInfo()
            .getWorldName();
    }

    @Override
    public String getDirectoryName() {
        return getWorld().getSaveHandler()
            .getWorldDirectoryName();
    }

    @Nullable
    @Override
    public String getDimensionFolder() {
        return getWorld().provider.getSaveFolder();
    }

    @Override
    public boolean useItem(Vector position, BaseItem item, Direction face) {
        Item nativeItem = Item.getItemById(item.getType());
        ItemStack stack = new ItemStack(nativeItem, 1, item.getData());
        World world = getWorld();
        return stack.tryPlaceItemIntoWorld(
            new WorldEditFakePlayer((WorldServer) world),
            world,
            position.getBlockX(),
            position.getBlockY(),
            position.getBlockZ(),
            ForgeAdapter.adapt(face),
            0,
            0,
            0);
    }

    /** How far outside the block being written its drops may appear. */
    private static final double DROP_MARGIN = 0.1;

    private static World writingWorld;
    private static int writingX;
    private static int writingY;
    private static int writingZ;

    /**
     * Whether something appearing at the given place comes out of the block WorldEdit is replacing
     * right now on the server thread. Items and experience it drops are duplicates of what history
     * keeps. Anything further away is left alone, such as a neighbour popping off or items in a chunk
     * that the write happened to load.
     *
     * @return true if it is inside the block being written
     */
    static boolean isDropOfWrittenBlock(World world, double x, double y, double z) {
        return world == writingWorld && isInsideBlock(writingX, writingY, writingZ, x, y, z);
    }

    static boolean isInsideBlock(int blockX, int blockY, int blockZ, double x, double y, double z) {
        return x >= blockX - DROP_MARGIN && x <= blockX + 1 + DROP_MARGIN
            && y >= blockY - DROP_MARGIN
            && y <= blockY + 1 + DROP_MARGIN
            && z >= blockZ - DROP_MARGIN
            && z <= blockZ + 1 + DROP_MARGIN;
    }

    @Override
    public boolean setBlock(Vector position, BaseBlock block, boolean notifyAndLight) throws WorldEditException {
        checkNotNull(position);
        checkNotNull(block);

        World world = getWorldChecked();
        int x = position.getBlockX();
        int y = position.getBlockY();
        int z = position.getBlockZ();

        // First set the block
        Chunk chunk = world.getChunkFromChunkCoords(x >> 4, z >> 4);
        int previousId = 0;

        if (notifyAndLight) {
            previousId = Block.getIdFromBlock(chunk.getBlock(x & 15, y, z & 15));
        }

        Block target = Block.getBlockById(block.getId());

        // Replacing a block runs its breakBlock, and many mods drop whatever their tile entity holds
        // there: AE2 cable buses drop every part and facade, not just inventories. The edit keeps its
        // own copy for undo, so the items it spawns during the write are discarded.
        boolean successful;
        writingWorld = world;
        writingX = x;
        writingY = y;
        writingZ = z;
        try {
            successful = chunk.func_150807_a(x & 15, y, z & 15, target, block.getData());
        } finally {
            writingWorld = null;
        }

        // Some blocks pick their own metadata when added, such as dispensers and furnaces facing away
        // from their neighbours, which would undo the facing that was copied.
        if (successful && chunk.getBlock(x & 15, y, z & 15) == target
            && chunk.getBlockMetadata(x & 15, y, z & 15) != block.getData()) {
            chunk.setBlockMetadata(x & 15, y, z & 15, block.getData());
        }

        // Create the TileEntity. The chunk ignores a write of the block already there, but its
        // contents can still be different.
        CompoundTag tag = block.getNbtData();
        if (tag != null && (successful || (chunk.getBlock(x & 15, y, z & 15) == target
            && chunk.getBlockMetadata(x & 15, y, z & 15) == block.getData()))) {
            NBTTagCompound nativeTag = NBTConverter.toNative(tag);
            nativeTag.setString("id", block.getNbtId());
            TileEntityUtils.setTileEntity(getWorld(), position, nativeTag);
            successful = true;
        }

        if (notifyAndLight) {
            world.func_147451_t(x, y, z);
            world.markBlockForUpdate(x, y, z);
            world.notifyBlockChange(x, y, z, Block.getBlockById(previousId));

            Block mcBlock = Block.getBlockById(previousId);
            if (mcBlock != null && mcBlock.hasComparatorInputOverride()) {
                world.func_147453_f(x, y, z, Block.getBlockById(block.getId()));
            }
        }

        return successful;
    }

    @Override
    public int getBlockLightLevel(Vector position) {
        checkNotNull(position);
        return getWorld().getBlockLightValue(position.getBlockX(), position.getBlockY(), position.getBlockZ());
    }

    @Override
    public int getEmittedLightLevel(Vector position) {
        checkNotNull(position);
        World world = getWorld();
        return world
            .getSavedLightValue(EnumSkyBlock.Block, position.getBlockX(), position.getBlockY(), position.getBlockZ());
    }

    @Override
    public int getSkyLightLevel(Vector position) {
        checkNotNull(position);
        World world = getWorld();
        return world
            .getSavedLightValue(EnumSkyBlock.Sky, position.getBlockX(), position.getBlockY(), position.getBlockZ());
    }

    @Override
    public void setEmittedLightLevel(Vector position, int level) {
        checkNotNull(position);
        World world = getWorld();
        world
            .setLightValue(EnumSkyBlock.Block, position.getBlockX(), position.getBlockY(), position.getBlockZ(), level);
    }

    @Override
    public void setSkyLightLevel(Vector position, int level) {
        checkNotNull(position);
        World world = getWorld();
        world.setLightValue(EnumSkyBlock.Sky, position.getBlockX(), position.getBlockY(), position.getBlockZ(), level);
    }

    @Override
    public boolean clearContainerBlockContents(Vector position) {
        checkNotNull(position);
        TileEntity tile = getWorld().getTileEntity(position.getBlockX(), position.getBlockY(), position.getBlockZ());
        if ((tile instanceof IInventory)) {
            IInventory inv = (IInventory) tile;
            int size = inv.getSizeInventory();
            for (int i = 0; i < size; i++) {
                inv.setInventorySlotContents(i, null);
            }
            return true;
        }
        return false;
    }

    @Override
    public BaseBiome getBiome(Vector2D position) {
        checkNotNull(position);
        return new BaseBiome(getWorld().getBiomeGenForCoords(position.getBlockX(), position.getBlockZ()).biomeID);
    }

    // EndlessIDs (ForgeWorldMixin) overwrites this for its 16-bit biome IDs. Keep the signature.
    @Override
    public boolean setBiome(Vector2D position, BaseBiome biome) {
        checkNotNull(position);
        checkNotNull(biome);

        Chunk chunk = getWorld().getChunkFromBlockCoords(position.getBlockX(), position.getBlockZ());
        if ((chunk != null) && (chunk.isChunkLoaded)) {
            chunk.getBiomeArray()[((position.getBlockZ() & 0xF) << 4 | position.getBlockX() & 0xF)] = (byte) biome
                .getId();
            return true;
        }

        return false;
    }

    @Override
    public void dropItem(Vector position, BaseItemStack item) {
        checkNotNull(position);
        checkNotNull(item);

        if (item.getType() == 0) {
            return;
        }

        EntityItem entity = new EntityItem(
            getWorld(),
            position.getX(),
            position.getY(),
            position.getZ(),
            ForgeWorldEdit.toForgeItemStack(item));
        entity.delayBeforeCanPickup = 10;
        getWorld().spawnEntityInWorld(entity);
    }

    @Override
    public Operation regenerate(Region region, EditSession editSession) {
        World world = getWorld();
        IChunkProvider provider = world.getChunkProvider();
        if (!(provider instanceof ChunkProviderServer)) {
            return null;
        }

        return new ForgeRegeneration(world, (ChunkProviderServer) provider, region.clone(), editSession, getMaxY());
    }

    /**
     * Send regenerated chunks to the players watching them. A new chunk object reaches clients
     * through nothing else, so without this they keep showing the old terrain.
     */
    static void resendChunks(World world, List<Vector2D> chunks) {
        if (!(world instanceof WorldServer)) {
            return;
        }

        PlayerManager playerManager = ((WorldServer) world).getPlayerManager();

        for (Vector2D coord : chunks) {
            int chunkX = coord.getBlockX();
            int chunkZ = coord.getBlockZ();
            Chunk chunk = null;
            Packet unload = null;
            Packet data = null;

            for (Object entity : world.playerEntities) {
                if (!(entity instanceof EntityPlayerMP)) {
                    continue;
                }

                EntityPlayerMP player = (EntityPlayerMP) entity;
                if (!playerManager.isPlayerWatchingChunk(player, chunkX, chunkZ)) {
                    continue;
                }

                if (chunk == null) {
                    chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
                    unload = new S21PacketChunkData(chunk, true, 0);
                    data = new S21PacketChunkData(chunk, true, 65535);
                }

                // Unloading drops the client's old chunk and its tile entities. Its own chunk is
                // skipped because unloading that would remove and respawn the local player.
                if (player.chunkCoordX != chunkX || player.chunkCoordZ != chunkZ) {
                    player.playerNetServerHandler.sendPacket(unload);
                }
                player.playerNetServerHandler.sendPacket(data);

                for (Object tile : chunk.chunkTileEntityMap.values()) {
                    Packet description = ((TileEntity) tile).getDescriptionPacket();
                    if (description != null) {
                        player.playerNetServerHandler.sendPacket(description);
                    }
                }
            }
        }
    }

    @Nullable
    private static WorldGenerator createWorldGenerator(TreeType type) {
        switch (type) {
            case TREE:
                return new WorldGenTrees(true);
            case BIG_TREE:
                return new WorldGenBigTree(true);
            case REDWOOD:
                return new WorldGenTaiga2(true);
            case TALL_REDWOOD:
                return new WorldGenTaiga1();
            case BIRCH:
                return new WorldGenForest(true, false);
            case JUNGLE:
                return new WorldGenMegaJungle(true, 10, 20, 3, 3);
            case SMALL_JUNGLE:
                return new WorldGenTrees(true, 4 + random.nextInt(7), 3, 3, false);
            case SHORT_JUNGLE:
                return new WorldGenTrees(true, 4 + random.nextInt(7), 3, 3, true);
            case JUNGLE_BUSH:
                return new WorldGenShrub(3, 0);
            case RED_MUSHROOM:
                return new WorldGenBigMushroom(1);
            case BROWN_MUSHROOM:
                return new WorldGenBigMushroom(0);
            case SWAMP:
                return new WorldGenSwamp();
            case ACACIA:
                return new WorldGenSavannaTree(true);
            case DARK_OAK:
                return new WorldGenCanopyTree(true);
            case MEGA_REDWOOD:
                return new WorldGenMegaPineTree(false, random.nextBoolean());
            case TALL_BIRCH:
                return new WorldGenForest(true, true);
            case RANDOM:
            case PINE:
            case RANDOM_REDWOOD:
            default:
                return null;
        }
    }

    @Override
    public boolean generateTree(TreeType type, EditSession editSession, Vector position)
        throws MaxChangedBlocksException {
        WorldGenerator generator = createWorldGenerator(type);
        return generator != null
            ? generator.generate(getWorld(), random, position.getBlockX(), position.getBlockY(), position.getBlockZ())
            : false;
    }

    @Override
    public WorldData getWorldData() {
        return ForgeWorldData.getInstance();
    }

    @Override
    public boolean isValidBlockType(int id) {
        return (id == 0) || (net.minecraft.block.Block.getBlockById(id) != null);
    }

    @Override
    public BaseBlock getBlock(Vector position) {
        World world = getWorld();
        int x = position.getBlockX();
        int y = position.getBlockY();
        int z = position.getBlockZ();
        Chunk chunk = chunkAt(world, x, y, z);
        if (chunk == null) {
            return new BaseBlock(0);
        }

        Block block = chunk.getBlock(x & 15, y, z & 15);
        int data = chunk.getBlockMetadata(x & 15, y, z & 15);
        int id = Block.getIdFromBlock(block);
        if (block.hasTileEntity(data)) {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile != null) {
                return new TileEntityBaseBlock(id, data, tile);
            }
        }
        return new BaseBlock(id, data);
    }

    @Override
    public BaseBlock getLazyBlock(Vector position) {
        World world = getWorld();
        int x = position.getBlockX();
        int y = position.getBlockY();
        int z = position.getBlockZ();
        Chunk chunk = chunkAt(world, x, y, z);
        if (chunk == null) {
            return new LazyBlock(0, 0, this, position);
        }
        return new LazyBlock(
            Block.getIdFromBlock(chunk.getBlock(x & 15, y, z & 15)),
            chunk.getBlockMetadata(x & 15, y, z & 15),
            this,
            position);
    }

    @Override
    public int getBlockType(Vector position) {
        int x = position.getBlockX();
        int y = position.getBlockY();
        int z = position.getBlockZ();
        Chunk chunk = chunkAt(getWorld(), x, y, z);
        return chunk != null ? Block.getIdFromBlock(chunk.getBlock(x & 15, y, z & 15)) : 0;
    }

    @Override
    public int getBlockData(Vector position) {
        int x = position.getBlockX();
        int y = position.getBlockY();
        int z = position.getBlockZ();
        Chunk chunk = chunkAt(getWorld(), x, y, z);
        return chunk != null ? chunk.getBlockMetadata(x & 15, y, z & 15) : 0;
    }

    /**
     * Get the chunk holding a position, looked up once for both the block and its metadata where the
     * world's own getters each look it up again. Null outside the world, where those return air.
     */
    @Nullable
    private static Chunk chunkAt(World world, int x, int y, int z) {
        if (y < 0 || y >= 256 || x < -30000000 || z < -30000000 || x >= 30000000 || z >= 30000000) {
            return null;
        }
        return world.getChunkFromChunkCoords(x >> 4, z >> 4);
    }

    @Override
    public int hashCode() {
        return getWorld().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if ((o instanceof ForgeWorld)) {
            ForgeWorld other = ((ForgeWorld) o);
            World otherWorld = other.worldRef.get();
            World thisWorld = worldRef.get();
            return otherWorld != null && thisWorld != null && otherWorld.equals(thisWorld);
        } else if (o instanceof com.sk89q.worldedit.world.World) {
            return ((com.sk89q.worldedit.world.World) o).getName()
                .equals(getName());
        } else {
            return false;
        }
    }

    /**
     * Fast mode writes without telling clients or updating light, so send the new blocks now and
     * send the chunks again once they have been relit.
     */
    @Override
    public void fixAfterFastMode(Iterable<BlockVector2D> chunks) {
        final List<Vector2D> changed = new ArrayList<Vector2D>();
        for (BlockVector2D chunk : chunks) {
            changed.add(chunk);
        }

        resendChunks(getWorld(), changed);

        WorldEdit.getInstance()
            .getLightingScheduler()
            .schedule(this, chunks, null, new Runnable() {

                @Override
                public void run() {
                    World world = worldRef.get();
                    if (world != null) {
                        resendChunks(world, changed);
                    }
                }
            });
    }

    @Override
    public void fixLighting(Iterable<BlockVector2D> chunks) {
        World world = getWorld();
        boolean hasSky = !world.provider.hasNoSky;
        int maxY = world.getActualHeight();
        for (BlockVector2D chunk : chunks) {
            int cx = chunk.getBlockX();
            int cz = chunk.getBlockZ();
            Chunk mcChunk = world.getChunkFromChunkCoords(cx, cz);
            if (mcChunk == null) {
                continue;
            }
            mcChunk.generateSkylightMap();
            int bx = cx << 4;
            int bz = cz << 4;
            int topY = mcChunk.getTopFilledSegment();
            if (topY < 0) {
                topY = 0;
            } else {
                topY += 16;
                if (topY > maxY) {
                    topY = maxY;
                }
            }
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < topY; y++) {
                        int wx = bx + x;
                        int wy = y;
                        int wz = bz + z;
                        world.updateLightByType(EnumSkyBlock.Block, wx, wy, wz);
                        if (hasSky) {
                            world.updateLightByType(EnumSkyBlock.Sky, wx, wy, wz);
                        }
                    }
                }
            }
            world.markBlockRangeForRenderUpdate(bx, 0, bz, bx + 15, topY - 1, bz + 15);
            mcChunk.setChunkModified();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<? extends Entity> getEntities(Region region) {
        List<Entity> entities = new ArrayList<Entity>();
        World world = getWorld();
        List<net.minecraft.entity.Entity> ents = world.loadedEntityList;
        for (net.minecraft.entity.Entity entity : ents) {
            if (region.contains(new Vector(entity.posX, entity.posY, entity.posZ))) {
                entities.add(new ForgeEntity(entity));
            }
        }
        return entities;
    }

    @Override
    public List<? extends Entity> getEntities() {
        List<Entity> entities = new ArrayList<Entity>();
        for (Object entity : getWorld().loadedEntityList) {
            entities.add(new ForgeEntity((net.minecraft.entity.Entity) entity));
        }
        return entities;
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        World world = getWorld();
        net.minecraft.entity.Entity createdEntity = EntityList.createEntityByName(entity.getTypeId(), world);
        if (createdEntity != null) {
            CompoundTag nativeTag = entity.getNbtData();
            if (nativeTag != null) {
                NBTTagCompound tag = NBTConverter.toNative(entity.getNbtData());
                for (String name : Constants.NO_COPY_ENTITY_NBT_FIELDS) {
                    tag.removeTag(name);
                }
                createdEntity.readFromNBT(tag);
            }

            createdEntity.setLocationAndAngles(
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch());

            world.spawnEntityInWorld(createdEntity);
            return new ForgeEntity(createdEntity);
        } else {
            return null;
        }
    }

    /**
     * Thrown when the reference to the world is lost.
     */
    private static class WorldReferenceLostException extends WorldEditException {

        private WorldReferenceLostException(String message) {
            super(message);
        }
    }

}
