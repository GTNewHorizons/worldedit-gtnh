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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
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
 * The server ticks between steps, so anything outside the selection is restored within the step that
 * changed it, from a snapshot taken in that same step. Restoring it any later would revert whatever
 * machines and players did in between, without a way to undo that.
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
    /** For each replaced chunk, the positions in it that are outside the selection. */
    private final Map<Long, int[]> outside = new HashMap<Long, int[]>();

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

        long key = ChunkCoordIntPair.chunkXZ2Int(chunkX, chunkZ);
        RegenColumn column = snapshotSelected(chunkX, chunkZ);
        int[] unselected = unselectedPositions(chunkX, chunkZ);
        columns.put(key, column);
        if (unselected.length > 0) {
            outside.put(key, unselected);
        }

        swapChunk(chunkX, chunkZ);

        // Put back what the selection doesn't cover before the server gets to tick.
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        for (int index : unselected) {
            Vector pt = position(baseX, baseZ, index);
            BaseBlock previous = column.blocks[index];
            if (!unchanged(previous, editSession.getBlock(pt))) {
                editSession.smartSetBlock(pt, previous);
            }
        }
    }

    private int[] unselectedPositions(int chunkX, int chunkZ) {
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        int[] found = new int[size()];
        int count = 0;
        for (int index = 0; index < found.length; index++) {
            if (!region.contains(position(baseX, baseZ, index))) {
                found[count++] = index;
            }
        }
        return Arrays.copyOf(found, count);
    }

    private static Vector position(int baseX, int baseZ, int index) {
        return new Vector(baseX + (index & 15), index >> 8, baseZ + ((index >> 4) & 15));
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
        long key = ChunkCoordIntPair.chunkXZ2Int(chunkX, chunkZ);
        RegenColumn column = columns.remove(key);
        outside.remove(key);
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;

        // Everything outside the selection was already put back, so only history is left.
        for (int index = 0; index < size(); index++) {
            Vector pt = position(baseX, baseZ, index);
            if (!region.contains(pt)) {
                continue;
            }

            BaseBlock previous = column.blocks[index];
            BaseBlock current = editSession.getBlock(pt);
            if (!unchanged(previous, current)) {
                editSession.getChangeSet()
                    .add(new BlockChange(pt.toBlockVector(), previous, current));
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
     * Record what a decoration pass could change outside the selection, as it is right now. A pass
     * writes mostly to its own chunk and the ones at +x and +z, but generators can reach further.
     */
    private List<RegenColumn> snapshotAround(int chunkX, int chunkZ) {
        List<Chunk> chunks = new ArrayList<Chunk>(9);
        List<int[]> limits = new ArrayList<int[]>(9);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int x = chunkX + dx;
                int z = chunkZ + dz;
                if (!isReplaced(x, z)) {
                    chunks.add(server.loadChunk(x, z));
                    limits.add(null);
                } else {
                    // In a replaced chunk, only the part outside the selection needs protecting.
                    int[] unselected = outside.get(ChunkCoordIntPair.chunkXZ2Int(x, z));
                    if (unselected != null) {
                        chunks.add(world.getChunkFromChunkCoords(x, z));
                        limits.add(unselected);
                    }
                }
            }
        }

        List<RegenColumn> around = new ArrayList<RegenColumn>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            around.add(snapshotNeighbour(chunks.get(i), limits.get(i)));
        }
        return around;
    }

    /**
     * @param only the positions to record, or null for the whole chunk along with its entities
     */
    private RegenColumn snapshotNeighbour(Chunk chunk, int[] only) {
        RegenColumn column = new RegenColumn(chunk.xPosition, chunk.zPosition, size());
        column.only = only;
        if (only != null) {
            for (int index : only) {
                column.blocks[index] = cache.get(
                    Block.getIdFromBlock(chunk.getBlock(index & 15, index >> 8, (index >> 4) & 15)),
                    chunk.getBlockMetadata(index & 15, index >> 8, (index >> 4) & 15));
            }
        } else {
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y <= maxY; y++) {
                    for (int z = 0; z < 16; z++) {
                        column.blocks[columnIndex(x, y, z)] = cache
                            .get(Block.getIdFromBlock(chunk.getBlock(x, y, z)), chunk.getBlockMetadata(x, y, z));
                    }
                }
            }
        }
        // Save the data now. Decoration replacing a chest empties the live tile entity on its way out,
        // so saving it only when restoring would bring back an empty chest.
        for (Object entry : chunk.chunkTileEntityMap.entrySet()) {
            Map.Entry<?, ?> tile = (Map.Entry<?, ?>) entry;
            ChunkPosition position = (ChunkPosition) tile.getKey();
            int index = columnIndex(position.chunkPosX, position.chunkPosY, position.chunkPosZ);
            BaseBlock block = column.blocks[index];
            if (block != null) {
                column.blocks[index] = new TileEntityBaseBlock(
                    block.getType(),
                    block.getData(),
                    (TileEntity) tile.getValue());
            }
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

            int count = column.only != null ? column.only.length : size();
            for (int i = 0; i < count; i++) {
                int index = column.only != null ? column.only[i] : i;
                int x = index & 15;
                int y = index >> 8;
                int z = (index >> 4) & 15;
                BaseBlock previous = column.blocks[index];
                if (previous.getType() == Block.getIdFromBlock(chunk.getBlock(x, y, z))
                    && previous.getData() == chunk.getBlockMetadata(x, y, z)) {
                    continue;
                }

                editSession.smartSetBlock(new Vector(baseX + x, y, baseZ + z), previous);
            }

            for (List<?> entities : chunk.entityLists) {
                for (Object entity : new ArrayList<Object>(entities)) {
                    if (column.entities.contains(entity) || entity instanceof EntityPlayer) {
                        continue;
                    }
                    // Mobs that decoration spawns in a regenerated chunk are meant to stay, but items
                    // dropped by blocks that were just put back would be duplicates.
                    if (column.only == null || entity instanceof EntityItem || entity instanceof EntityXPOrb) {
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
        private int[] only;
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
