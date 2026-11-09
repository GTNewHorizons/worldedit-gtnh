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

package com.sk89q.worldedit.command;

import static com.sk89q.worldedit.command.brigadier.CommandRunner.run;
import static com.sk89q.worldedit.command.brigadier.Nodes.literal;
import static com.sk89q.worldedit.command.brigadier.Nodes.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.minecraft.command.ICommandSender;

import com.gtnewhorizon.gtnhlib.brigadier.BrigadierApi;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.sk89q.worldedit.command.brigadier.CommandCall;
import com.sk89q.worldedit.command.brigadier.Nodes;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.util.formatting.ColorCodeBuilder;
import com.sk89q.worldedit.util.formatting.Style;
import com.sk89q.worldedit.util.formatting.StyledFragment;
import com.sk89q.worldedit.util.formatting.component.Code;
import com.sk89q.worldedit.util.formatting.component.CommandListBox;

/**
 * {@code //help [command] [subcommand...] [page]}: lists the WorldEdit commands, or shows how to use one.
 *
 * <p>
 * It reads the Brigadier tree, so it always shows what can really be typed.
 * </p>
 */
public final class HelpCommand {

    private HelpCommand() {}

    public static void register(CommandDispatcher<ICommandSender> dispatcher) {
        Nodes.register(dispatcher, node("/help"));
    }

    /**
     * The help command, to be registered under the name it is typed with.
     */
    // spotless:off
    public static LiteralCommandNode<ICommandSender> node(String name) {
        return literal(name, "Displays help for WorldEdit commands")
                .executes(run("worldedit.help", c -> help(c, "")))
                .then(text("command")
                        .executes(run("worldedit.help", c -> help(c, c.string("command")))))
                .build();
    }
    // spotless:on

    private static void help(CommandCall call, String topic) {
        Actor actor = call.actor();
        String trimmed = topic.trim();
        String[] words = trimmed.isEmpty() ? new String[0] : trimmed.split("\\s+");
        int effectiveLength = words.length;
        int page = 0;
        int perPage = actor instanceof Player ? 8 : 20;

        // A number at the end is the page
        if (words.length > 0) {
            try {
                page = Math.max(1, Integer.parseInt(words[words.length - 1])) - 1;
                effectiveLength--;
            } catch (NumberFormatException ignored) {}
        }

        CommandNode<ICommandSender> node = BrigadierApi.getCommandDispatcher()
            .getRoot();
        List<String> visited = new ArrayList<>();
        for (int i = 0; i < effectiveLength; i++) {
            String word = words[i];
            CommandNode<ICommandSender> next = findChild(node, word, i == 0);
            if (next == null) {
                if (i == 0) {
                    actor.printError(String.format("The command '%s' could not be found.", word));
                } else {
                    actor.printError(
                        String.format(
                            "The sub-command '%s' under '%s' could not be found.",
                            word,
                            String.join(" ", visited)));
                }
                return;
            }
            node = next.getRedirect() != null ? next.getRedirect() : next;
            visited.add(word);
        }

        List<CommandNode<ICommandSender>> subCommands = subCommands(node);
        if (subCommands.isEmpty()) {
            showUsage(actor, node, visited);
        } else {
            showList(actor, node.getName(), visited, subCommands, page, perPage);
        }
    }

    /**
     * At the top level the slashes are optional: {@code //help limit} finds {@code //limit}.
     */
    private static CommandNode<ICommandSender> findChild(CommandNode<ICommandSender> node, String word,
        boolean isRootLevel) {
        CommandNode<ICommandSender> found = node.getChild(word);
        if (found != null) {
            return found;
        }
        if (isRootLevel) {
            if (word.length() > 1 && word.charAt(0) == '/') {
                found = node.getChild(word.substring(1));
                if (found != null) {
                    return found;
                }
            }
            return node.getChild("/" + word);
        }
        return null;
    }

    /**
     * The commands below a node, not counting the names that only forward to another command and the flags.
     */
    private static List<CommandNode<ICommandSender>> subCommands(CommandNode<ICommandSender> node) {
        List<CommandNode<ICommandSender>> children = new ArrayList<>();
        for (CommandNode<ICommandSender> child : node.getChildren()) {
            if (child instanceof LiteralCommandNode && child.getRedirect() == null && !isFlag(child)) {
                children.add(child);
            }
        }
        return children;
    }

    private static boolean isFlag(CommandNode<ICommandSender> node) {
        return node.getName()
            .length() == 2
            && node.getName()
                .charAt(0) == '-';
    }

    private static String sortKey(CommandNode<ICommandSender> node) {
        String name = node.getName();
        return name.startsWith("/") ? name.substring(1) : name;
    }

    private static void showList(Actor actor, String group, List<String> visited,
        List<CommandNode<ICommandSender>> subCommands, int page, int perPage) {
        boolean isRootLevel = visited.isEmpty();
        List<CommandNode<ICommandSender>> sorted = new ArrayList<>(subCommands);
        Collections.sort(sorted, new Comparator<CommandNode<ICommandSender>>() {

            @Override
            public int compare(CommandNode<ICommandSender> a, CommandNode<ICommandSender> b) {
                return sortKey(a).compareToIgnoreCase(sortKey(b));
            }
        });

        int offset = perPage * page;
        int pageTotal = (int) Math.ceil(sorted.size() / (double) perPage);

        CommandListBox box = new CommandListBox(String.format("Help: page %d/%d ", page + 1, pageTotal));
        StyledFragment contents = box.getContents();
        StyledFragment tip = contents.createFragment(Style.GRAY);

        if (offset >= sorted.size()) {
            tip.createFragment(Style.RED)
                .append(String.format("There is no page %d (total number of pages is %d).", page + 1, pageTotal))
                .newLine();
        } else {
            tip.append("Type ");
            tip.append(
                new Code().append("//help ")
                    .append("<command> [<page>]"));
            tip.append(" for more information.")
                .newLine();

            for (CommandNode<ICommandSender> child : sorted
                .subList(offset, Math.min(offset + perPage, sorted.size()))) {
                StringBuilder builder = new StringBuilder();
                if (isRootLevel) {
                    builder.append("/");
                } else {
                    builder.append(String.join(" ", visited))
                        .append(" ");
                }
                builder.append(child.getName());
                box.appendCommand(builder.toString(), Nodes.descriptionOf(group, child.getName()));
            }
        }
        actor.printRaw(ColorCodeBuilder.asColorCodes(box));
    }

    private static void showUsage(Actor actor, CommandNode<ICommandSender> node, List<String> visited) {
        String path = "/" + String.join(" ", visited);
        CommandListBox box = new CommandListBox("Help: " + path);
        String description = Nodes.descriptionOf(node.getName());
        if (!description.isEmpty()) {
            box.getContents()
                .createFragment(Style.GRAY)
                .append(description)
                .newLine();
        }
        for (String line : Nodes.usageLines(path, node)) {
            box.appendCommand(line, "");
        }
        actor.printRaw(ColorCodeBuilder.asColorCodes(box));
    }
}
