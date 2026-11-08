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

// $Id$

package com.sk89q.worldedit.world.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import com.sk89q.worldedit.world.DataException;
import com.sk89q.worldedit.world.World;

/**
 * Represents the chunk store used by Minecraft alpha but zipped.
 */
public class ZippedMcRegionChunkStore extends McRegionChunkStore {

    protected File zipFile;
    protected ZipFile zip;
    protected String folder;

    /**
     * Create an instance. The folder argument lets you choose a folder or
     * path to look into in the ZIP for the files. Use a blank string for
     * the folder to not look into a subdirectory.
     *
     * @param zipFile the ZIP file
     * @param folder  the folder
     * @throws IOException
     * @throws ZipException
     */
    public ZippedMcRegionChunkStore(File zipFile, String folder) throws IOException, ZipException {
        this.zipFile = zipFile;
        this.folder = folder;

        zip = new ZipFile(zipFile);
    }

    /**
     * Create an instance. The sub-folder containing the chunk data will
     * be detected.
     *
     * @param zipFile the ZIP file
     * @throws IOException
     * @throws ZipException
     */
    public ZippedMcRegionChunkStore(File zipFile) throws IOException, ZipException {
        this.zipFile = zipFile;

        zip = new ZipFile(zipFile);
    }

    @Override
    protected InputStream getInputStream(String name, World world) throws IOException, DataException {
        if (folder == null) {
            folder = findRegionFolder(world);
        }

        ZipEntry entry = getEntry(folder.isEmpty() ? name : folder + "/" + name);
        if (entry == null) {
            throw new MissingChunkException();
        }
        try {
            return zip.getInputStream(entry);
        } catch (ZipException e) {
            throw new IOException("Failed to read " + name + " in ZIP");
        }
    }

    /**
     * Find the folder holding a world's region files. The world can be at any depth, because backups
     * taken in single player keep the saves/ folder in their paths.
     */
    private String findRegionFolder(World world) throws MissingWorldException {
        String dimension = world.getDimensionFolder();
        String regionPath = dimension == null ? "region" : dimension + "/region";
        String worldRegionPath = world.getDirectoryName() + "/" + regionPath;
        boolean atRoot = false;

        for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements();) {
            String entry = e.nextElement()
                .getName()
                .replace('\\', '/');
            int slash = entry.lastIndexOf('/');
            if (slash < 0 || !entry.endsWith(".mca")) {
                continue;
            }

            String parent = entry.substring(0, slash);
            if (parent.equals(worldRegionPath) || parent.endsWith("/" + worldRegionPath)) {
                return parent;
            }
            atRoot |= parent.equals(regionPath);
        }

        if (atRoot) {
            return regionPath;
        }
        throw new MissingWorldException("Target world is not present in ZIP.", world.getDirectoryName());
    }

    /**
     * Get an entry from the ZIP, trying both types of slashes.
     *
     * @param file the file
     * @return a ZIP entry
     */
    private ZipEntry getEntry(String file) {
        ZipEntry entry = zip.getEntry(file);
        if (entry != null) {
            return entry;
        }
        return zip.getEntry(file.replace("/", "\\"));
    }

    @Override
    public void close() throws IOException {
        zip.close();
    }

}
