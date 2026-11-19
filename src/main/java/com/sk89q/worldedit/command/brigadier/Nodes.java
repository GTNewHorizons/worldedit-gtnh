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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.SingleRedirectModifier;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

/**
 * Small helpers that keep the command trees short: node builders, flags, aliases and a usage message for commands that
 * were typed incompletely.
 */
public final class Nodes {

    private static final Map<String, String> DESCRIPTIONS = new HashMap<>();

    private Nodes() {}

    public static LiteralArgumentBuilder<ICommandSender> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    /**
     * A command name together with the one line description that //help shows for it.
     */
    public static LiteralArgumentBuilder<ICommandSender> literal(String name, String description) {
        DESCRIPTIONS.put(name, description);
        return LiteralArgumentBuilder.literal(name);
    }

    /**
     * the one line description of a command, or an empty string if it has none
     */
    public static String descriptionOf(String name) {
        String description = DESCRIPTIONS.get(name);
        return description == null ? "" : description;
    }

    /**
     * Same as {@link #literal(String, String)} for a name that more than one group uses, such as {@code list}.
     */
    public static LiteralArgumentBuilder<ICommandSender> sub(String group, String name, String description) {
        DESCRIPTIONS.put(group + " " + name, description);
        return LiteralArgumentBuilder.literal(name);
    }

    /**
     * The description of a command inside a group.
     */
    public static String descriptionOf(String group, String name) {
        String description = DESCRIPTIONS.get(group + " " + name);
        return description != null ? description : descriptionOf(name);
    }

    public static <T> RequiredArgumentBuilder<ICommandSender, T> arg(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    /**
     * A word that is resolved later: a block, pattern, mask, direction, name...
     */
    public static RequiredArgumentBuilder<ICommandSender, String> word(String name) {
        return arg(name, TokenArgumentType.token());
    }

    /**
     * A word that is resolved later, with a few suggested values for tab completion.
     */
    public static RequiredArgumentBuilder<ICommandSender, String> word(String name, String... suggestions) {
        return Nodes.<String>arg(name, TokenArgumentType.token())
            .suggests((ctx, builder) -> {
                for (String suggestion : suggestions) {
                    if (suggestion.toLowerCase()
                        .startsWith(
                            builder.getRemaining()
                                .toLowerCase())) {
                        builder.suggest(suggestion);
                    }
                }
                return builder.buildFuture();
            });
    }

    /**
     * A whole number that must be at least {@code min}. Like all numbers it can be typed as a formula, such as 2*8, and
     * decimals are cut off.
     */
    public static RequiredArgumentBuilder<ICommandSender, NumberArgumentType.Input> integer(String name, int min) {
        return arg(name, new NumberArgumentType(min, Integer.MAX_VALUE));
    }

    public static RequiredArgumentBuilder<ICommandSender, NumberArgumentType.Input> integer(String name) {
        return arg(name, new NumberArgumentType(Integer.MIN_VALUE, Integer.MAX_VALUE));
    }

    public static RequiredArgumentBuilder<ICommandSender, NumberArgumentType.Input> integer(String name, int min,
        int max) {
        return arg(name, new NumberArgumentType(min, max));
    }

    public static RequiredArgumentBuilder<ICommandSender, NumberArgumentType.Input> decimal(String name, double min,
        double max) {
        return arg(name, new NumberArgumentType(min, max));
    }

    public static RequiredArgumentBuilder<ICommandSender, NumberArgumentType.Input> decimal(String name) {
        return arg(name, new NumberArgumentType(-Double.MAX_VALUE, Double.MAX_VALUE));
    }

    /**
     * Everything that is left of the line, spaces included.
     */
    public static RequiredArgumentBuilder<ICommandSender, String> text(String name) {
        return arg(name, StringArgumentType.greedyString());
    }

    /**
     * Builds a node that accepts the given flags in front of its arguments, as in {@code //sphere -hr stone 5}. The
     * flags can be asked for with {@link CommandCall#flag(char)}.
     *
     * @param builder the node
     * @param flags   the accepted flag letters, such as {@code "hr"}
     */
    public static LiteralCommandNode<ICommandSender> withFlags(LiteralArgumentBuilder<ICommandSender> builder,
        Flags flags) {
        LiteralArgumentBuilder<ICommandSender> bare = literal(builder.getLiteral()).requires(builder.getRequirement());
        if (builder.getCommand() != null) {
            bare.executes(builder.getCommand());
        }
        LiteralCommandNode<ICommandSender> node = bare.build();

        // Flags have to come first: when a word could be a flag or an argument, Brigadier takes the first sibling
        // that fits, and "//cut -e" must not treat -e as a pattern.
        // Typing a flag goes back to this same node, so any number of flags can be given, in any order.
        if (!flags.switches()
            .isEmpty()) {
            node.addChild(
                flagArgument(
                    "flags",
                    new FlagsArgumentType(flags.switches()),
                    node,
                    ctx -> FlaggedSender.withFlags(ctx.getSource(), ctx.getArgument("flags", String.class))).build());
        }
        for (Map.Entry<Character, String> valueFlag : flags.valueFlags()
            .entrySet()) {
            final char letter = valueFlag.getKey();
            final String label = valueFlag.getValue();
            node.addChild(
                literal("-" + letter)
                    .then(
                        flagArgument(
                            label,
                            TokenArgumentType.token(),
                            node,
                            ctx -> FlaggedSender
                                .withValue(ctx.getSource(), letter, ctx.getArgument(label, String.class))))
                    .build());
        }

        for (CommandNode<ICommandSender> argument : builder.getArguments()) {
            node.addChild(argument);
        }
        return node;
    }

    /**
     * Brigadier only applies a redirect modifier when something is typed after the redirect, so when the flag is the
     * last thing on the line, as in {@code //paste -a}, the command has to apply it by itself.
     */
    private static RequiredArgumentBuilder<ICommandSender, String> flagArgument(String name, ArgumentType<String> type,
        LiteralCommandNode<ICommandSender> target, SingleRedirectModifier<ICommandSender> applyFlag) {
        RequiredArgumentBuilder<ICommandSender, String> argument = RequiredArgumentBuilder.argument(name, type);
        Command<ICommandSender> command = target.getCommand();
        if (command != null) {
            argument.executes(ctx -> command.run(ctx.copyFor(applyFlag.apply(ctx))));
        }
        return argument.redirect(target, applyFlag);
    }

    public static LiteralCommandNode<ICommandSender> withFlags(LiteralArgumentBuilder<ICommandSender> builder,
        String switches) {
        return withFlags(builder, Flags.of(switches));
    }

    /**
     * Registers a command and all its aliases. Aliases forward to the command, so they take the same arguments.
     */
    public static void register(CommandDispatcher<ICommandSender> dispatcher, CommandNode<ICommandSender> node,
        String... aliases) {
        dispatcher.getRoot()
            .addChild(node);
        for (String alias : aliases) {
            dispatcher.getRoot()
                .addChild(
                    LiteralArgumentBuilder.<ICommandSender>literal(alias)
                        .requires(node.getRequirement())
                        .executes(node.getCommand())
                        .redirect(node)
                        .build());
        }
    }

    public static void register(CommandDispatcher<ICommandSender> dispatcher,
        LiteralArgumentBuilder<ICommandSender> builder, String... aliases) {
        register(dispatcher, builder.build(), aliases);
    }

    /**
     * Same as {@link #register(CommandDispatcher, CommandNode, String...)} for a command that takes flags.
     */
    public static void registerWithFlags(CommandDispatcher<ICommandSender> dispatcher,
        LiteralArgumentBuilder<ICommandSender> builder, String switches, String... aliases) {
        register(dispatcher, withFlags(builder, switches), aliases);
    }

    public static void registerWithFlags(CommandDispatcher<ICommandSender> dispatcher,
        LiteralArgumentBuilder<ICommandSender> builder, Flags flags, String... aliases) {
        register(dispatcher, withFlags(builder, flags), aliases);
    }

    /**
     * Adds an alias for a command that lives inside a group, such as {@code //brush sphere} also being
     * {@code //brush sph}.
     */
    public static LiteralCommandNode<ICommandSender> alias(String alias, LiteralCommandNode<ICommandSender> target) {
        return LiteralArgumentBuilder.<ICommandSender>literal(alias)
            .requires(target.getRequirement())
            .executes(target.getCommand())
            .redirect(target)
            .build();
    }

    // ------------------------------------------------------------------ usage

    /**
     * Put this on a literal that can not be run on its own, such as {@code //brush} or {@code //sphere}: typing it by
     * itself lists what can follow it.
     */
    public static final Command<ICommandSender> USAGE = ctx -> {
        ICommandSender sender = FlaggedSender.unwrap(ctx.getSource());
        List<ParsedCommandNode<ICommandSender>> typed = ctx.getNodes();
        CommandNode<ICommandSender> node = typed.get(typed.size() - 1)
            .getNode();
        if (node.getRedirect() != null) {
            node = node.getRedirect();
        }
        send(sender, EnumChatFormatting.RED, "Usage:");
        for (String line : usageLines(
            "/" + ctx.getInput()
                .trim(),
            node)) {
            send(sender, EnumChatFormatting.GRAY, line);
        }
        return 0;
    };

    private static void send(ICommandSender sender, EnumChatFormatting color, String text) {
        ChatComponentText message = new ChatComponentText(text);
        message.getChatStyle()
            .setColor(color);
        sender.addChatMessage(message);
    }

    /**
     * @return one line per way of using the node, such as {@code /line [-h] <pattern> [thickness]}
     */
    public static List<String> usageLines(String path, CommandNode<ICommandSender> node) {
        List<String> lines = new ArrayList<>();
        for (String tail : usageTails(node)) {
            lines.add(tail.isEmpty() ? path : path + " " + tail);
        }
        return lines;
    }

    private static boolean isValueFlag(CommandNode<ICommandSender> node) {
        return node instanceof LiteralCommandNode && node.getName()
            .length() == 2
            && node.getName()
                .charAt(0) == '-'
            && node.getChildren()
                .size() == 1
            && node.getChildren()
                .iterator()
                .next()
                .getRedirect() != null;
    }

    private static List<String> usageTails(CommandNode<ICommandSender> node) {
        List<String> tails = new ArrayList<>();
        String flags = "";
        List<CommandNode<ICommandSender>> visible = new ArrayList<>();
        for (CommandNode<ICommandSender> child : node.getChildren()) {
            if (child instanceof ArgumentCommandNode
                && ((ArgumentCommandNode<?, ?>) child).getType() instanceof FlagsArgumentType) {
                flags += "[-" + ((FlagsArgumentType) ((ArgumentCommandNode<?, ?>) child).getType()).allowed() + "] ";
            } else if (isValueFlag(child)) {
                flags += "[" + child.getName()
                    + " <"
                    + child.getChildren()
                        .iterator()
                        .next()
                        .getName()
                    + ">] ";
            } else if (child.getRedirect() == null) {
                visible.add(child);
            }
        }
        if (visible.isEmpty()) {
            return Collections.singletonList(flags.trim());
        }
        // Arguments after a node that can run on its own are optional
        boolean optional = node.getCommand() != null && node.getCommand() != USAGE;
        for (CommandNode<ICommandSender> child : visible) {
            String token = child instanceof LiteralCommandNode ? child.getName() : "<" + child.getName() + ">";
            for (String tail : usageTails(child)) {
                String text = token + (tail.isEmpty() ? "" : " " + tail);
                tails.add(flags + (optional ? "[" + text + "]" : text));
            }
        }
        return tails;
    }
}
