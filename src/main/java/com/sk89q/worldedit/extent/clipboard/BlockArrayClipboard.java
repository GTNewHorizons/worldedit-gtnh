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

package com.sk89q.worldedit.extent.clipboard;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BaseBiome;

/**
 * Stores clipboard blocks in lazily allocated, paletted sections of up to 16 blocks on each axis.
 */
public class BlockArrayClipboard implements Clipboard {

    private final Region region;
    private final int xBits;
    private final int yBits;
    private final int zBits;
    private Vector origin;
    private final Map<BlockVector, ClipboardSection> sections = new HashMap<>();
    private ClipboardSection lastSection;
    private final List<ClipboardEntity> entities = new ArrayList<ClipboardEntity>();

    /**
     * Create a new instance.
     *
     * <p>
     * The origin will be placed at the region's lowest minimum point.
     * </p>
     *
     * @param region the bounding region
     */
    public BlockArrayClipboard(Region region) {
        checkNotNull(region);
        this.region = region.clone();
        this.origin = region.getMinimumPoint();
        Vector dimensions = this.region.getMaximumPoint()
            .subtract(this.region.getMinimumPoint())
            .add(1, 1, 1);
        xBits = sectionBits(dimensions.getX());
        yBits = sectionBits(dimensions.getY());
        zBits = sectionBits(dimensions.getZ());
    }

    // Short axes use a smaller power of two. A one-block-wide wire needs only 16 slots per section.
    private static int sectionBits(double dimension) {
        int length = Math.max(1, Math.min(16, (int) Math.ceil(dimension)));
        return Integer.SIZE - Integer.numberOfLeadingZeros(length - 1);
    }

    @Override
    public Region getRegion() {
        return region.clone();
    }

    @Override
    public Vector getOrigin() {
        return origin;
    }

    @Override
    public void setOrigin(Vector origin) {
        this.origin = origin;
    }

    @Override
    public Vector getDimensions() {
        return region.getMaximumPoint()
            .subtract(region.getMinimumPoint())
            .add(1, 1, 1);
    }

    @Override
    public Vector getMinimumPoint() {
        return region.getMinimumPoint();
    }

    @Override
    public Vector getMaximumPoint() {
        return region.getMaximumPoint();
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        List<Entity> filtered = new ArrayList<Entity>();
        for (Entity entity : entities) {
            if (region.contains(
                entity.getLocation()
                    .toVector())) {
                filtered.add(entity);
            }
        }
        return Collections.unmodifiableList(filtered);
    }

    @Override
    public List<? extends Entity> getEntities() {
        return Collections.unmodifiableList(entities);
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        ClipboardEntity ret = new ClipboardEntity(location, entity);
        entities.add(ret);
        return ret;
    }

    @Override
    public BaseBlock getBlock(Vector position) {
        if (!region.contains(position) || sections.isEmpty()) {
            return new BaseBlock(BlockID.AIR);
        }

        Vector minimum = region.getMinimumPoint();
        int x = (int) Math.round(position.getX() - minimum.getX());
        int y = (int) Math.round(position.getY() - minimum.getY());
        int z = (int) Math.round(position.getZ() - minimum.getZ());
        ClipboardSection section = sectionAt(x >> xBits, y >> yBits, z >> zBits);
        int index = localIndex(x, y, z);
        long state = section.stateAt(index);
        return new BaseBlock((int) (state >>> 32), (int) state, section.tagAt(index));
    }

    @Override
    public BaseBlock getLazyBlock(Vector position) {
        return getBlock(position);
    }

    @Override
    public boolean setBlock(Vector position, BaseBlock block) throws WorldEditException {
        if (!region.contains(position)) {
            return false;
        }

        Vector minimum = region.getMinimumPoint();
        int x = (int) Math.round(position.getX() - minimum.getX());
        int y = (int) Math.round(position.getY() - minimum.getY());
        int z = (int) Math.round(position.getZ() - minimum.getZ());
        long state = ((long) block.getId() << 32) | (block.getData() & 0xffffffffL);
        CompoundTag tag = block.getNbtData();
        if (sections.isEmpty() && state == 0 && tag == null) {
            return true;
        }
        ClipboardSection section = sectionAt(x >> xBits, y >> yBits, z >> zBits);
        boolean wasEmpty = section.isEmpty();
        int index = localIndex(x, y, z);
        section.set(index, state, tag);
        if (section.isEmpty() != wasEmpty) {
            if (wasEmpty) {
                sections.put(section.key, section);
            } else {
                sections.remove(section.key);
            }
        }
        return true;
    }

    private int localIndex(int x, int y, int z) {
        int localX = x & ((1 << xBits) - 1);
        int localY = y & ((1 << yBits) - 1);
        int localZ = z & ((1 << zBits) - 1);
        return (localY << (zBits + xBits)) | (localZ << xBits) | localX;
    }

    private ClipboardSection sectionAt(int x, int y, int z) {
        ClipboardSection section = lastSection;
        if (section != null && section.key.getBlockX() == x
            && section.key.getBlockY() == y
            && section.key.getBlockZ() == z) {
            return section;
        }
        BlockVector key = new BlockVector(x, y, z);
        section = sections.get(key);
        if (section == null) {
            // Cache misses as well: visiting a run of air in a sparse schematic needs just one lookup.
            section = new ClipboardSection(key, 1 << (xBits + yBits + zBits));
        }
        lastSection = section;
        return section;
    }

    @Override
    public BaseBiome getBiome(Vector2D position) {
        return new BaseBiome(0);
    }

    @Override
    public boolean setBiome(Vector2D position, BaseBiome biome) {
        return false;
    }

    @Nullable
    @Override
    public Operation commit() {
        return null;
    }

    /**
     * Stores entity data.
     */
    private class ClipboardEntity extends StoredEntity {

        ClipboardEntity(Location location, BaseEntity entity) {
            super(location, entity);
        }

        @Override
        public boolean remove() {
            return entities.remove(this);
        }

        @Nullable
        @Override
        public <T> T getFacet(Class<? extends T> cls) {
            return null;
        }
    }

}
