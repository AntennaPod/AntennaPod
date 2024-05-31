package de.danoeh.antennapod.parser.media.vorbis;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;


public class VorbisInputStream extends FilterInputStream {
    // capture_pattern + stream_structure_version
    private static final int[] REST_PATTERN = {'g', 'g', 'S', 0};
    private static final int HEADER_SKIP_LENGTH = 1 + 8 + 4 + 4 + 4;

    private final Deque<Integer> buffer = new ArrayDeque<>();
    private final BufferedInputStream bis;


    protected VorbisInputStream(InputStream in) {
        super(in);
        this.bis = new BufferedInputStream(in);
    }


    private int readWithBuffer() throws IOException {
        if (!buffer.isEmpty()) {
            return buffer.removeFirst();
        } else {
            return this.bis.read();
        }
    }


    @Override
    public int read() throws IOException {
        int readByte = readWithBuffer();

        if (readByte == 'O') {
            int[] nextBytes = {
                    readWithBuffer(),
                    readWithBuffer(),
                    readWithBuffer(),
                    readWithBuffer(),
            };

            if (Arrays.equals(nextBytes, REST_PATTERN)) {
                IOUtils.skipFully(this, HEADER_SKIP_LENGTH);

                // skip segment_table
                int pageSegments = this.bis.read();
                IOUtils.skipFully(this, pageSegments);

                readByte = this.bis.read(); // read new byte
            } else {
                // false positive, put read bytes back into buffer
                for (int b : nextBytes) {
                    this.buffer.add(b);
                }
            }
        }

        return readByte;
    }


    // called by IOUtils.skipFully
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int i;
        for (i = 0; i < len; i++) {
            int val = this.read();
            if (val == -1) {
                break;
            }

            b[i] = (byte) (val & 0xff);
        }

        return i;
    }
}
