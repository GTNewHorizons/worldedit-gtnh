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

import org.junit.Test;

public class SchematicReaderTest {

    @Test
    public void readsAddBlocks2HighBits() {
        short blockId = 15122; // 0x3B12

        byte[] blocks = new byte[] { (byte) (blockId & 0xFF) };
        byte[] addBlocks = new byte[] { (byte) ((blockId >> 8) & 0x0F) };
        byte[] addBlocks2 = new byte[] { (byte) ((blockId >> 12) & 0x0F) };

        short[] combined = SchematicReader.combineBlockIds(blocks, addBlocks, addBlocks2);

        assertEquals(blockId, combined[0]);
    }
}
