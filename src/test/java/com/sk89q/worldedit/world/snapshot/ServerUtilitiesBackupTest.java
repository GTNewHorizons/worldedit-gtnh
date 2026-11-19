package com.sk89q.worldedit.world.snapshot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.sk89q.jnbt.ByteArrayTag;
import com.sk89q.jnbt.ByteTag;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.chunk.Chunk;
import com.sk89q.worldedit.world.storage.ChunkStore;

/**
 * Reads real region data through the same path as //restore, from a zip laid out like a Server
 * Utilities backup, with sections in the formats EndlessIDs and NotEnoughIDs write.
 */
public class ServerUtilitiesBackupTest {

    private static final Vector BLOCK = new Vector(0, 0, 0);
    private static final Vector NEXT_BLOCK = new Vector(1, 0, 0);

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void readsExtendedIdsAndMetadataFromTheRightDimension() throws Exception {
        File dir = folder.newFolder("backups");
        writeBackup(
            new File(dir, "2026-10-07-22-40-46.zip"),
            "saves/New World/",
            region(endlessIdsSection(0x012345, 0xABCD)),
            region(endlessIdsSection(0x054321, 0x1234)));

        assertBlock(dir, "New World", null, 0x012345, 0xABCD);
        assertBlock(dir, "New World", "DIM-1", 0x054321, 0x1234);
    }

    @Test
    public void readsServerLayoutWithTheWorldAtTheTop() throws Exception {
        File dir = folder.newFolder("backups");
        writeBackup(
            new File(dir, "2026-10-07-22-40-46.zip"),
            "World/",
            region(endlessIdsSection(70000, 40)),
            region(endlessIdsSection(1, 1)));

        assertBlock(dir, "World", null, 70000, 40);
    }

    @Test
    public void readsNotEnoughIdsSections() throws Exception {
        File dir = folder.newFolder("backups");
        writeBackup(
            new File(dir, "2026-10-07-22-40-46.zip"),
            "World/",
            region(notEnoughIdsSection(0xF234, 0xFEDC)),
            region(notEnoughIdsSection(1, 1)));

        assertBlock(dir, "World", null, 0xF234, 0xFEDC);
    }

    @Test
    public void readsVanillaSections() throws Exception {
        File dir = folder.newFolder("backups");
        writeBackup(
            new File(dir, "2026-10-07-22-40-46.zip"),
            "World/",
            region(vanillaSection(0x234, 7)),
            region(vanillaSection(1, 1)));

        assertBlock(dir, "World", null, 0x234, 7);
    }

    @Test
    public void ignoresBackupsOfOtherWorlds() throws Exception {
        File dir = folder.newFolder("backups");
        writeBackup(
            new File(dir, "2026-10-07-22-40-46.zip"),
            "saves/Other World/",
            region(vanillaSection(1, 1)),
            region(vanillaSection(1, 1)));

        assertTrue(
            new SnapshotRepository(dir).getSnapshots(true, "World")
                .isEmpty());
    }

    private static void assertBlock(File dir, String worldFolder, String dimension, int id, int data) throws Exception {
        World world = mock(World.class);
        when(world.getDirectoryName()).thenReturn(worldFolder);
        when(world.getDimensionFolder()).thenReturn(dimension);

        Snapshot snapshot = new SnapshotRepository(dir).getDefaultSnapshot(worldFolder);
        ChunkStore store = snapshot.getChunkStore();
        try {
            Chunk chunk = store.getChunk(new Vector2D(0, 0), world);
            assertEquals(id, chunk.getBlockID(BLOCK));
            assertEquals(data, chunk.getBlockData(BLOCK));
            assertEquals(0, chunk.getBlockID(NEXT_BLOCK));
            assertEquals(0, chunk.getBlockData(NEXT_BLOCK));
        } finally {
            store.close();
        }
    }

    private static void writeBackup(File file, String prefix, byte[] overworld, byte[] nether) throws Exception {
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(file));
        try {
            // The nether goes first, because the region folder must not be picked by order.
            add(zip, prefix + "DIM-1/region/r.0.0.mca", nether);
            add(zip, prefix + "level.dat", new byte[1]);
            add(zip, prefix + "region/r.0.0.mca", overworld);
        } finally {
            zip.close();
        }
    }

    private static void add(ZipOutputStream zip, String name, byte[] content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content);
        zip.closeEntry();
    }

    private static byte[] region(CompoundTag section) throws Exception {
        List<Tag> sections = new ArrayList<Tag>();
        sections.add(section);

        Map<String, Tag> level = new HashMap<String, Tag>();
        level.put("xPos", new IntTag(0));
        level.put("zPos", new IntTag(0));
        level.put("Sections", new ListTag(CompoundTag.class, sections));
        level.put("TileEntities", new ListTag(CompoundTag.class, new ArrayList<Tag>()));

        Map<String, Tag> root = new HashMap<String, Tag>();
        root.put("Level", new CompoundTag(level));

        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        NBTOutputStream nbt = new NBTOutputStream(new DeflaterOutputStream(compressed));
        nbt.writeNamedTag("", new CompoundTag(root));
        nbt.close();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(out);
        int sectors = compressed.size() / 4096 + 1;
        data.writeInt(2 << 8 | sectors); // chunk 0,0 starts at sector 2
        data.write(new byte[8192 - 4]);
        data.writeInt(compressed.size() + 1);
        data.writeByte(2); // deflate
        data.write(compressed.toByteArray());
        data.write(new byte[4096]);
        return out.toByteArray();
    }

    private static CompoundTag vanillaSection(int id, int data) {
        byte[] blocks = new byte[4096];
        byte[] add = new byte[2048];
        byte[] meta = new byte[2048];
        blocks[0] = (byte) id;
        add[0] = (byte) (id >> 8 & 0x0F);
        meta[0] = (byte) (data & 0x0F);

        Map<String, Tag> section = new HashMap<String, Tag>();
        section.put("Y", new ByteTag((byte) 0));
        section.put("Blocks", new ByteArrayTag(blocks));
        section.put("Add", new ByteArrayTag(add));
        section.put("Data", new ByteArrayTag(meta));
        return new CompoundTag(section);
    }

    private static CompoundTag endlessIdsSection(int id, int data) {
        Map<String, Tag> section = new HashMap<String, Tag>(vanillaSection(id, data).getValue());

        byte[] blocksHigh = new byte[2048];
        byte[] blocksTop = new byte[4096];
        byte[] dataHigh = new byte[2048];
        byte[] dataTop = new byte[4096];
        blocksHigh[0] = (byte) (id >> 12 & 0x0F);
        blocksTop[0] = (byte) (id >> 16);
        dataHigh[0] = (byte) (data >> 4 & 0x0F);
        dataTop[0] = (byte) (data >> 8);

        section.put("BlocksB2Hi", new ByteArrayTag(blocksHigh));
        section.put("BlocksB3", new ByteArrayTag(blocksTop));
        section.put("Data1High", new ByteArrayTag(dataHigh));
        section.put("Data2", new ByteArrayTag(dataTop));
        return new CompoundTag(section);
    }

    private static CompoundTag notEnoughIdsSection(int id, int data) {
        byte[] blocks = new byte[8192];
        byte[] meta = new byte[8192];
        blocks[0] = (byte) (id >> 8);
        blocks[1] = (byte) id;
        meta[0] = (byte) (data >> 8);
        meta[1] = (byte) data;

        Map<String, Tag> section = new HashMap<String, Tag>();
        section.put("Y", new ByteTag((byte) 0));
        section.put("Blocks16", new ByteArrayTag(blocks));
        section.put("Data16", new ByteArrayTag(meta));
        return new CompoundTag(section);
    }
}
