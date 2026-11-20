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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.ShortTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;

/**
 * Finds, reads and rewrites block and item IDs stored in tile entity and entity NBT.
 *
 * <p>
 * Item stacks are {@code id/Count/Damage} or {@code Item/Count/Meta}. EndlessIDs saves a stack with an ID
 * above {@link #MAX_SHORT_ID} as {@code id: 0s} plus {@code idExt: <int>}, and also accepts a plain int
 * {@code id}. Any other compound with an int {@code id} (e.g. GregTech covers) packs the ID in the low 16
 * bits and the meta in the high 16 bits.
 */
final class SchematicItemIds {

    static final String EXTENDED_ID = "idExt";
    /** Highest item ID EndlessIDs still writes to the short {@code id} tag. */
    static final int MAX_SHORT_ID = 31999;
    static final int NO_ID = -1;

    private static final String[] DOOR_ITEM_KEYS = { "bottomMaterial", "topMaterial", "frame", "item" };
    private static final String DOOR_BLOCK_KEY = "block";
    /** Item compounds that hold further items. */
    private static final String[] ITEM_CHILD_KEYS = { "tag", "d" };

    private SchematicItemIds() {}

    static boolean isStack(Map<String, Tag> tag) {
        return tag.containsKey("Count") && (tag.containsKey("id") && tag.containsKey("Damage")
            || tag.containsKey("Item") && tag.containsKey("Meta"));
    }

    /**
     * @return the key holding the item ID, or null if the tag is not an item
     */
    static String itemIdKey(Map<String, Tag> tag) {
        if (isStack(tag)) {
            return tag.containsKey("id") && tag.containsKey("Damage") ? "id" : "Item";
        }
        return tag.get("id") instanceof IntTag ? "id" : null;
    }

    /**
     * @return the item ID, or {@link #NO_ID} if the tag holds none
     */
    static int read(Map<String, Tag> item, String idKey) {
        if ("id".equals(idKey) && item.get(EXTENDED_ID) instanceof IntTag ext) {
            return ext.getValue();
        }
        Tag tag = item.get(idKey);
        if (tag instanceof IntTag intId) {
            return isStack(item) ? intId.getValue() : intId.getValue() & 0xFFFF;
        }
        if (tag instanceof ShortTag id) {
            return Short.toUnsignedInt(id.getValue());
        }
        return NO_ID;
    }

    /**
     * Rewrite the item ID in place using the conversion map. IDs without a mapping are left untouched.
     */
    static void remap(Map<String, Tag> item, String idKey, Map<Integer, Integer> conversion) {
        int id = read(item, idKey);
        if (id == NO_ID) {
            return;
        }
        Integer mapped = conversion.get(id);
        if (mapped == null || mapped == id) {
            return;
        }
        int newId = mapped;

        Tag tag = item.get(idKey);
        if ("id".equals(idKey) && (tag instanceof ShortTag || item.containsKey(EXTENDED_ID))) {
            if (newId > MAX_SHORT_ID) {
                item.put("id", new ShortTag((short) 0));
                item.put(EXTENDED_ID, new IntTag(newId));
            } else {
                item.put("id", new ShortTag((short) newId));
                item.remove(EXTENDED_ID);
            }
        } else if (tag instanceof IntTag packed) {
            if (isStack(item)) {
                item.put(idKey, new IntTag(newId));
            } else if (newId <= 0xFFFF) {
                item.put(idKey, new IntTag((packed.getValue() & 0xFFFF0000) | (newId & 0xFFFF)));
            }
        } else if (tag instanceof ShortTag && newId <= 0xFFFF) {
            item.put(idKey, new ShortTag((short) newId));
        }
    }

    /**
     * @return whether the tag is an item stack whose source ID is in {@code missingIds}
     */
    static boolean isMissingStack(Map<String, Tag> tag, Set<Integer> missingIds) {
        if (missingIds.isEmpty() || !isStack(tag)) {
            return false;
        }
        return missingIds.contains(read(tag, itemIdKey(tag)));
    }

    /**
     * Rewrite every block and item ID in the tag. A stack of a missing item is removed only from a list
     * whose entries carry a {@code Slot}, i.e. an inventory. Elsewhere list position can matter (e.g. AE2
     * pattern inputs), so the entry stays and its ID becomes whatever the conversion map says.
     */
    static CompoundTag remapAll(CompoundTag tag, Map<Integer, Integer> items, Map<Integer, Integer> blocks,
        Set<Integer> missingItems) {
        Map<String, Tag> map = new HashMap<>(tag.getValue());

        String idKey = itemIdKey(map);
        if (idKey != null) {
            remap(map, idKey, items);
            for (String key : ITEM_CHILD_KEYS) {
                if (map.get(key) instanceof CompoundTag child) {
                    map.put(key, remapAll(child, items, blocks, missingItems));
                }
            }
            return tag.setValue(map);
        }

        if (isDoor(map)) {
            for (String key : DOOR_ITEM_KEYS) {
                remapInt(map, key, items);
            }
            remapInt(map, DOOR_BLOCK_KEY, blocks);
        }

        for (Map.Entry<String, Tag> entry : map.entrySet()) {
            if (entry.getValue() instanceof ListTag list) {
                List<Tag> entries = new ArrayList<>();
                for (Tag element : list.getValue()) {
                    if (!(element instanceof CompoundTag compound)) {
                        entries.add(element);
                    } else if (!(compound.containsKey("Slot") && isMissingStack(compound.getValue(), missingItems))) {
                        entries.add(remapAll(compound, items, blocks, missingItems));
                    }
                }
                entry.setValue(list.setValue(entries));
            } else if (entry.getValue() instanceof CompoundTag child) {
                entry.setValue(remapAll(child, items, blocks, missingItems));
            }
        }
        return tag.setValue(map);
    }

    /**
     * Report every block and item ID in the tag, walking it the same way as
     * {@link #remapAll(CompoundTag, Map, Map, Set)}.
     */
    static void collect(CompoundTag tag, IntConsumer items, IntConsumer blocks) {
        Map<String, Tag> map = tag.getValue();

        String idKey = itemIdKey(map);
        if (idKey != null) {
            int id = read(map, idKey);
            if (id != NO_ID) {
                items.accept(id);
            }
            for (String key : ITEM_CHILD_KEYS) {
                if (map.get(key) instanceof CompoundTag child) {
                    collect(child, items, blocks);
                }
            }
            return;
        }

        if (isDoor(map)) {
            for (String key : DOOR_ITEM_KEYS) {
                if (map.get(key) instanceof IntTag id) {
                    items.accept(id.getValue());
                }
            }
            if (map.get(DOOR_BLOCK_KEY) instanceof IntTag id) {
                blocks.accept(id.getValue());
            }
        }

        for (Tag value : map.values()) {
            if (value instanceof ListTag list) {
                for (Tag element : list.getValue()) {
                    if (element instanceof CompoundTag compound) {
                        collect(compound, items, blocks);
                    }
                }
            } else if (value instanceof CompoundTag child) {
                collect(child, items, blocks);
            }
        }
    }

    private static boolean isDoor(Map<String, Tag> tag) {
        return tag.get("id") instanceof StringTag id && "customDoorTileEntity".equals(id.getValue());
    }

    private static void remapInt(Map<String, Tag> tag, String key, Map<Integer, Integer> conversion) {
        if (tag.get(key) instanceof IntTag id) {
            tag.put(key, new IntTag(conversion.getOrDefault(id.getValue(), id.getValue())));
        }
    }

    /**
     * Record a registry name for an ID, using the int mapping for IDs that don't fit in a short. Some tile
     * entities store numbers that look like IDs but aren't, or name items that are no longer registered,
     * so IDs without a name are left out.
     */
    static void putMapping(Map<String, Tag> mapping, Map<String, Tag> extendedMapping, String name, int id) {
        if (name == null) {
            return;
        }
        if (id > 0xFFFF) {
            extendedMapping.put(name, new IntTag(id));
        } else {
            mapping.put(name, new ShortTag((short) id));
        }
    }
}
