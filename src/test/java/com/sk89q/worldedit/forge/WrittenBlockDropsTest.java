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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WrittenBlockDropsTest {

    @Test
    public void dropsSpawnedInsideTheBlockAreItsOwn() {
        assertTrue(ForgeWorld.isInsideBlock(10, 64, -5, 10.5, 64.5, -4.5));
        assertTrue(ForgeWorld.isInsideBlock(10, 64, -5, 10.15, 64.85, -4.15));
        assertTrue(ForgeWorld.isInsideBlock(10, 64, -5, 10.0, 65.0, -5.0));
    }

    @Test
    public void aNeighbourPoppingOffKeepsItsDrop() {
        // Vanilla drops land 0.15 to 0.85 into their own block.
        assertFalse(ForgeWorld.isInsideBlock(10, 64, -5, 11.15, 64.5, -4.5));
        assertFalse(ForgeWorld.isInsideBlock(10, 64, -5, 9.85, 64.5, -4.5));
        assertFalse(ForgeWorld.isInsideBlock(10, 64, -5, 10.5, 65.5, -4.5));
        assertFalse(ForgeWorld.isInsideBlock(10, 64, -5, 10.5, 64.5, -5.5));
    }

    @Test
    public void itemsInAChunkLoadedByTheWriteAreLeftAlone() {
        assertFalse(ForgeWorld.isInsideBlock(15, 64, 0, 16.5, 64.5, 0.5));
        assertFalse(ForgeWorld.isInsideBlock(15, 64, 0, 40.0, 70.0, 3.0));
    }
}
