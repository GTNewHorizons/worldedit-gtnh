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

import java.util.Arrays;
import java.util.List;

import net.minecraft.command.ICommandSender;

import com.mojang.brigadier.context.CommandContext;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.UnknownDirectionException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.NoMatchException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.util.TreeGenerator.TreeType;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.biome.Biomes;
import com.sk89q.worldedit.world.registry.BiomeRegistry;

/**
 * Everything a command needs while it runs: who typed it, their session, the parsed arguments and flags.
 *
 * <p>
 * Instances are created by {@link CommandRunner} each time a command is run.
 * </p>
 */
public final class CommandCall {

    private final WorldEdit worldEdit;
    private final CommandContext<ICommandSender> context;
    private final Actor actor;
    private EditSession editSession;

    CommandCall(WorldEdit worldEdit, CommandContext<ICommandSender> context, Actor actor) {
        this.worldEdit = worldEdit;
        this.context = context;
        this.actor = actor;
    }

    // ---------------------------------------------------------------- who

    public WorldEdit worldEdit() {
        return worldEdit;
    }

    public Actor actor() {
        return actor;
    }

    /**
     * The player that ran the command. Only players can run WorldEdit commands, so this does not fail in practice.
     */
    public Player player() {
        if (actor instanceof Player) {
            return (Player) actor;
        }
        throw new CommandFailure("This command can only be used by a player.");
    }

    public LocalSession session() {
        return worldEdit.getSessionManager()
            .get(actor);
    }

    /**
     * The edit session of this command, created on first use. It is recorded for undo when the command is done.
     */
    public EditSession editSession() {
        if (editSession == null) {
            Player player = player();
            LocalSession session = session();
            editSession = session.createEditSession(player);
            editSession.enableQueue();
            session.tellVersion(player);
        }
        return editSession;
    }

    /**
     * @return the edit session if the command asked for one, otherwise null
     */
    EditSession editSessionIfCreated() {
        return editSession;
    }

    /**
     * The selection of the player.
     *
     * @throws IncompleteRegionException if nothing is selected
     */
    public Region region() throws IncompleteRegionException {
        Player player = player();
        return session().getSelection(player.getWorld());
    }

    public World world() {
        return player().getWorld();
    }

    // -------------------------------------------------------------- flags

    /**
     * @return true if the flag was typed, as in {@code -h}
     */
    public boolean flag(char flag) {
        return FlaggedSender.flagsOf(context.getSource())
            .contains(flag);
    }

    /**
     * @return the text typed after a flag such as {@code -m stone}, or null if the flag was not used
     */
    public String flagValue(char flag) {
        return FlaggedSender.valuesOf(context.getSource())
            .get(flag);
    }

    /**
     * @return the number typed after the flag, or the default if the flag was not used
     */
    public int flagInteger(char flag, int defaultValue) {
        String value = flagValue(flag);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new CommandFailure("Number expected after -" + flag + "; string \"" + value + "\" given.");
        }
    }

    /**
     * @return the mask typed after the flag, or null if the flag was not used
     */
    public Mask maskFlag(char flag) throws WorldEditException {
        String value = flagValue(flag);
        return value == null ? null : parseMask(value);
    }

    /**
     * @return the pattern typed after the flag, or null if the flag was not used
     */
    public Pattern patternFlag(char flag) throws WorldEditException {
        String value = flagValue(flag);
        return value == null ? null : parsePattern(value);
    }

    // ---------------------------------------------------------- arguments

    /**
     * @return true if the optional argument was typed
     */
    public boolean has(String name) {
        try {
            context.getArgument(name, Object.class);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public String string(String name) {
        return context.getArgument(name, String.class);
    }

    public String string(String name, String defaultValue) {
        return has(name) ? string(name) : defaultValue;
    }

    public int integer(String name) {
        return (int) decimal(name);
    }

    public int integer(String name, int defaultValue) {
        return has(name) ? integer(name) : defaultValue;
    }

    public double decimal(String name) {
        return context.getArgument(name, NumberArgumentType.Input.class)
            .value();
    }

    public double decimal(String name, double defaultValue) {
        return has(name) ? decimal(name) : defaultValue;
    }

    public boolean bool(String name) {
        return context.getArgument(name, Boolean.class);
    }

    // ---------------------------------------------- arguments needing a player

    private ParserContext parserContext() {
        ParserContext parserContext = new ParserContext();
        parserContext.setActor(actor);
        if (actor instanceof Entity) {
            Extent extent = ((Entity) actor).getExtent();
            if (extent instanceof World) {
                parserContext.setWorld((World) extent);
            }
        }
        parserContext.setSession(session());
        return parserContext;
    }

    /**
     * Reads a yes or no answer: true, t, yes, y, on or 1 are yes, and false, f, no, n, off or 0 are no.
     */
    public boolean yesNo(String name) {
        String input = string(name).toLowerCase();
        switch (input) {
            case "true":
            case "t":
            case "yes":
            case "y":
            case "on":
            case "1":
                return true;
            case "false":
            case "f":
            case "no":
            case "n":
            case "off":
            case "0":
                return false;
            default:
                throw new CommandFailure("Expected yes or no for <" + name + ">, but got '" + input + "'.");
        }
    }

    public BaseItem item(String name) throws WorldEditException {
        try {
            return worldEdit.getItemFactory()
                .parseFromInput(string(name), parserContext());
        } catch (NoMatchException e) {
            throw new CommandFailure(e.getMessage(), e);
        }
    }

    public BaseBlock block(String name) throws WorldEditException {
        return parseBlock(string(name));
    }

    public BaseBlock parseBlock(String input) throws WorldEditException {
        try {
            return worldEdit.getBlockFactory()
                .parseFromInput(input, parserContext());
        } catch (NoMatchException e) {
            throw new CommandFailure(e.getMessage(), e);
        }
    }

    public Pattern pattern(String name) throws WorldEditException {
        return parsePattern(string(name));
    }

    public Pattern parsePattern(String input) throws WorldEditException {
        try {
            return worldEdit.getPatternFactory()
                .parseFromInput(input, parserContext());
        } catch (NoMatchException e) {
            throw new CommandFailure(e.getMessage(), e);
        }
    }

    public Mask mask(String name) throws WorldEditException {
        return parseMask(string(name));
    }

    public Mask parseMask(String input) throws WorldEditException {
        // Some masks, such as =expression, read the edit session of the current request
        editSession();
        try {
            return worldEdit.getMaskFactory()
                .parseFromInput(input, parserContext());
        } catch (NoMatchException e) {
            throw new CommandFailure(e.getMessage(), e);
        }
    }

    /**
     * Resolves a direction such as {@code north}, {@code me} or {@code up} relative to the player.
     */
    public Vector direction(String name) throws UnknownDirectionException {
        return worldEdit.getDirection(player(), string(name));
    }

    public Vector direction(String name, String defaultValue) throws UnknownDirectionException {
        return worldEdit.getDirection(player(), string(name, defaultValue));
    }

    public TreeType treeType(String name) {
        String input = string(name);
        TreeType type = TreeGenerator.lookup(input);
        if (type == null) {
            throw new CommandFailure(
                String.format(
                    "Can't recognize tree type '%s' -- choose from: %s",
                    input,
                    Arrays.toString(TreeType.values())));
        }
        return type;
    }

    public BaseBiome biome(String name) {
        String input = string(name);
        BiomeRegistry biomeRegistry = world().getWorldData()
            .getBiomeRegistry();
        List<BaseBiome> knownBiomes = biomeRegistry.getBiomes();
        BaseBiome biome = Biomes.findBiomeByName(knownBiomes, input, biomeRegistry);
        if (biome == null) {
            throw new CommandFailure(
                String.format("Can't recognize biome type '%s' -- use /biomelist to list available types", input));
        }
        return biome;
    }

    /**
     * @return the command exactly as it was typed, without the leading slash
     */
    public String input() {
        return context.getInput();
    }
}
