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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
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

        try {
            int originX = requireTag(schematic, "WEOriginX", IntTag.class).getValue();
            int originY = requireTag(schematic, "WEOriginY", IntTag.class).getValue();
            int originZ = requireTag(schematic, "WEOriginZ", IntTag.class).getValue();
            Vector min = new Vector(originX, originY, originZ);

            int offsetX = requireTag(schematic, "WEOffsetX", IntTag.class).getValue();
            int offsetY = requireTag(schematic, "WEOffsetY", IntTag.class).getValue();
            int offsetZ = requireTag(schematic, "WEOffsetZ", IntTag.class).getValue();
            Vector offset = new Vector(offsetX, offsetY, offsetZ);

            origin = min.subtract(offset);
            region = new CuboidRegion(
                min,
                min.add(width, height, length)
                    .subtract(Vector.ONE));
        } catch (IOException ignored) {
            origin = new Vector(0, 0, 0);
            region = new CuboidRegion(
                origin,
                origin.add(width, height, length)
                    .subtract(Vector.ONE));
        }

        // ====================================================================
        // Blocks
        // ====================================================================

        Map<Short, Short> blockConversionMap = new HashMap<>();
        if (schematic.containsKey("BlockMapping")) {
            Map<String, Tag> mapping = requireTag(schematic, "BlockMapping", CompoundTag.class).getValue();

            for (String key : mapping.keySet()) {
                short sourceId = requireTag(mapping, key, ShortTag.class).getValue();
                Block block = Block.getBlockFromName(key);
                blockConversionMap.put(sourceId, (short) Block.getIdFromBlock(block));
            }
        }

        Map<Short, Short> itemConversionMap = new HashMap<>();
        if (schematic.containsKey("ItemMapping")) {
            Map<String, Tag> mapping = requireTag(schematic, "ItemMapping", CompoundTag.class).getValue();

            for (String key : mapping.keySet()) {
                short sourceId = requireTag(mapping, key, ShortTag.class).getValue();
                Item item = (Item) Item.itemRegistry.getObject(key);
                itemConversionMap.put(sourceId, (short) Item.getIdFromItem(item));
            }
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
        short[] blocks = new short[blockId.length]; // Have to later combine IDs

        // We support 4096 block IDs using the same method as vanilla Minecraft, where
        // the highest 4 bits are stored in a separate byte array.
        if (schematic.containsKey("AddBlocks")) {
            addId = requireTag(schematic, "AddBlocks", ByteArrayTag.class).getValue();
        }

        if (schematic.containsKey("AddBlocks2")) {
            addId2 = requireTag(schematic, "AddBlocks", ByteArrayTag.class).getValue();
        }
        // Combine the AddBlocks data with the first 8-bit block ID
        for (int index = 0; index < blockId.length; index++) {
            if ((index >> 1) >= addId.length) { // No corresponding AddBlocks index
                blocks[index] = (short) (blockId[index] & 0xFF);
            } else {
                if ((index & 1) == 0) {
                    blocks[index] = (short) (((addId[index >> 1] & 0x0F) << 8) + (blockId[index] & 0xFF));
                } else {
                    blocks[index] = (short) (((addId[index >> 1] & 0xF0) << 4) + (blockId[index] & 0xFF));
                }
            }
        }

        for (int index = 0; index < blockId.length; index++) {
            if ((index >> 1) < addId2.length) { // No corresponding AddBlocks2 index
                if ((index & 1) == 0) {
                    blocks[index] = (short) (((addId2[index >> 1] & 0x0F) << 8) + (blocks[index] & 0xFFF));
                } else {
                    blocks[index] = (short) (((addId2[index >> 1] & 0xF0) << 4) + (blocks[index] & 0xFFF));
                }
            }
        }

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

                    if (!blockConversionMap.isEmpty()) {
                        blocks[index] = blockConversionMap.getOrDefault(blocks[index], blocks[index]);
                    }

                    BaseBlock block = new BaseBlock(
                        blocks[index] & 0xFFFF,
                        (blockData[index] & 0xFF) + (addData != null ? (addData[index] & 0xFF << 8) : 0));

                    if (tileEntitiesMap.containsKey(pt)) {
                        BiPredicate<CompoundTag, String[]> isItem = (itemTag, idPtr) -> {
                            // Logic below is intentional to sneak a variable assignment into a boolean return statement
                            return ((idPtr[0] = "id") != null && itemTag.containsKey("id")
                                && itemTag.containsKey("Count")
                                && itemTag.containsKey("Damage"))
                                || ((idPtr[0] = "Item") != null) && itemTag.containsKey("Item")
                                    && itemTag.containsKey("Count")
                                    && itemTag.containsKey("Meta")
                                || ((idPtr[0] = "id") != null) && itemTag.containsKey("id")
                                    && itemTag.getValue()
                                        .get("id") instanceof IntTag;
                        };
                        Function<CompoundTag, CompoundTag> convertItems = new Function<CompoundTag, CompoundTag>() {

                            @Override
                            public CompoundTag apply(CompoundTag nbtData) {
                                String[] idPtr = new String[1];
                                if (isItem.test(nbtData, idPtr)) {
                                    short id;
                                    Integer id_data = null;
                                    if (nbtData.getValue()
                                        .get(idPtr[0]) instanceof IntTag) {
                                        id_data = nbtData.getInt(idPtr[0]);
                                        id = id_data.shortValue();
                                    } else {
                                        id = nbtData.getShort(idPtr[0]);
                                    }
                                    HashMap<String, Tag> itemMap = new HashMap<>(nbtData.getValue());
                                    short newId = itemConversionMap.getOrDefault(id, id);
                                    if (id_data != null) {
                                        itemMap.put(idPtr[0], new IntTag(newId + (id_data & 0xFFFF0000)));
                                    } else {
                                        itemMap.put(idPtr[0], new ShortTag(newId));
                                    }

                                    if (nbtData.containsKey("tag") && itemMap.get("tag") instanceof CompoundTag nbt) {
                                        itemMap.put("tag", apply(nbt));
                                    }

                                    if (nbtData.containsKey("d") && itemMap.get("d") instanceof CompoundTag d) {
                                        itemMap.put("d", apply(d));
                                    }
                                    return nbtData.setValue(itemMap);
                                } else {
                                    if (nbtData.containsKey("id") && nbtData.getValue()
                                        .get("id") instanceof StringTag str
                                        && "customDoorTileEntity".equals(str.getValue())) {

                                        if (nbtData.containsKey("bottomMaterial") && nbtData.getValue()
                                            .get("bottomMaterial") instanceof IntTag itag) {
                                            int _id = itag.getValue();
                                            itemConversionMap.getOrDefault((short) _id, (short) _id);
                                        }

                                        if (nbtData.containsKey("topMaterial") && nbtData.getValue()
                                            .get("topMaterial") instanceof IntTag itag) {
                                            int _id = itag.getValue();
                                            itemConversionMap.getOrDefault((short) _id, (short) _id);
                                        }

                                        if (nbtData.containsKey("frame") && nbtData.getValue()
                                            .get("frame") instanceof IntTag itag) {
                                            int _id = itag.getValue();
                                            itemConversionMap.getOrDefault((short) _id, (short) _id);
                                        }

                                        if (nbtData.containsKey("block") && nbtData.getValue()
                                            .get("block") instanceof IntTag itag) {
                                            int _id = itag.getValue();
                                            blockConversionMap.getOrDefault((short) _id, (short) _id);
                                        }

                                        if (nbtData.containsKey("item") && nbtData.getValue()
                                            .get("item") instanceof IntTag itag) {
                                            int _id = itag.getValue();
                                            itemConversionMap.getOrDefault((short) _id, (short) _id);
                                        }
                                    }

                                    HashMap<String, Tag> nbtMap = new HashMap<>(nbtData.getValue());
                                    for (String key : nbtMap.keySet()) {
                                        {
                                            if (nbtMap.get(key) instanceof ListTag inventoryTag) {
                                                ArrayList<Tag> inventoryList = new ArrayList<>(inventoryTag.getValue());
                                                for (int i = 0; i < inventoryList.size(); i++) {
                                                    if (inventoryList.get(i) instanceof CompoundTag itemTag) {
                                                        inventoryList.set(i, apply(itemTag));
                                                    }
                                                }
                                                nbtMap.put(key, inventoryTag.setValue(inventoryList));
                                            } else if (nbtMap.get(key) instanceof CompoundTag itemTag) {
                                                nbtMap.put(key, apply(itemTag));
                                            }
                                        }
                                    }
                                    return nbtData.setValue(nbtMap);
                                }
                            }
                        };

                        CompoundTag nbtData = new CompoundTag(tileEntitiesMap.get(pt));
                        if (!itemConversionMap.isEmpty()) {
                            nbtData = convertItems.apply(nbtData);
                        }
                        block.setNbtData(nbtData);
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
                        BaseEntity state = new BaseEntity(id, compound);
                        clipboard.createEntity(location, state);
                    }
                }
            }
        } catch (IOException ignored) { // No entities? No problem
        }

        return clipboard;
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
