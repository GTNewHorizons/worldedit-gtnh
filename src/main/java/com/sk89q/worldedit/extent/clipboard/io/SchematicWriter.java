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
import java.util.Map.Entry;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import net.minecraft.block.Block;
import net.minecraft.item.Item;

import com.google.common.base.Predicate;
import com.sk89q.jnbt.ByteArrayTag;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.DoubleTag;
import com.sk89q.jnbt.FloatTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.jnbt.ShortTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.registry.WorldData;

/**
 * Writes schematic files based that are compatible with MCEdit and other editors.
 */
public class SchematicWriter implements ClipboardWriter {

    private static final int MAX_SIZE = Short.MAX_VALUE - Short.MIN_VALUE;
    private final NBTOutputStream outputStream;

    /**
     * Create a new schematic writer.
     *
     * @param outputStream the output stream to write to
     */
    public SchematicWriter(NBTOutputStream outputStream) {
        checkNotNull(outputStream);
        this.outputStream = outputStream;
    }

    @Override
    public void write(Clipboard clipboard, WorldData data) throws IOException {
        Region region = clipboard.getRegion();
        Vector origin = clipboard.getOrigin();
        Vector min = region.getMinimumPoint();
        Vector offset = min.subtract(origin);
        int width = region.getWidth();
        int height = region.getHeight();
        int length = region.getLength();

        if (width > MAX_SIZE) {
            throw new IllegalArgumentException("Width of region too large for a .schematic");
        }
        if (height > MAX_SIZE) {
            throw new IllegalArgumentException("Height of region too large for a .schematic");
        }
        if (length > MAX_SIZE) {
            throw new IllegalArgumentException("Length of region too large for a .schematic");
        }

        // ====================================================================
        // Metadata
        // ====================================================================

        HashMap<String, Tag> schematic = new HashMap<String, Tag>();
        schematic.put("Width", new ShortTag((short) width));
        schematic.put("Length", new ShortTag((short) length));
        schematic.put("Height", new ShortTag((short) height));
        schematic.put("Materials", new StringTag("Alpha"));
        schematic.put("WEOriginX", new IntTag(min.getBlockX()));
        schematic.put("WEOriginY", new IntTag(min.getBlockY()));
        schematic.put("WEOriginZ", new IntTag(min.getBlockZ()));
        schematic.put("WEOffsetX", new IntTag(offset.getBlockX()));
        schematic.put("WEOffsetY", new IntTag(offset.getBlockY()));
        schematic.put("WEOffsetZ", new IntTag(offset.getBlockZ()));
        HashMap<String, Tag> blockMapping = new HashMap<>();
        HashMap<String, Tag> itemMapping = new HashMap<>();

        // ====================================================================
        // Block handling
        // ====================================================================

        byte[] blocks = new byte[width * height * length];
        byte[] addBlocks = null;
        byte[] blockData = new byte[width * height * length];
        byte[] extraBlockData = new byte[width * height * length];
        List<Tag> tileEntities = new ArrayList<Tag>();

        for (Vector point : region) {
            Vector relative = point.subtract(min);
            int x = relative.getBlockX();
            int y = relative.getBlockY();
            int z = relative.getBlockZ();

            int index = y * width * length + z * width + x;
            BaseBlock block = clipboard.getBlock(point);

            // Save 4096 IDs in an AddBlocks section
            if (block.getType() > 255) {
                if (addBlocks == null) { // Lazily create section
                    addBlocks = new byte[(blocks.length >> 1) + 1];
                }

                addBlocks[index
                    >> 1] = (byte) (((index & 1) == 0) ? addBlocks[index >> 1] & 0xF0 | (block.getType() >> 8) & 0xF
                        : addBlocks[index >> 1] & 0xF | ((block.getType() >> 8) & 0xF) << 4);
            }

            blockMapping.put(
                Block.blockRegistry.getNameForObject(Block.getBlockById(block.getId())),
                new ShortTag((short) block.getId()));

            blocks[index] = (byte) block.getType();
            blockData[index] = (byte) block.getData();
            extraBlockData[index] = (byte) (block.getData() >> 8);

            // Store TileEntity data
            CompoundTag rawTag = block.getNbtData();
            if (rawTag != null) {
                Map<String, Tag> values = new HashMap<String, Tag>();
                for (Entry<String, Tag> entry : rawTag.getValue()
                    .entrySet()) {
                    values.put(entry.getKey(), entry.getValue());
                }

                values.put("id", new StringTag(block.getNbtId()));
                values.put("x", new IntTag(x));
                values.put("y", new IntTag(y));
                values.put("z", new IntTag(z));

                CompoundTag tileEntityTag = new CompoundTag(values);
                tileEntities.add(tileEntityTag);

                BiPredicate<CompoundTag, String[]> isItem = (itemTag, idPtr) -> {
                    //Logic below is intentional to sneak a variable assignment into a boolean return statement
                    return ((idPtr[0] = "id")!=null && itemTag.containsKey("id")
                        && itemTag.containsKey("Count")
                        && itemTag.containsKey("Damage"))
                        || ((idPtr[0] = "Item")!=null) && itemTag.containsKey("Item")
                        && itemTag.containsKey("Count")
                        && itemTag.containsKey("Meta");
                };
                Consumer<CompoundTag> convertItems = new Consumer<CompoundTag>() {

                    @Override
                    public void accept(CompoundTag nbtData)
                    {
                        String[] idPtr = new String[1];
                        if (isItem.test(nbtData, idPtr))
                        {
                            short id = nbtData.getShort(idPtr[0]);
                            itemMapping.put(
                                Item.itemRegistry.getNameForObject(Item.getItemById(Short.toUnsignedInt(id))),
                                new ShortTag(id));

                            if (nbtData.containsKey("tag") && nbtData.getValue()
                                .get("tag") instanceof CompoundTag nbt)
                            {
                                accept(nbt);
                            }
                        }
                        else
                        {
                            for (Tag tag : nbtData.getValue().values())
                            {
                                if (tag instanceof ListTag inventoryTag)
                                {
                                    for (Tag tag2 : inventoryTag.getValue())
                                    {
                                        if (tag2 instanceof CompoundTag itemTag)
                                        {
                                            accept(itemTag);
                                        }
                                    }
                                }
                                else if (tag instanceof CompoundTag itemTag)
                                {
                                    accept(itemTag);
                                }
                            }
                        }
                    }
                };
                convertItems.accept(tileEntityTag);
            }
        }

        schematic.put("Blocks", new ByteArrayTag(blocks));
        schematic.put("Data", new ByteArrayTag(blockData));
        schematic.put("ExtraData", new ByteArrayTag(extraBlockData));
        schematic.put("TileEntities", new ListTag(CompoundTag.class, tileEntities));

        if (addBlocks != null) {
            schematic.put("AddBlocks", new ByteArrayTag(addBlocks));
        }

        // ====================================================================
        // Entities
        // ====================================================================

        List<Tag> entities = new ArrayList<Tag>();
        for (Entity entity : clipboard.getEntities()) {
            BaseEntity state = entity.getState();

            if (state != null) {
                Map<String, Tag> values = new HashMap<String, Tag>();

                // Put NBT provided data
                CompoundTag rawTag = state.getNbtData();
                if (rawTag != null) {
                    values.putAll(rawTag.getValue());
                }

                // Store our location data, overwriting any
                values.put("id", new StringTag(state.getTypeId()));
                values.put(
                    "Pos",
                    writeVector(
                        entity.getLocation()
                            .toVector(),
                        "Pos"));
                values.put("Rotation", writeRotation(entity.getLocation(), "Rotation"));

                CompoundTag entityTag = new CompoundTag(values);
                entities.add(entityTag);
            }
        }

        schematic.put("Entities", new ListTag(CompoundTag.class, entities));

        schematic.put("BlockMapping", new CompoundTag(blockMapping));
        schematic.put("ItemMapping", new CompoundTag(itemMapping));
        // ====================================================================
        // Output
        // ====================================================================

        CompoundTag schematicTag = new CompoundTag(schematic);
        outputStream.writeNamedTag("Schematic", schematicTag);
    }

    private Tag writeVector(Vector vector, String name) {
        List<DoubleTag> list = new ArrayList<DoubleTag>();
        list.add(new DoubleTag(vector.getX()));
        list.add(new DoubleTag(vector.getY()));
        list.add(new DoubleTag(vector.getZ()));
        return new ListTag(DoubleTag.class, list);
    }

    private Tag writeRotation(Location location, String name) {
        List<FloatTag> list = new ArrayList<FloatTag>();
        list.add(new FloatTag(location.getYaw()));
        list.add(new FloatTag(location.getPitch()));
        return new ListTag(FloatTag.class, list);
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
    }
}
