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
import static com.sk89q.worldedit.command.brigadier.Nodes.word;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import net.minecraft.command.ICommandSender;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.brigadier.CommandFailure;
import com.sk89q.worldedit.command.brigadier.Flags;
import com.sk89q.worldedit.command.brigadier.Nodes;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.io.Closer;
import com.sk89q.worldedit.util.io.file.FilenameException;
import com.sk89q.worldedit.util.operation.OperationScheduler;
import com.sk89q.worldedit.world.registry.WorldData;

/**
 * Commands that work with schematic files.
 */
public final class SchematicCommands {

    private SchematicCommands() {}

    // spotless:off
    public static void register(CommandDispatcher<ICommandSender> dispatcher) {
        LiteralCommandNode<ICommandSender> load = literal("load", "Load a schematic into your clipboard").executes(Nodes.USAGE)
                        .then(word("filename-or-format")
                                .executes(run("worldedit.clipboard.load|worldedit.schematic.load",
                                        c -> load(c.player(), c.session(), "schematic", c.string("filename-or-format"))))
                                .then(word("filename")
                                        .executes(run("worldedit.clipboard.load|worldedit.schematic.load",
                                                c -> load(c.player(), c.session(), c.string("filename-or-format"), c.string("filename")))))).build();

        LiteralCommandNode<ICommandSender> save = literal("save", "Save your clipboard into a schematic").executes(Nodes.USAGE)
                        .then(word("filename-or-format")
                                .executes(run("worldedit.clipboard.save|worldedit.schematic.save",
                                        c -> save(c.player(), c.session(), "schematic", c.string("filename-or-format"))))
                                .then(word("filename")
                                        .executes(run("worldedit.clipboard.save|worldedit.schematic.save",
                                                c -> save(c.player(), c.session(), c.string("filename-or-format"), c.string("filename")))))).build();

        // The old names of these two
        dispatcher.getRoot().addChild(Nodes.alias("/load", load));
        dispatcher.getRoot().addChild(Nodes.alias("/save", save));

        LiteralCommandNode<ICommandSender> delete = deleteNode("delete");
        LiteralCommandNode<ICommandSender> formats = formatsNode("formats");
        LiteralCommandNode<ICommandSender> list = listNode("list");

        Nodes.register(dispatcher, literal("schematic", "Schematic commands for saving/loading areas").executes(Nodes.USAGE)
                .then(load)
                .then(save)
                .then(delete).then(Nodes.alias("d", delete))
                .then(formats).then(Nodes.alias("listformats", formats)).then(Nodes.alias("f", formats))
                .then(list).then(Nodes.alias("all", list)).then(Nodes.alias("ls", list)),
                "schem");
    }

    private static LiteralCommandNode<ICommandSender> deleteNode(String name) {
        return literal(name, "Delete a saved schematic").executes(Nodes.USAGE)
                .then(word("filename")
                        .executes(run("worldedit.schematic.delete", c -> delete(c.player(), c.string("filename")))))
                .build();
    }

    private static LiteralCommandNode<ICommandSender> formatsNode(String name) {
        return literal(name, "List available formats")
                .executes(run("worldedit.schematic.formats", c -> formats(c.actor())))
                .build();
    }

    private static LiteralCommandNode<ICommandSender> listNode(String name) {
        return Nodes.withFlags(Nodes.sub("schematic", name, "List saved schematics")
                .executes(run("worldedit.schematic.list",
                        c -> list(c.actor(), c.flag('d'), c.flag('n'), c.flagInteger('p', 1)))),
                Flags.of("dn").value('p', "page"));
    }
    // spotless:on

    /**
     * 9 schematics per page fits in the MC chat window.
     */
    private static final int SCHEMATICS_PER_PAGE = 9;
    private static final Logger log = Logger.getLogger(SchematicCommands.class.getCanonicalName());

    private static void load(Player player, LocalSession session, String formatName, String filename)
        throws FilenameException {
        LocalConfiguration config = WorldEdit.getInstance()
            .getConfiguration();

        File dir = WorldEdit.getInstance()
            .getWorkingDirectoryFile(config.saveDir);
        File f = WorldEdit.getInstance()
            .getSafeOpenFile(player, dir, filename, "schematic", "schematic");

        if (!f.exists()) {
            player.printError("Schematic " + filename + " does not exist!");
            return;
        }

        ClipboardFormat format = ClipboardFormat.findByAlias(formatName);
        if (format == null) {
            player.printError("Unknown schematic format: " + formatName);
            return;
        }

        Closer closer = Closer.create();
        try {
            FileInputStream fis = closer.register(new FileInputStream(f));
            BufferedInputStream bis = closer.register(new BufferedInputStream(fis));
            ClipboardReader reader = format.getReader(bis);

            WorldData worldData = player.getWorld()
                .getWorldData();
            Clipboard clipboard = reader.read(
                player.getWorld()
                    .getWorldData());
            session.setClipboard(new ClipboardHolder(clipboard, worldData));

            log.info(player.getName() + " loaded " + f.getCanonicalPath());
            player.print(filename + " loaded. Paste it with //paste");
        } catch (IOException e) {
            player.printError("Schematic could not read or it does not exist: " + e.getMessage());
            log.log(Level.WARNING, "Failed to load a saved clipboard", e);
        } finally {
            try {
                closer.close();
            } catch (IOException ignored) {}
        }
    }

    private static void save(Player player, LocalSession session, String formatName, String filename)
        throws WorldEditException {
        if (WorldEdit.getInstance()
            .getOperationScheduler()
            .hasPendingWork(player.getUniqueId())) {
            // A //copy still running would save a half-filled clipboard.
            player.printError(OperationScheduler.STILL_RUNNING);
            return;
        }

        LocalConfiguration config = WorldEdit.getInstance()
            .getConfiguration();

        File dir = WorldEdit.getInstance()
            .getWorkingDirectoryFile(config.saveDir);
        File f = WorldEdit.getInstance()
            .getSafeSaveFile(player, dir, filename, "schematic", "schematic");

        ClipboardFormat format = ClipboardFormat.findByAlias(formatName);
        if (format == null) {
            player.printError("Unknown schematic format: " + formatName);
            return;
        }

        ClipboardHolder holder = session.getClipboard();
        Clipboard clipboard = holder.getClipboard();
        Transform transform = holder.getTransform();
        Clipboard target;

        // If we have a transform, bake it into the copy
        if (!transform.isIdentity()) {
            FlattenedClipboardTransform result = FlattenedClipboardTransform
                .transform(clipboard, transform, holder.getWorldData());
            target = new BlockArrayClipboard(result.getTransformedRegion());
            target.setOrigin(clipboard.getOrigin());
            Operations.completeLegacy(result.copyTo(target));
        } else {
            target = clipboard;
        }

        Closer closer = Closer.create();
        try {
            // Create parent directories
            File parent = f.getParentFile();
            if (parent != null && !parent.exists()) {
                if (!parent.mkdirs()) {
                    throw new CommandFailure("Could not create folder for schematics!");
                }
            }

            FileOutputStream fos = closer.register(new FileOutputStream(f));
            BufferedOutputStream bos = closer.register(new BufferedOutputStream(fos));
            ClipboardWriter writer = closer.register(format.getWriter(bos));
            writer.write(target, holder.getWorldData());
            log.info(player.getName() + " saved " + f.getCanonicalPath());
            player.print(filename + " saved.");
        } catch (IOException e) {
            player.printError("Schematic could not written: " + e.getMessage());
            log.log(Level.WARNING, "Failed to write a saved clipboard", e);
        } finally {
            try {
                closer.close();
            } catch (IOException ignored) {}
        }
    }

    private static void delete(Player player, String filename) throws WorldEditException {

        LocalConfiguration config = WorldEdit.getInstance()
            .getConfiguration();

        File dir = WorldEdit.getInstance()
            .getWorkingDirectoryFile(config.saveDir);
        File f = WorldEdit.getInstance()
            .getSafeSaveFile(player, dir, filename, "schematic", "schematic");

        if (!f.exists()) {
            player.printError("Schematic " + filename + " does not exist!");
            return;
        }

        if (!f.delete()) {
            player.printError("Deletion of " + filename + " failed! Maybe it is read-only.");
            return;
        }

        player.print(filename + " has been deleted.");
    }

    private static void formats(Actor actor) throws WorldEditException {
        actor.print("Available clipboard formats (Name: Lookup names)");
        StringBuilder builder;
        boolean first = true;
        for (ClipboardFormat format : ClipboardFormat.values()) {
            builder = new StringBuilder();
            builder.append(format.name())
                .append(": ");
            for (String lookupName : format.getAliases()) {
                if (!first) {
                    builder.append(", ");
                }
                builder.append(lookupName);
                first = false;
            }
            first = true;
            actor.print(builder.toString());
        }
    }

    private static void list(Actor actor, boolean oldestFirst, boolean newestFirst, int page)
        throws WorldEditException {
        File dir = WorldEdit.getInstance()
            .getWorkingDirectoryFile(
                WorldEdit.getInstance()
                    .getConfiguration().saveDir);
        List<File> fileList = allFiles(dir);

        if (fileList.isEmpty()) {
            actor.printError("No schematics found.");
            return;
        }

        File[] files = new File[fileList.size()];
        fileList.toArray(files);

        int pageCount = files.length / SCHEMATICS_PER_PAGE + 1;
        if (page < 1) {
            actor.printError("Page must be at least 1");
            return;
        }
        if (page > pageCount) {
            actor.printError("Page must be less than " + (pageCount + 1));
            return;
        }

        final int sortType = oldestFirst ? -1 : newestFirst ? 1 : 0;
        // cleanup file list
        Arrays.sort(files, new Comparator<File>() {

            @Override
            public int compare(File f1, File f2) {
                // http://stackoverflow.com/questions/203030/best-way-to-list-files-in-java-sorted-by-date-modified
                int res;
                if (sortType == 0) { // use name by default
                    int p = f1.getParent()
                        .compareTo(f2.getParent());
                    if (p == 0) { // same parent, compare names
                        res = f1.getName()
                            .compareTo(f2.getName());
                    } else { // different parent, sort by that
                        res = p;
                    }
                } else {
                    res = Long.valueOf(f1.lastModified())
                        .compareTo(f2.lastModified()); // use date if there is a flag
                    if (sortType == 1) res = -res; // flip date for newest first instead of oldest first
                }
                return res;
            }
        });

        List<String> schematics = listFiles(
            WorldEdit.getInstance()
                .getConfiguration().saveDir,
            files);
        int offset = (page - 1) * SCHEMATICS_PER_PAGE;

        actor.print("Available schematics (Filename: Format) [" + page + "/" + pageCount + "]:");
        StringBuilder build = new StringBuilder();
        int limit = Math.min(offset + SCHEMATICS_PER_PAGE, schematics.size());
        for (int i = offset; i < limit;) {
            build.append(schematics.get(i));
            if (++i != limit) {
                build.append("\n");
            }
        }

        actor.print(build.toString());
    }

    private static List<File> allFiles(File root) {
        File[] files = root.listFiles();
        if (files == null) return null;
        List<File> fileList = new ArrayList<File>();
        for (File f : files) {
            if (f.isDirectory()) {
                List<File> subFiles = allFiles(f);
                if (subFiles == null) continue; // empty subdir
                fileList.addAll(subFiles);
            } else {
                fileList.add(f);
            }
        }
        return fileList;
    }

    private static List<String> listFiles(String prefix, File[] files) {
        if (prefix == null) prefix = "";
        List<String> result = new ArrayList<String>();
        for (File file : files) {
            StringBuilder build = new StringBuilder();

            build.append("\u00a72");
            ClipboardFormat format = ClipboardFormat.findByFile(file);
            boolean inRoot = file.getParentFile()
                .getName()
                .equals(prefix);
            build.append(
                inRoot ? file.getName()
                    : file.getPath()
                        .split(Pattern.quote(prefix + File.separator))[1])
                .append(": ")
                .append(format == null ? "Unknown" : format.name());
            result.add(build.toString());
        }
        return result;
    }
}
