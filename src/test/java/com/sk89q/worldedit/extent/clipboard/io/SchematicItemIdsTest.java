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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.ShortTag;
import com.sk89q.jnbt.Tag;

public class SchematicItemIdsTest {

    private static Map<String, Tag> shortItem(int id) {
        Map<String, Tag> item = new HashMap<>();
        item.put("id", new ShortTag((short) id));
        return item;
    }

    private static Map<String, Tag> extendedItem(int id) {
        Map<String, Tag> item = shortItem(0);
        item.put(SchematicItemIds.EXTENDED_ID, new IntTag(id));
        return item;
    }

    private static Map<Integer, Integer> conversion(int from, int to) {
        Map<Integer, Integer> map = new HashMap<>();
        map.put(from, to);
        return map;
    }

    @Test
    public void readsEndlessIdsExtendedId() {
        assertEquals(40000, SchematicItemIds.read(extendedItem(40000), "id"));
    }

    @Test
    public void readsShortIdUnsigned() {
        assertEquals(40000, SchematicItemIds.read(shortItem(40000), "id"));
    }

    @Test
    public void readsLow16BitsOfPackedIntId() {
        Map<String, Tag> item = new HashMap<>();
        item.put("id", new IntTag((5 << 16) | 1234));
        assertEquals(1234, SchematicItemIds.read(item, "id"));
    }

    @Test
    public void remapsExtendedIdToShortId() {
        Map<String, Tag> item = extendedItem(40000);
        SchematicItemIds.remap(item, "id", conversion(40000, 5000));

        assertEquals(5000, ((ShortTag) item.get("id")).getValue() & 0xFFFF);
        assertFalse(item.containsKey(SchematicItemIds.EXTENDED_ID));
    }

    @Test
    public void remapsShortIdToExtendedIdAbove31999() {
        Map<String, Tag> item = shortItem(5000);
        SchematicItemIds.remap(item, "id", conversion(5000, 40000));

        assertEquals(0, (int) ((ShortTag) item.get("id")).getValue());
        assertEquals(40000, (int) ((IntTag) item.get(SchematicItemIds.EXTENDED_ID)).getValue());
    }

    @Test
    public void remapKeepsPackedMeta() {
        Map<String, Tag> item = new HashMap<>();
        item.put("id", new IntTag((5 << 16) | 1234));
        SchematicItemIds.remap(item, "id", conversion(1234, 40000));

        assertEquals((5 << 16) | 40000, (int) ((IntTag) item.get("id")).getValue());
    }

    @Test
    public void unmappedIdIsUntouched() {
        Map<String, Tag> item = shortItem(40000);
        SchematicItemIds.remap(item, "id", conversion(1, 2));

        assertEquals((short) 40000, (short) ((ShortTag) item.get("id")).getValue());
        assertFalse(item.containsKey(SchematicItemIds.EXTENDED_ID));
    }

    @Test
    public void detectsStacksOfMissingItems() {
        Set<Integer> missing = Collections.singleton(40000);

        Map<String, Tag> stack = extendedItem(40000);
        stack.put("Count", new ShortTag((short) 1));
        stack.put("Damage", new ShortTag((short) 0));
        assertTrue(SchematicItemIds.isMissingStack(stack, missing));

        Map<String, Tag> ae2Style = new HashMap<>();
        ae2Style.put("Item", new ShortTag((short) 40000));
        ae2Style.put("Count", new ShortTag((short) 1));
        ae2Style.put("Meta", new ShortTag((short) 0));
        assertTrue(SchematicItemIds.isMissingStack(ae2Style, missing));

        stack.put(SchematicItemIds.EXTENDED_ID, new IntTag(40001));
        assertFalse(SchematicItemIds.isMissingStack(stack, missing));

        // Not stack-shaped, e.g. a packed GT cover ID
        assertFalse(SchematicItemIds.isMissingStack(extendedItem(40000), missing));
    }

    @Test
    public void mappingSplitsByWidthAndSkipsUnnamed() {
        Map<String, Tag> mapping = new HashMap<>();
        Map<String, Tag> extended = new HashMap<>();

        SchematicItemIds.putMapping(mapping, extended, "a", 40000);
        SchematicItemIds.putMapping(mapping, extended, "b", 70000);
        SchematicItemIds.putMapping(mapping, extended, null, 1);

        assertEquals(40000, ((ShortTag) mapping.get("a")).getValue() & 0xFFFF);
        assertEquals(70000, (int) ((IntTag) extended.get("b")).getValue());
        assertEquals(1, mapping.size());
        assertTrue(extended.containsKey("b"));
    }
}
