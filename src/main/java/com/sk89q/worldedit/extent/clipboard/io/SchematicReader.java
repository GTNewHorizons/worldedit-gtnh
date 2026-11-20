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

package com.sk89q.worldedit.extent.clipboard.io;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.item.Item;

import com.sk89q.jnbt.ByteArrayTag;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NamedTag;
import com.sk89q.jnbt.ShortTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.registry.WorldData;
import com.sk89q.worldedit.world.storage.NBTConversions;

/**
 * Reads schematic files based that are compatible with MCEdit and other editors.
 */
public class SchematicReader implements ClipboardReader {

    private static final Logger log = Logger.getLogger(SchematicReader.class.getCanonicalName());
    private final NBTInputStream inputStream;
    private final Set<String> missingNames = new TreeSet<>();

    /**
     * Create a new instance.
     *
     * @param inputStream the input stream to read from
     */
    public SchematicReader(NBTInputStream inputStream) {
        checkNotNull(inputStream);
        this.inputStream = inputStream;
    }

    @Override
    public Collection<String> getMissingNames() {
        return Collections.unmodifiableSet(missingNames);
    }

    @Override
    public Clipboard read(WorldData data) throws IOException {
        // Schematic tag
        NamedTag rootTag = inputStream.readNamedTag();
        if (!rootTag.getName()
            .equals("Schematic")) {
            throw new IOException("Tag 'Schematic' does not exist or is not first");
        }
        CompoundTag schematicTag = (CompoundTag) rootTag.getTag();

        // Check
        Map<String, Tag> schematic = schematicTag.getValue();
        if (!schematic.containsKey("Blocks")) {
            throw new IOException("Schematic file is missing a 'Blocks' tag");
        }

        // Check type of Schematic
        String materials = requireTag(schematic, "Materials", StringTag.class).getValue();
        if (!materials.equals("Alpha")) {
            throw new IOException("Schematic file is not an Alpha schematic");
        }

        // ====================================================================
        // Metadata
        // ====================================================================

        Vector origin;
        Region region;

        // Get information
        short width = requireTag(schematic, "Width", ShortTag.class).getValue();
        short height = requireTag(schematic, "Height", ShortTag.class).getValue();
        short length = requireTag(schematic, "Length", ShortTag.class).getValue();

        // Read WEOrigin and WEOffset independently so the paste anchor is
        // honoured even when only WEOffset is present (e.g. converted schems).
        Vector min;
        try {
            int originX = requireTag(schematic, "WEOriginX", IntTag.class).getValue();
            int originY = requireTag(schematic, "WEOriginY", IntTag.class).getValue();
            int originZ = requireTag(schematic, "WEOriginZ", IntTag.class).getValue();
            min = new Vector(originX, originY, originZ);
        } catch (IOException ignored) {
            min = new Vector(0, 0, 0);
        }

        Vector offset;
        try {
            int offsetX = requireTag(schematic, "WEOffsetX", IntTag.class).getValue();
            int offsetY = requireTag(schematic, "WEOffsetY", IntTag.class).getValue();
            int offsetZ = requireTag(schematic, "WEOffsetZ", IntTag.class).getValue();
            offset = new Vector(offsetX, offsetY, offsetZ);
        } catch (IOException ignored) {
            offset = new Vector(0, 0, 0);
        }

        origin = min.subtract(offset);
        region = new CuboidRegion(
            min,
            min.add(width, height, length)
                .subtract(Vector.ONE));

        // ====================================================================
        // Blocks
        // ====================================================================

        missingNames.clear();

        // Blocks missing from this game become air
        Map<Integer, Integer> blockConversionMap = new HashMap<>();
        Set<Integer> missingBlockIds = new HashSet<>();
        for (Map.Entry<String, Integer> entry : readMapping(schematic, "BlockMapping", "BlockMappingE").entrySet()) {
            Block block = Block.getBlockFromName(entry.getKey());
            if (block == null) {
                missingNames.add(entry.getKey());
                missingBlockIds.add(entry.getValue());
                blockConversionMap.put(entry.getValue(), 0);
            } else {
                blockConversionMap.put(entry.getValue(), Block.getIdFromBlock(block));
            }
        }

        // Items missing from this game become ID 0, and their stacks are removed from inventories
        Map<Integer, Integer> itemConversionMap = new HashMap<>();
        Set<Integer> missingItemIds = new HashSet<>();
        for (Map.Entry<String, Integer> entry : readMapping(schematic, "ItemMapping", "ItemMappingE").entrySet()) {
            Item item = (Item) Item.itemRegistry.getObject(entry.getKey());
            if (item == null) {
                missingNames.add(entry.getKey());
                missingItemIds.add(entry.getValue());
            }
            itemConversionMap.put(entry.getValue(), Item.getIdFromItem(item));
        }

        // Get blocks
        byte[] blockId = requireTag(schematic, "Blocks", ByteArrayTag.class).getValue();
        byte[] blockData = requireTag(schematic, "Data", ByteArrayTag.class).getValue();
        byte[] addData = null;
        if (schematic.containsKey("AddData")) {
            addData = requireTag(schematic, "AddData", ByteArrayTag.class).getValue();
        }

        byte[] addId = new byte[0];
        byte[] addId2 = new byte[0];
        byte[] addId3 = new byte[0];

        // We support 4096 block IDs using the same method as vanilla Minecraft, where
        // the highest 4 bits are stored in a separate byte array.
        if (schematic.containsKey("AddBlocks")) {
            addId = requireTag(schematic, "AddBlocks", ByteArrayTag.class).getValue();
        }

        if (schematic.containsKey("AddBlocks2")) {
            addId2 = requireTag(schematic, "AddBlocks2", ByteArrayTag.class).getValue();
        }

        if (schematic.containsKey("AddBlocks3")) {
            addId3 = requireTag(schematic, "AddBlocks3", ByteArrayTag.class).getValue();
        }

        int[] blocks = combineBlockIds(blockId, addId, addId2, addId3);

        // Need to pull out tile entities
        List<Tag> tileEntities = requireTag(schematic, "TileEntities", ListTag.class).getValue();
        Map<BlockVector, Map<String, Tag>> tileEntitiesMap = new HashMap<BlockVector, Map<String, Tag>>();

        for (Tag tag : tileEntities) {
            if (!(tag instanceof CompoundTag)) continue;
            CompoundTag t = (CompoundTag) tag;

            int x = 0;
            int y = 0;
            int z = 0;

            Map<String, Tag> values = new HashMap<String, Tag>();

            for (Map.Entry<String, Tag> entry : t.getValue()
                .entrySet()) {
                if (entry.getKey()
                    .equals("x")) {
                    if (entry.getValue() instanceof IntTag) {
                        x = ((IntTag) entry.getValue()).getValue();
                    }
                } else if (entry.getKey()
                    .equals("y")) {
                        if (entry.getValue() instanceof IntTag) {
                            y = ((IntTag) entry.getValue()).getValue();
                        }
                    } else if (entry.getKey()
                        .equals("z")) {
                            if (entry.getValue() instanceof IntTag) {
                                z = ((IntTag) entry.getValue()).getValue();
                            }
                        }

                values.put(entry.getKey(), entry.getValue());
            }

            BlockVector vec = new BlockVector(x, y, z);
            tileEntitiesMap.put(vec, values);
        }

        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        clipboard.setOrigin(origin);

        // Don't log a torrent of errors
        int failedBlockSets = 0;

        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                for (int z = 0; z < length; ++z) {
                    int index = y * width * length + z * width + x;
                    BlockVector pt = new BlockVector(x, y, z);

                    BaseBlock block;
                    if (missingBlockIds.contains(blocks[index])) {
                        // Plain air: the source data and tile entity may not be valid here
                        block = new BaseBlock(0);
                    } else {
                        if (!blockConversionMap.isEmpty()) {
                            blocks[index] = blockConversionMap.getOrDefault(blocks[index], blocks[index]);
                        }

                        block = new BaseBlock(
                            blocks[index],
                            (blockData[index] & 0xFF) + (addData != null ? ((addData[index] & 0xFF) << 8) : 0));

                        if (tileEntitiesMap.containsKey(pt)) {
                            CompoundTag nbtData = new CompoundTag(tileEntitiesMap.get(pt));
                            if (!itemConversionMap.isEmpty()) {
                                nbtData = SchematicItemIds
                                    .remapAll(nbtData, itemConversionMap, blockConversionMap, missingItemIds);
                            }
                            block.setNbtData(nbtData);
                        }
                    }

                    try {
                        clipboard.setBlock(
                            region.getMinimumPoint()
                                .add(pt),
                            block);
                    } catch (WorldEditException e) {
                        switch (failedBlockSets) {
                            case 0:
                                log.log(Level.WARNING, "Failed to set block on a Clipboard", e);
                                break;
                            case 1:
                                log.log(
                                    Level.WARNING,
                                    "Failed to set block on a Clipboard (again) -- no more messages will be logged",
                                    e);
                                break;
                            default:
                                // no-op
                        }

                        failedBlockSets++;
                    }
                }
            }
        }

        // ====================================================================
        // Entities
        // ====================================================================

        try {
            List<Tag> entityTags = requireTag(schematic, "Entities", ListTag.class).getValue();

            for (Tag tag : entityTags) {
                if (tag instanceof CompoundTag) {
                    CompoundTag compound = (CompoundTag) tag;
                    String id = compound.getString("id");
                    Location location = NBTConversions
                        .toLocation(clipboard, compound.getListTag("Pos"), compound.getListTag("Rotation"));

                    if (!id.isEmpty()) {
                        if (!itemConversionMap.isEmpty()) {
                            compound = SchematicItemIds
                                .remapAll(compound, itemConversionMap, blockConversionMap, missingItemIds);
                        }
                        BaseEntity state = new BaseEntity(id, compound);
                        clipboard.createEntity(location, state);
                    }
                }
            }
        } catch (IOException ignored) { // No entities? No problem
        }

        return clipboard;
    }

    /**
     * Combine the block ID sections. AddBlocks and AddBlocks2 hold one nibble per block (bits 8-11 and
     * 12-15), AddBlocks3 one byte per block (bits 16-23, EndlessIDs).
     */
    static int[] combineBlockIds(byte[] blockId, @Nullable byte[] addId, @Nullable byte[] addId2,
        @Nullable byte[] addId3) {
        byte[] addIdSafe = addId != null ? addId : new byte[0];
        byte[] addId2Safe = addId2 != null ? addId2 : new byte[0];
        byte[] addId3Safe = addId3 != null ? addId3 : new byte[0];

        int[] blocks = new int[blockId.length];

        for (int index = 0; index < blockId.length; index++) {
            int id = blockId[index] & 0xFF;
            int half = index >> 1;
            int shift = (index & 1) == 0 ? 0 : 4;

            if (half < addIdSafe.length) {
                id |= ((addIdSafe[half] >> shift) & 0xF) << 8;
            }
            if (half < addId2Safe.length) {
                id |= ((addId2Safe[half] >> shift) & 0xF) << 12;
            }
            if (index < addId3Safe.length) {
                id |= (addId3Safe[index] & 0xFF) << 16;
            }

            blocks[index] = id;
        }

        return blocks;
    }

    /**
     * Read a registry name to source ID mapping from the short tag (IDs up to 65535, read unsigned) and
     * the int tag for larger IDs.
     */
    private static Map<String, Integer> readMapping(Map<String, Tag> schematic, String shortKey, String intKey)
        throws IOException {
        Map<String, Integer> result = new HashMap<>();
        if (schematic.containsKey(shortKey)) {
            Map<String, Tag> mapping = requireTag(schematic, shortKey, CompoundTag.class).getValue();
            for (String key : mapping.keySet()) {
                result.put(key, Short.toUnsignedInt(requireTag(mapping, key, ShortTag.class).getValue()));
            }
        }
        if (schematic.containsKey(intKey)) {
            Map<String, Tag> mapping = requireTag(schematic, intKey, CompoundTag.class).getValue();
            for (String key : mapping.keySet()) {
                result.put(key, requireTag(mapping, key, IntTag.class).getValue());
            }
        }
        return result;
    }

    private static <T extends Tag> T requireTag(Map<String, Tag> items, String key, Class<T> expected)
        throws IOException {
        if (!items.containsKey(key)) {
            throw new IOException("Schematic file is missing a \"" + key + "\" tag");
        }

        Tag tag = items.get(key);
        if (!expected.isInstance(tag)) {
            throw new IOException(key + " tag is not of tag type " + expected.getName());
        }

        return expected.cast(tag);
    }

    @Nullable
    private static <T extends Tag> T getTag(CompoundTag tag, Class<T> expected, String key) {
        Map<String, Tag> items = tag.getValue();

        if (!items.containsKey(key)) {
            return null;
        }

        Tag test = items.get(key);
        if (!expected.isInstance(test)) {
            return null;
        }

        return expected.cast(test);
    }

}
