package com.sk89q.worldedit.command.brigadier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.command.ICommandSender;

import org.junit.Test;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.sk89q.worldedit.WorldEdit;

/**
 * Builds the whole command tree and checks it against the documentation, so that a command can not be added or changed
 * without its page in {@code docs/commands} following.
 */
public class CommandDocsTest {

    private static final Set<String> GROUPS = new HashSet<>(
        Arrays.asList("/brush", "/schematic", "/snapshot", "/superpickaxe", "/tool", "worldedit"));

    /** Commands whose name would not make a clear or unique file name. */
    private static final Map<String, String> PAGE_NAMES = new HashMap<>();

    private static CommandDispatcher<ICommandSender> buildTree() {
        CommandDispatcher<ICommandSender> dispatcher = new CommandDispatcher<>();
        CommandRegistry.registerAll(dispatcher, mock(WorldEdit.class));
        return dispatcher;
    }

    /** One command as a player types it, and the page that documents it. */
    private static final class Entry {

        final String page;
        final String typed;
        final List<String> usage;
        final List<String> aliases;

        Entry(String page, String typed, List<String> usage, List<String> aliases) {
            this.page = page;
            this.typed = typed;
            this.usage = usage;
            this.aliases = aliases;
        }
    }

    private static List<Entry> entries(CommandDispatcher<ICommandSender> dispatcher) {
        CommandNode<ICommandSender> root = dispatcher.getRoot();
        List<Entry> entries = new ArrayList<>();
        for (CommandNode<ICommandSender> node : root.getChildren()) {
            if (node.getRedirect() != null) {
                continue;
            }
            String name = node.getName();
            String typed = "/" + name;
            String page = PAGE_NAMES.containsKey(name) ? PAGE_NAMES.get(name) : name.replace("/", "");
            entries.add(new Entry(page, typed, Nodes.usageLines(typed, node), aliases(root, node, true)));

            if (GROUPS.contains(name)) {
                for (CommandNode<ICommandSender> child : node.getChildren()) {
                    // The tool group holds the same commands that can be typed without the group
                    if (child.getRedirect() == null && child instanceof LiteralCommandNode
                        && root.getChild(child.getName()) != child) {
                        String childTyped = typed + " " + child.getName();
                        entries.add(
                            new Entry(
                                page + "-" + child.getName(),
                                childTyped,
                                Nodes.usageLines(childTyped, child),
                                aliases(node, child, false)));
                    }
                }
            }
        }
        return entries;
    }

    private static List<String> aliases(CommandNode<ICommandSender> parent, CommandNode<ICommandSender> node,
        boolean isRoot) {
        List<String> aliases = new ArrayList<>();
        for (CommandNode<ICommandSender> other : parent.getChildren()) {
            if (other.getRedirect() != node) {
                continue;
            }
            String name = other.getName();
            // Any command can be typed with an extra slash, that is not worth a mention
            boolean extraSlash = isRoot && name.startsWith("/") && parent.getChild(name.substring(1)) != null;
            if (!extraSlash) {
                aliases.add(name);
            }
        }
        Collections.sort(aliases);
        return aliases;
    }

    private static File docsDirectory() {
        File dir = new File(System.getProperty("user.dir")).getAbsoluteFile();
        while (dir != null) {
            File docs = new File(dir, "docs/commands");
            if (docs.isDirectory()) {
                return docs;
            }
            dir = dir.getParentFile();
        }
        throw new AssertionError("docs/commands not found from " + System.getProperty("user.dir"));
    }

    @Test
    public void everyCommandStartsWithTwoSlashes() {
        List<String> single = new ArrayList<>();
        for (CommandNode<ICommandSender> node : buildTree().getRoot()
            .getChildren()) {
            String name = node.getName();
            // Typed with one slash too, so that a mod clash can always be worked around
            if (!name.startsWith("/") && !name.equals("worldedit") && !name.equals("we")) {
                single.add("/" + name);
            }
        }
        assertEquals("Commands that can be typed with one slash: " + single, 0, single.size());
    }

    @Test
    public void everyCommandBuilds() {
        assertTrue(entries(buildTree()).size() > 100);
    }

    @Test
    public void everyCommandHasAPageWithItsRealUsage() throws IOException {
        File docs = docsDirectory();
        List<String> problems = new ArrayList<>();
        for (Entry entry : entries(buildTree())) {
            File page = new File(docs, entry.page + ".md");
            if (!page.isFile()) {
                problems.add("No page for " + entry.typed + " (" + page.getName() + ")");
                continue;
            }
            String text = new String(Files.readAllBytes(page.toPath()), StandardCharsets.UTF_8);
            for (String usage : entry.usage) {
                if (!text.contains(usage)) {
                    problems.add(page.getName() + " does not show the usage: " + usage);
                }
            }
        }
        assertEquals(String.join("\n", problems), 0, problems.size());
    }

    @Test
    public void everyPageBelongsToACommand() {
        Set<String> pages = new HashSet<>();
        for (Entry entry : entries(buildTree())) {
            pages.add(entry.page + ".md");
        }
        List<String> orphans = new ArrayList<>();
        for (String file : docsDirectory().list()) {
            if (!pages.contains(file)) {
                orphans.add(file);
            }
        }
        assertEquals(String.join("\n", orphans), 0, orphans.size());
    }

}
