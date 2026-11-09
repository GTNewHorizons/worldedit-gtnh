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

package com.sk89q.worldedit.command.brigadier;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

/**
 * Reads one whitespace separated word without interpreting it.
 *
 * <p>
 * Blocks, patterns, masks and directions all have their own grammar ({@code 5%stone,dirt}, {@code #clipboard},
 * {@code !air}...) and need the player to be resolved, so Brigadier only splits them off and the command resolves them
 * with {@link CommandCall}.
 * </p>
 */
public final class TokenArgumentType implements ArgumentType<String> {

    private static final TokenArgumentType INSTANCE = new TokenArgumentType();

    private TokenArgumentType() {}

    public static TokenArgumentType token() {
        return INSTANCE;
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        if (reader.canRead() && (reader.peek() == '"' || reader.peek() == '\'')) {
            return readQuoted(reader);
        }
        while (reader.canRead() && reader.peek() != ' ') {
            reader.skip();
        }
        if (reader.getCursor() == start) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol()
                .createWithContext(reader, "a value");
        }
        return reader.getString()
            .substring(start, reader.getCursor());
    }

    /**
     * A value in quotes may hold spaces, as in {@code //gmask "stone !air"} or a file name.
     */
    private static String readQuoted(StringReader reader) throws CommandSyntaxException {
        char quote = reader.read();
        int start = reader.getCursor();
        while (reader.canRead() && reader.peek() != quote) {
            reader.skip();
        }
        if (!reader.canRead()) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedEndOfQuote()
                .createWithContext(reader);
        }
        String value = reader.getString()
            .substring(start, reader.getCursor());
        reader.skip();
        return value;
    }
}
