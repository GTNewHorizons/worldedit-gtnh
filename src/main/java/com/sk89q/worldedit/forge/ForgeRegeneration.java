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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.RegenerationOperation;
import com.sk89q.worldedit.history.change.BlockChange;
import com.sk89q.worldedit.regions.Region;

/**
 * Regenerates chunks in a Forge world.
 *
 * <p>
 * The server ticks between steps, so anything outside the replaced chunks is recorded and restored
 * within a single step. Otherwise whatever machines and players did in between would be reverted too.
 * </p>
 */
final class ForgeRegeneration extends RegenerationOperation {

    private final World world;
    private final ChunkProviderServer server;
    private final Region region;
    private final EditSession editSession;
    private final int maxY;
    private final BlockCache cache = new BlockCache();
    private final Map<Long, RegenColumn> columns = new HashMap<Long, RegenColumn>();

    ForgeRegeneration(World world, ChunkProviderServer server, Region region, EditSession editSession, int maxY) {
        super(region.getChunks());
        this.world = world;
        this.server = server;
        this.region = region;
        this.editSession = editSession;
        this.maxY = maxY;
    }

    @Override
    protected void replaceChunk(int chunkX, int chunkZ) {
        // Load these first, because loading a chunk can populate the ones next to it.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                server.loadChunk(chunkX + dx, chunkZ + dz);
            }
        }

        columns.put(ChunkCoordIntPair.chunkXZ2Int(chunkX, chunkZ), snapshotSelected(chunkX, chunkZ));
        swapChunk(chunkX, chunkZ);
    }

    @Override
    protected void decorateChunk(int chunkX, int chunkZ) {
        List<RegenColumn> around = snapshotAround(chunkX, chunkZ);
        try {
            // Runs mod generators and marks the chunk populated. It skips a chunk that a neighbour
            // being loaded has already populated.
            server.populate(server, chunkX, chunkZ);
        } finally {
            restoreAround(around);
        }
    }

    @Override
    protected void decorateNeighbour(int chunkX, int chunkZ) {
        List<RegenColumn> around = snapshotAround(chunkX, chunkZ);
        try {
            // Mod generators and the populated flag are left alone here.
            server.currentChunkProvider.populate(server, chunkX, chunkZ);
        } finally {
            restoreAround(around);
        }
    }

    @Override
    protected void restoreChunk(int chunkX, int chunkZ) {
        RegenColumn column = columns.remove(ChunkCoordIntPair.chunkXZ2Int(chunkX, chunkZ));
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;

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
                        editSession.getChangeSet()
                            .add(new BlockChange(pt.toBlockVector(), previous, current));
                    } else {
                        editSession.smartSetBlock(pt, previous);
                    }
                }
            }
        }
    }

    @Override
    protected Operation commitEdits() {
        return editSession.commit();
    }

    @Override
    protected void resendChunk(int chunkX, int chunkZ) {
        ForgeWorld.resendChunks(world, Collections.singletonList(new Vector2D(chunkX, chunkZ)));
    }

    private RegenColumn snapshotSelected(int chunkX, int chunkZ) {
        RegenColumn column = new RegenColumn(chunkX, chunkZ, size());
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;

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
        return column;
    }

    /**
     * Record the chunks around a decoration pass that are not being regenerated. A pass writes mostly
     * to its own chunk and the ones at +x and +z, but generators can reach further.
     */
    private List<RegenColumn> snapshotAround(int chunkX, int chunkZ) {
        List<Chunk> chunks = new ArrayList<Chunk>(9);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!isReplaced(chunkX + dx, chunkZ + dz)) {
                    chunks.add(server.loadChunk(chunkX + dx, chunkZ + dz));
                }
            }
        }

        List<RegenColumn> around = new ArrayList<RegenColumn>(chunks.size());
        for (Chunk chunk : chunks) {
            around.add(snapshotNeighbour(chunk));
        }
        return around;
    }

    private RegenColumn snapshotNeighbour(Chunk chunk) {
        RegenColumn column = new RegenColumn(chunk.xPosition, chunk.zPosition, size());
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
        return column;
    }

    /**
     * Undo what decoration did to chunks outside the selection. Only changed blocks are touched, so
     * machines in these chunks are left running.
     */
    private void restoreAround(List<RegenColumn> around) {
        for (RegenColumn column : around) {
            Chunk chunk = world.getChunkFromChunkCoords(column.chunkX, column.chunkZ);
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
    }

    /**
     * Swap a loaded chunk for freshly generated terrain.
     */
    @SuppressWarnings("unchecked")
    private void swapChunk(int chunkX, int chunkZ) {
        long pos = ChunkCoordIntPair.chunkXZ2Int(chunkX, chunkZ);
        List<EntityPlayer> players = new ArrayList<EntityPlayer>();

        // Loading the chunk also took it off the unload queue.
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

    private int size() {
        return 256 * (maxY + 1);
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
     * A chunk as it was before being changed. Blocks without NBT are shared through a
     * {@link BlockCache}, which keeps this to roughly a reference per block.
     */
    private static final class RegenColumn {

        private final int chunkX;
        private final int chunkZ;
        private final BaseBlock[] blocks;
        private final Map<Integer, TileEntity> tiles = new HashMap<Integer, TileEntity>();
        private final Set<Object> entities = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());

        private RegenColumn(int chunkX, int chunkZ, int size) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
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
}
