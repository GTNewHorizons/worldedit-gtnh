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

package com.sk89q.worldedit.command.util;

/**
 * Reads typed coordinates such as {@code 10,64,-20}, {@code ~,~5,~} or {@code ~ ~5 ~}, where {@code ~}
 * is relative to a given origin, usually where the player stands.
 */
public final class RelativeCoordinates {

    private RelativeCoordinates() {}

    /**
     * Read coordinates separated by commas or spaces.
     *
     * @param input  what was typed
     * @param origin what {@code ~} is relative to, one value per coordinate
     * @return the coordinates, as many as the origin has
     * @throws IllegalArgumentException with a message for the player if the input can't be read
     */
    public static int[] parse(String input, int... origin) {
        String[] parts = input.trim()
            .split("\\s*,\\s*|\\s+");
        if (parts.length != origin.length) {
            throw new IllegalArgumentException(
                "Expected " + origin.length + " coordinates, separated by commas or spaces, but got: " + input);
        }

        int[] result = new int[origin.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = parseOne(parts[i], origin[i], input);
        }
        return result;
    }

    private static int parseOne(String part, int origin, String input) {
        boolean relative = part.startsWith("~");
        String number = relative ? part.substring(1) : part;
        if (relative && number.isEmpty()) {
            return origin;
        }
        if (!number.matches("[-+]?\\d{1,9}")) {
            throw new IllegalArgumentException("Invalid coordinates " + input);
        }
        int value = Integer.parseInt(number);
        return relative ? origin + value : value;
    }
}
