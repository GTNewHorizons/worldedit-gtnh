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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

/**
 * Wraps the real command sender and remembers which flags (such as the {@code -h} in {@code //sphere -h stone 5})
 * were typed in front of the arguments.
 *
 * <p>
 * Brigadier gives every redirect its own context, so the flags can not be read from the final context. Carrying them in
 * the source is the only thing that survives a redirect.
 * </p>
 */
final class FlaggedSender implements ICommandSender {

    private final ICommandSender delegate;
    private final Set<Character> flags;
    private final Map<Character, String> values;

    private FlaggedSender(ICommandSender delegate, Set<Character> flags, Map<Character, String> values) {
        this.delegate = delegate;
        this.flags = flags;
        this.values = values;
    }

    /**
     * Returns a sender that has the given flags in addition to those of {@code sender}.
     */
    static ICommandSender withFlags(ICommandSender sender, String typedFlags) {
        Set<Character> merged = new HashSet<>(flagsOf(sender));
        for (char c : typedFlags.toCharArray()) {
            if (c != '-') {
                merged.add(c);
            }
        }
        return new FlaggedSender(unwrap(sender), merged, valuesOf(sender));
    }

    /**
     * Returns a sender that also remembers the value typed after a flag such as {@code -m stone}.
     */
    static ICommandSender withValue(ICommandSender sender, char flag, String value) {
        Map<Character, String> merged = new HashMap<>(valuesOf(sender));
        merged.put(flag, value);
        return new FlaggedSender(unwrap(sender), new HashSet<>(flagsOf(sender)), merged);
    }

    static Map<Character, String> valuesOf(ICommandSender sender) {
        return sender instanceof FlaggedSender ? ((FlaggedSender) sender).values
            : Collections.<Character, String>emptyMap();
    }

    static Set<Character> flagsOf(ICommandSender sender) {
        return sender instanceof FlaggedSender ? ((FlaggedSender) sender).flags : Collections.<Character>emptySet();
    }

    static ICommandSender unwrap(ICommandSender sender) {
        return sender instanceof FlaggedSender ? ((FlaggedSender) sender).delegate : sender;
    }

    @Override
    public String getCommandSenderName() {
        return delegate.getCommandSenderName();
    }

    @Override
    public IChatComponent func_145748_c_() {
        return delegate.func_145748_c_();
    }

    @Override
    public void addChatMessage(IChatComponent message) {
        delegate.addChatMessage(message);
    }

    @Override
    public boolean canCommandSenderUseCommand(int level, String command) {
        return delegate.canCommandSenderUseCommand(level, command);
    }

    @Override
    public ChunkCoordinates getPlayerCoordinates() {
        return delegate.getPlayerCoordinates();
    }

    @Override
    public World getEntityWorld() {
        return delegate.getEntityWorld();
    }
}
