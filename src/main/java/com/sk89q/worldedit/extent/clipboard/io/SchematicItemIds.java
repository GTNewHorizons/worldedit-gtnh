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

import java.util.Map;

import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.ShortTag;
import com.sk89q.jnbt.Tag;

/**
 * Reads and rewrites item IDs stored in tile entity NBT.
 *
 * <p>
 * EndlessIDs saves item stacks with an ID above {@link #MAX_SHORT_ID} as {@code id: 0s} plus
 * {@code idExt: <int>}. An int {@code id} tag (e.g. GregTech) packs the ID in the low 16 bits and the
 * meta in the high 16 bits.
 */
final class SchematicItemIds {

    static final String EXTENDED_ID = "idExt";
    /** Highest item ID EndlessIDs still writes to the short {@code id} tag. */
    static final int MAX_SHORT_ID = 31999;
    static final int NO_ID = -1;

    private SchematicItemIds() {}

    /**
     * @return the item ID, or {@link #NO_ID} if the tag holds none
     */
    static int read(Map<String, Tag> item, String idKey) {
        if ("id".equals(idKey) && item.get(EXTENDED_ID) instanceof IntTag ext) {
            return ext.getValue();
        }
        Tag tag = item.get(idKey);
        if (tag instanceof IntTag packed) {
            return packed.getValue() & 0xFFFF;
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
            if (newId <= 0xFFFF) {
                item.put(idKey, new IntTag((packed.getValue() & 0xFFFF0000) | (newId & 0xFFFF)));
            }
        } else if (tag instanceof ShortTag && newId <= 0xFFFF) {
            item.put(idKey, new ShortTag((short) newId));
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
