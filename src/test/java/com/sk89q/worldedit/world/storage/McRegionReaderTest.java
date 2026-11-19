package com.sk89q.worldedit.world.storage;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.zip.DeflaterOutputStream;

import org.junit.Test;

import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.util.io.ForwardSeekableInputStream;

public class McRegionReaderTest {

    /** Newer Java versions read through this method, even for a single readInt(). */
    @Test
    public void bulkReadsCountEachByteOnce() throws Exception {
        byte[] bytes = new byte[32];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) i;
        }
        ForwardSeekableInputStream stream = new ForwardSeekableInputStream(new ByteArrayInputStream(bytes));

        assertEquals(4, stream.read(new byte[4], 0, 4));
        stream.seek(8);

        assertEquals(8, stream.read());
    }

    @Test
    public void readsChunkFromAStreamThatOnlyReturnsPartsAtATime() throws Exception {
        byte[] chunk = new byte[20000];
        new Random(1).nextBytes(chunk);

        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        DeflaterOutputStream deflater = new DeflaterOutputStream(compressed);
        deflater.write(chunk);
        deflater.close();

        // Chunk 16,12 at sector 921, like the chunk in the backup that exposed this.
        ByteArrayOutputStream region = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(region);
        for (int i = 0; i < 1024; i++) {
            out.writeInt(i == 16 + 12 * 32 ? 921 << 8 | (compressed.size() / 4096 + 1) : 0);
        }
        out.write(new byte[920 * 4096]);
        out.writeInt(compressed.size() + 1);
        out.writeByte(2);
        out.write(compressed.toByteArray());

        McRegionReader reader = new McRegionReader(new TricklingStream(new ByteArrayInputStream(region.toByteArray())));
        InputStream result = reader.getChunkInputStream(new Vector2D(16, 12));

        ByteArrayOutputStream read = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int count;
        while ((count = result.read(buffer)) != -1) {
            read.write(buffer, 0, count);
        }
        assertArrayEquals(chunk, read.toByteArray());
    }

    /** Behaves like a zip entry's stream, which is allowed to return less than was asked for. */
    private static final class TricklingStream extends FilterInputStream {

        private TricklingStream(InputStream in) {
            super(in);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return super.read(b, off, Math.min(len, 7));
        }

        @Override
        public long skip(long n) throws IOException {
            return super.skip(Math.min(n, 512));
        }
    }
}
