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

package com.sk89q.worldedit.function.generator;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.pattern.BlockPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.RandomPattern;

/**
 * Generates flora (which may include tall grass, flowers, etc.).
 *
 * <p>
 * The current implementation is not biome-aware, but it may become so in
 * the future.
 * </p>
 */
public class FloraGenerator implements RegionFunction {

    private final EditSession editSession;
    private boolean biomeAware = false;
    private final Pattern desertPattern = getDesertPattern();
    private final Pattern temperatePattern = getTemperatePattern();

    /**
     * Create a new flora generator.
     *
     * @param editSession the edit session
     */
    public FloraGenerator(EditSession editSession) {
        this.editSession = editSession;
    }

    /**
     * Return whether the flora generator is set to be biome-aware.
     *
     * <p>
     * By default, it is currently disabled by default, but
     * this may change.
     * </p>
     *
     * @return true if biome aware
     */
    public boolean isBiomeAware() {
        return biomeAware;
    }

    /**
     * Set whether the generator is biome aware.
     *
     * <p>
     * It is currently not possible to make the generator biome-aware.
     * </p>
     *
     * @param biomeAware must always be false
     */
    public void setBiomeAware(boolean biomeAware) {
        if (biomeAware) {
            throw new IllegalArgumentException("Cannot enable biome-aware mode; not yet implemented");
        }
    }

    /**
     * Get a pattern for plants to place inside a desert environment.
     *
     * @return a pattern that places flora
     */
    public static Pattern getDesertPattern() {
        RandomPattern pattern = new RandomPattern();
        pattern.add(new BlockPattern(new BaseBlock(BlockID.DEAD_BUSH)), 30);
        pattern.add(new BlockPattern(new BaseBlock(BlockID.CACTUS)), 20);
        pattern.add(new BlockPattern(new BaseBlock(BlockID.AIR)), 300);
        return pattern;
    }

    /**
     * Get a pattern for plants to place inside a temperate environment.
     *
     * @return a pattern that places flora
     */
    public static Pattern getTemperatePattern() {
        RandomPattern pattern = new RandomPattern();
        pattern.add(new BlockPattern(new BaseBlock(BlockID.LONG_GRASS, 1)), 300);
        pattern.add(new BlockPattern(new BaseBlock(BlockID.RED_FLOWER)), 5);
        pattern.add(new BlockPattern(new BaseBlock(BlockID.YELLOW_FLOWER)), 5);
        return pattern;
    }

    @Override
    public boolean apply(Vector position) throws WorldEditException {
        BaseBlock block = editSession.getBlock(position);

        if (block.getType() == BlockID.GRASS) {
            editSession.setBlock(position.add(0, 1, 0), temperatePattern.apply(position));
            return true;
        } else if (block.getType() == BlockID.SAND) {
            editSession.setBlock(position.add(0, 1, 0), desertPattern.apply(position));
            return true;
        }

        return false;
    }

}
