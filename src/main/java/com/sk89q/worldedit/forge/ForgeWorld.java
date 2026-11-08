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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.ChunkPosition;
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
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.blocks.LazyBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
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

        boolean successful = chunk.func_150807_a(x & 15, y, z & 15, Block.getBlockById(block.getId()), block.getData());

        // Create the TileEntity
        if (successful) {
            CompoundTag tag = block.getNbtData();
            if (tag != null) {
                NBTTagCompound nativeTag = NBTConverter.toNative(tag);
                nativeTag.setString("id", block.getNbtId());
                TileEntityUtils.setTileEntity(getWorld(), position, nativeTag);
            }
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
    public boolean regenerate(Region region, EditSession editSession) {
        World world = getWorld();
        IChunkProvider provider = world.getChunkProvider();
        if (!(provider instanceof ChunkProviderServer)) {
            return false;
        }

        ChunkProviderServer chunkServer = (ChunkProviderServer) provider;

        Set<Vector2D> selected = region.getChunks();
        Map<Long, RegenColumn> columns = snapshotColumns(selected, chunkServer, editSession);

        List<Vector2D> replaced = new ArrayList<Vector2D>();
        boolean success = true;

        // All terrain is replaced before anything is decorated, so features can cross between
        // selected chunks without being wiped by the next chunk's replacement.
        for (Vector2D chunk : selected) {
            try {
                replaceChunk(chunkServer, chunk.getBlockX(), chunk.getBlockZ());
                replaced.add(chunk);
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Failed to regenerate chunk " + chunk, t);
                success = false;
                break;
            }
        }

        success &= decorate(replaced, columns, chunkServer);

        for (RegenColumn column : columns.values()) {
            if (column.selected) {
                restoreSelectedColumn(column, region, editSession);
            } else {
                restoreNeighbourColumn(column, world, editSession);
            }
        }

        // Write the restored blocks first, so the chunks sent to clients are the final result.
        editSession.flushQueue();
        resendChunks(world, replaced);

        return success;
    }

    /**
     * Record the selected chunks and the chunks around them, which decoration can spill into.
     */
    private Map<Long, RegenColumn> snapshotColumns(Set<Vector2D> selected, ChunkProviderServer chunkServer,
        EditSession editSession) {
        Set<Long> selectedKeys = new HashSet<Long>();
        Set<Long> neighbourKeys = new LinkedHashSet<Long>();
        for (Vector2D chunk : selected) {
            selectedKeys.add(ChunkCoordIntPair.chunkXZ2Int(chunk.getBlockX(), chunk.getBlockZ()));
        }

        // Load everything first, because loading a chunk can populate the ones next to it.
        List<Chunk> neighbours = new ArrayList<Chunk>();
        for (Vector2D chunk : selected) {
            chunkServer.loadChunk(chunk.getBlockX(), chunk.getBlockZ());
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int x = chunk.getBlockX() + dx;
                    int z = chunk.getBlockZ() + dz;
                    long key = ChunkCoordIntPair.chunkXZ2Int(x, z);
                    if (!selectedKeys.contains(key) && neighbourKeys.add(key)) {
                        neighbours.add(chunkServer.loadChunk(x, z));
                    }
                }
            }
        }

        int maxY = getMaxY();
        int size = 256 * (maxY + 1);
        BlockCache cache = new BlockCache();
        Map<Long, RegenColumn> columns = new LinkedHashMap<Long, RegenColumn>();

        for (Vector2D chunk : selected) {
            RegenColumn column = new RegenColumn(chunk.getBlockX(), chunk.getBlockZ(), true, size);
            int baseX = column.chunkX << 4;
            int baseZ = column.chunkZ << 4;
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y <= maxY; y++) {
                    for (int z = 0; z < 16; z++) {
                        BaseBlock block = editSession.getBlock(new Vector(baseX + x, y, baseZ + z));
                        column.blocks[columnIndex(x, y, z)] = block.getNbtData() == null
                            ? cache.get(block.getType(), block.getData())
                            : block;
                    }
                }
            }
            columns.put(ChunkCoordIntPair.chunkXZ2Int(column.chunkX, column.chunkZ), column);
        }

        for (Chunk chunk : neighbours) {
            RegenColumn column = new RegenColumn(chunk.xPosition, chunk.zPosition, false, size);
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y <= maxY; y++) {
                    for (int z = 0; z < 16; z++) {
                        column.blocks[columnIndex(x, y, z)] = cache
                            .get(Block.getIdFromBlock(chunk.getBlock(x, y, z)), chunk.getBlockMetadata(x, y, z));
                    }
                }
            }
            for (Object entry : chunk.chunkTileEntityMap.entrySet()) {
                Map.Entry<?, ?> tile = (Map.Entry<?, ?>) entry;
                ChunkPosition position = (ChunkPosition) tile.getKey();
                column.tiles.put(
                    columnIndex(position.chunkPosX, position.chunkPosY, position.chunkPosZ),
                    (TileEntity) tile.getValue());
            }
            for (List<?> entities : chunk.entityLists) {
                column.entities.addAll(entities);
            }
            columns.put(ChunkCoordIntPair.chunkXZ2Int(column.chunkX, column.chunkZ), column);
        }

        return columns;
    }

    /**
     * Decorate the new chunks. A chunk only gets half its features from its own pass, the rest
     * coming from the chunks at -x and -z, so those run too. Their output outside the selection is
     * undone afterwards, and mod generators only run for selected chunks.
     */
    private static boolean decorate(List<Vector2D> replaced, Map<Long, RegenColumn> columns,
        ChunkProviderServer chunkServer) {
        boolean success = true;
        Set<Long> neighboursDone = new HashSet<Long>();

        for (Vector2D chunk : replaced) {
            for (int[] offset : new int[][] { { 0, 0 }, { -1, 0 }, { 0, -1 }, { -1, -1 } }) {
                int x = chunk.getBlockX() + offset[0];
                int z = chunk.getBlockZ() + offset[1];
                long key = ChunkCoordIntPair.chunkXZ2Int(x, z);
                RegenColumn column = columns.get(key);
                boolean isSelected = column != null && column.selected;

                try {
                    if (isSelected) {
                        // Runs mod generators and marks the chunk populated. It skips chunks a
                        // neighbour's pass has already populated, or that failed to be replaced.
                        chunkServer.populate(chunkServer, x, z);
                    } else if (neighboursDone.add(key)) {
                        // Mod generators and the populated flag are left alone here.
                        chunkServer.currentChunkProvider.populate(chunkServer, x, z);
                    }
                } catch (Throwable t) {
                    logger.log(Level.WARNING, "Failed to decorate chunk " + x + ", " + z, t);
                    success = false;
                }
            }
        }

        return success;
    }

    private void restoreSelectedColumn(RegenColumn column, Region region, EditSession editSession) {
        int maxY = getMaxY();
        int baseX = column.chunkX << 4;
        int baseZ = column.chunkZ << 4;

        for (int x = 0; x < 16; x++) {
            for (int y = 0; y <= maxY; y++) {
                for (int z = 0; z < 16; z++) {
                    Vector pt = new Vector(baseX + x, y, baseZ + z);
                    BaseBlock previous = column.blocks[columnIndex(x, y, z)];
                    BaseBlock current = editSession.getBlock(pt);

                    if (unchanged(previous, current)) {
                        continue;
                    }

                    if (region.contains(pt)) {
                        editSession.rememberChange(pt, previous, current);
                    } else {
                        editSession.smartSetBlock(pt, previous);
                    }
                }
            }
        }
    }

    /**
     * Undo what decoration did to a chunk outside the selection. Only changed blocks are touched, so
     * machines in these chunks are left running.
     */
    private void restoreNeighbourColumn(RegenColumn column, World world, EditSession editSession) {
        Chunk chunk = world.getChunkFromChunkCoords(column.chunkX, column.chunkZ);
        int maxY = getMaxY();
        int baseX = column.chunkX << 4;
        int baseZ = column.chunkZ << 4;

        for (int x = 0; x < 16; x++) {
            for (int y = 0; y <= maxY; y++) {
                for (int z = 0; z < 16; z++) {
                    int index = columnIndex(x, y, z);
                    BaseBlock previous = column.blocks[index];
                    if (previous.getType() == Block.getIdFromBlock(chunk.getBlock(x, y, z))
                        && previous.getData() == chunk.getBlockMetadata(x, y, z)) {
                        continue;
                    }

                    TileEntity tile = column.tiles.get(index);
                    editSession.smartSetBlock(
                        new Vector(baseX + x, y, baseZ + z),
                        tile != null ? new TileEntityBaseBlock(previous.getType(), previous.getData(), tile)
                            : previous);
                }
            }
        }

        for (List<?> entities : chunk.entityLists) {
            for (Object entity : new ArrayList<Object>(entities)) {
                if (!column.entities.contains(entity) && !(entity instanceof EntityPlayer)) {
                    world.removeEntity((net.minecraft.entity.Entity) entity);
                }
            }
        }
    }

    /**
     * Send regenerated chunks to the players watching them. A new chunk object reaches clients
     * through nothing else, so without this they keep showing the old terrain.
     */
    private static void resendChunks(World world, List<Vector2D> chunks) {
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

    /**
     * Take the players out of a chunk that is about to be discarded. Unloading a chunk removes its
     * entities from the world, which for a player would stop them being ticked and tracked.
     */
    private static List<EntityPlayer> detachPlayers(Chunk chunk) {
        List<EntityPlayer> players = new ArrayList<EntityPlayer>();
        for (List<?> entities : chunk.entityLists) {
            for (Object entity : entities) {
                if (entity instanceof EntityPlayer) {
                    players.add((EntityPlayer) entity);
                }
            }
        }

        for (EntityPlayer player : players) {
            chunk.removeEntity(player);
        }
        return players;
    }

    private static int columnIndex(int x, int y, int z) {
        return (y << 8) | (z << 4) | x;
    }

    /**
     * Cheap "nothing to do here" test. Any block carrying NBT on either side counts as changed, so
     * tile entities are always restored or recorded rather than compared.
     */
    private static boolean unchanged(BaseBlock previous, BaseBlock current) {
        return previous.getType() == current.getType() && previous.getData() == current.getData()
            && previous.getNbtData() == null
            && current.getNbtData() == null;
    }

    /**
     * A chunk as it was before regenerating. Blocks without NBT are shared through a
     * {@link BlockCache}, which keeps this to roughly a reference per block.
     */
    private static final class RegenColumn {

        private final int chunkX;
        private final int chunkZ;
        private final boolean selected;
        private final BaseBlock[] blocks;
        private final Map<Integer, TileEntity> tiles = new HashMap<Integer, TileEntity>();
        private final Set<Object> entities = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());

        private RegenColumn(int chunkX, int chunkZ, boolean selected, int size) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.selected = selected;
            this.blocks = new BaseBlock[size];
        }
    }

    private static final class BlockCache {

        private final Map<Long, BaseBlock> blocks = new HashMap<Long, BaseBlock>();
        private long lastKey = -1;
        private BaseBlock last;

        private BaseBlock get(int id, int data) {
            long key = ((long) id << 16) | (data & 0xFFFF);
            if (key != lastKey) {
                BaseBlock block = blocks.get(key);
                if (block == null) {
                    block = new BaseBlock(id, data);
                    blocks.put(key, block);
                }
                lastKey = key;
                last = block;
            }
            return last;
        }
    }

    /**
     * Swap a loaded chunk for freshly generated terrain. The snapshot taken before this loaded the
     * chunk, which also took it off the unload queue.
     */
    @SuppressWarnings("unchecked")
    private static void replaceChunk(ChunkProviderServer server, int chunkX, int chunkZ) {
        long pos = ChunkCoordIntPair.chunkXZ2Int(chunkX, chunkZ);
        List<EntityPlayer> players = new ArrayList<EntityPlayer>();

        Chunk existing = server.loadChunk(chunkX, chunkZ);
        if (existing != null) {
            players = detachPlayers(existing);
            existing.onChunkUnload();
            // Otherwise ChunkProviderServer keeps ticking, saving and unloading the old chunk.
            server.loadedChunks.remove(existing);
        }

        server.loadedChunkHashMap.remove(pos);

        Chunk regenerated = server.currentChunkProvider.provideChunk(chunkX, chunkZ);
        if (regenerated == null) {
            return;
        }

        server.loadedChunkHashMap.add(pos, regenerated);
        server.loadedChunks.add(regenerated);
        regenerated.onChunkLoad();
        for (EntityPlayer player : players) {
            regenerated.addEntity(player);
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
        int id = Block.getIdFromBlock(world.getBlock(position.getBlockX(), position.getBlockY(), position.getBlockZ()));
        int data = world.getBlockMetadata(position.getBlockX(), position.getBlockY(), position.getBlockZ());
        TileEntity tile = getWorld().getTileEntity(position.getBlockX(), position.getBlockY(), position.getBlockZ());

        if (tile != null) {
            return new TileEntityBaseBlock(id, data, tile);
        } else {
            return new BaseBlock(id, data);
        }
    }

    @Override
    public BaseBlock getLazyBlock(Vector position) {
        World world = getWorld();
        int id = Block.getIdFromBlock(world.getBlock(position.getBlockX(), position.getBlockY(), position.getBlockZ()));
        int data = world.getBlockMetadata(position.getBlockX(), position.getBlockY(), position.getBlockZ());
        return new LazyBlock(id, data, this, position);
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
