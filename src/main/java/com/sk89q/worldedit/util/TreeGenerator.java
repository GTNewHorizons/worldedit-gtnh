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

package com.sk89q.worldedit.util;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.Sets;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;

/**
 * Tree generator.
 */
public class TreeGenerator {

    public enum TreeType {

        TREE("Oak tree", "oak", "tree", "regular"),
        BIG_TREE("Large oak tree", "largeoak", "bigoak", "big", "bigtree"),
        REDWOOD("Spruce tree", "spruce", "redwood", "sequoia", "sequoioideae"),
        TALL_REDWOOD("Tall spruce tree", "tallspruce", "bigspruce", "tallredwood", "tallsequoia", "tallsequoioideae"),
        MEGA_REDWOOD("Large spruce tree", "largespruce", "megaredwood"),
        RANDOM_REDWOOD("Random spruce tree", "randspruce", "randredwood", "randomredwood", "anyredwood") {

            @Override
            public boolean generate(EditSession editSession, Vector pos) throws MaxChangedBlocksException {
                TreeType[] choices = { REDWOOD, TALL_REDWOOD, MEGA_REDWOOD };
                return choices[TreeGenerator.RANDOM.nextInt(choices.length)].generate(editSession, pos);
            }
        },
        BIRCH("Birch tree", "birch", "white", "whitebark"),
        TALL_BIRCH("Tall birch tree", "tallbirch"),
        RANDOM_BIRCH("Random birch tree", "randbirch", "randombirch") {

            @Override
            public boolean generate(EditSession editSession, Vector pos) throws MaxChangedBlocksException {
                TreeType[] choices = { BIRCH, TALL_BIRCH };
                return choices[TreeGenerator.RANDOM.nextInt(choices.length)].generate(editSession, pos);
            }
        },
        JUNGLE("Jungle tree", "jungle"),
        SMALL_JUNGLE("Small jungle tree", "shortjungle", "smalljungle"),
        SHORT_JUNGLE("Short jungle tree") {

            @Override
            public boolean generate(EditSession editSession, Vector pos) throws MaxChangedBlocksException {
                return SMALL_JUNGLE.generate(editSession, pos);
            }
        },
        RANDOM_JUNGLE("Random jungle tree", "randjungle", "randomjungle") {

            @Override
            public boolean generate(EditSession editSession, Vector pos) throws MaxChangedBlocksException {
                TreeType[] choices = { JUNGLE, SMALL_JUNGLE };
                return choices[TreeGenerator.RANDOM.nextInt(choices.length)].generate(editSession, pos);
            }
        },
        JUNGLE_BUSH("Jungle bush", "junglebush", "jungleshrub"),
        RED_MUSHROOM("Red mushroom", "redmushroom", "redgiantmushroom"),
        BROWN_MUSHROOM("Brown mushroom", "brownmushroom", "browngiantmushroom"),
        RANDOM_MUSHROOM("Random mushroom", "randmushroom", "randommushroom") {

            @Override
            public boolean generate(EditSession editSession, Vector pos) throws MaxChangedBlocksException {
                TreeType[] choices = { RED_MUSHROOM, BROWN_MUSHROOM };
                return choices[TreeGenerator.RANDOM.nextInt(choices.length)].generate(editSession, pos);
            }
        },
        SWAMP("Swamp tree", "swamp", "swamptree"),
        ACACIA("Acacia tree", "acacia"),
        DARK_OAK("Dark oak tree", "darkoak"),
        PINE("Pine tree", "pine") {

            @Override
            public boolean generate(EditSession editSession, Vector pos) throws MaxChangedBlocksException {
                makePineTree(editSession, pos);
                return true;
            }
        },
        RANDOM("Random tree", "rand", "random") {

            @Override
            public boolean generate(EditSession editSession, Vector pos) throws MaxChangedBlocksException {
                TreeType[] choices = TreeType.values();
                return choices[TreeGenerator.RANDOM.nextInt(choices.length)].generate(editSession, pos);
            }
        };

        /**
         * Stores a map of the names for fast access.
         */
        private static final Map<String, TreeType> lookup = new HashMap<String, TreeType>();
        private static final Set<String> primaryAliases = Sets.newHashSet();

        private final String name;
        private final String[] lookupKeys;

        static {
            for (TreeType type : EnumSet.allOf(TreeType.class)) {
                for (String key : type.lookupKeys) {
                    lookup.put(key, type);
                }
                if (type.lookupKeys.length > 0) {
                    primaryAliases.add(type.lookupKeys[0]);
                }
            }
        }

        TreeType(String name, String... lookupKeys) {
            this.name = name;
            this.lookupKeys = lookupKeys;
        }

        public static Set<String> getAliases() {
            return Collections.unmodifiableSet(lookup.keySet());
        }

        public static Set<String> getPrimaryAliases() {
            return Collections.unmodifiableSet(primaryAliases);
        }

        public boolean generate(EditSession editSession, Vector pos) throws MaxChangedBlocksException {
            return editSession.getWorld()
                .generateTree(this, editSession, pos);
        }

        /**
         * Get user-friendly tree type name.
         *
         * @return a name
         */
        public String getName() {
            return name;
        }

        /**
         * Return type from name. May return null.
         *
         * @param name name to search
         * @return a tree type or null
         */
        @Nullable
        public static TreeType lookup(String name) {
            return lookup.get(name.toLowerCase());
        }
    }

    private static final Random RANDOM = new Random();

    private TreeType type;

    /**
     * Construct the tree generator with a tree type.
     *
     * @param type the tree type
     */
    @Deprecated
    public TreeGenerator(TreeType type) {
        this.type = type;
    }

    /**
     * Generate a tree.
     *
     * @param editSession the edit session
     * @param position    the position to generate the tree at
     * @return true if generation was successful
     * @throws MaxChangedBlocksException
     */
    public boolean generate(EditSession editSession, Vector position) throws MaxChangedBlocksException {
        return type.generate(editSession, position);
    }

    /**
     * Makes a terrible looking pine tree.
     *
     * @param basePosition the base position
     */
    private static void makePineTree(EditSession editSession, Vector basePosition) throws MaxChangedBlocksException {
        int trunkHeight = (int) Math.floor(Math.random() * 2) + 3;
        int height = (int) Math.floor(Math.random() * 5) + 8;

        BaseBlock logBlock = new BaseBlock(BlockID.LOG);
        BaseBlock leavesBlock = new BaseBlock(BlockID.LEAVES);

        // Create trunk
        for (int i = 0; i < trunkHeight; ++i) {
            if (!editSession.setBlockIfAir(basePosition.add(0, i, 0), logBlock)) {
                return;
            }
        }

        // Move up
        basePosition = basePosition.add(0, trunkHeight, 0);

        // Create tree + leaves
        for (int i = 0; i < height; ++i) {
            editSession.setBlockIfAir(basePosition.add(0, i, 0), logBlock);

            // Less leaves at these levels
            double chance = ((i == 0 || i == height - 1) ? 0.6 : 1);

            // Inner leaves
            editSession.setChanceBlockIfAir(basePosition.add(-1, i, 0), leavesBlock, chance);
            editSession.setChanceBlockIfAir(basePosition.add(1, i, 0), leavesBlock, chance);
            editSession.setChanceBlockIfAir(basePosition.add(0, i, -1), leavesBlock, chance);
            editSession.setChanceBlockIfAir(basePosition.add(0, i, 1), leavesBlock, chance);
            editSession.setChanceBlockIfAir(basePosition.add(1, i, 1), leavesBlock, chance);
            editSession.setChanceBlockIfAir(basePosition.add(-1, i, 1), leavesBlock, chance);
            editSession.setChanceBlockIfAir(basePosition.add(1, i, -1), leavesBlock, chance);
            editSession.setChanceBlockIfAir(basePosition.add(-1, i, -1), leavesBlock, chance);

            if (!(i == 0 || i == height - 1)) {
                for (int j = -2; j <= 2; ++j) {
                    editSession.setChanceBlockIfAir(basePosition.add(-2, i, j), leavesBlock, 0.6);
                }
                for (int j = -2; j <= 2; ++j) {
                    editSession.setChanceBlockIfAir(basePosition.add(2, i, j), leavesBlock, 0.6);
                }
                for (int j = -2; j <= 2; ++j) {
                    editSession.setChanceBlockIfAir(basePosition.add(j, i, -2), leavesBlock, 0.6);
                }
                for (int j = -2; j <= 2; ++j) {
                    editSession.setChanceBlockIfAir(basePosition.add(j, i, 2), leavesBlock, 0.6);
                }
            }
        }

        editSession.setBlockIfAir(basePosition.add(0, height, 0), leavesBlock);
    }

    /**
     * Looks up a tree type. May return null if a tree type by that
     * name is not found.
     *
     * @param type the tree type
     * @return a tree type or null
     */
    @Nullable
    public static TreeType lookup(String type) {
        return TreeType.lookup(type);
    }

}
