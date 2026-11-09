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

import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

/**
 * Parses a group of single letter flags such as {@code -h} or {@code -ao}. Only the letters the command declares are
 * accepted, so a negative number such as {@code -5} is never mistaken for a flag.
 */
final class FlagsArgumentType implements ArgumentType<String> {

    private static final SimpleCommandExceptionType NOT_A_FLAG = new SimpleCommandExceptionType(
        new LiteralMessage("Not a flag of this command"));

    private final String allowed;

    FlagsArgumentType(String allowed) {
        this.allowed = allowed;
    }

    String allowed() {
        return allowed;
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        if (!reader.canRead() || reader.peek() != '-') {
            throw NOT_A_FLAG.createWithContext(reader);
        }
        reader.skip();
        int letters = 0;
        while (reader.canRead() && reader.peek() != ' ') {
            if (allowed.indexOf(reader.peek()) < 0) {
                reader.setCursor(start);
                throw NOT_A_FLAG.createWithContext(reader);
            }
            reader.skip();
            letters++;
        }
        if (letters == 0) {
            reader.setCursor(start);
            throw NOT_A_FLAG.createWithContext(reader);
        }
        return reader.getString()
            .substring(start, reader.getCursor());
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining();
        if (remaining.isEmpty() || remaining.charAt(0) == '-') {
            for (char flag : allowed.toCharArray()) {
                String candidate = "-" + flag;
                if (candidate.startsWith(remaining)) {
                    builder.suggest(candidate);
                }
            }
        }
        return builder.buildFuture();
    }
}
