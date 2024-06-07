package de.danoeh.antennapod.parser.media.vorbis;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;


public class VorbisInputStream extends FilterInputStream {
    private static final byte[] CAPTURE_PATTERN = {'O', 'g', 'g', 'S'};
    private static final int HEADER_SKIP_LENGTH = 1 + 1 + 8 + 4 + 4 + 4;

    private final BufferedInputStream bis;
    private int pageRemainBytes = 0;


    protected VorbisInputStream(InputStream in) {
        super(in);
        this.bis = new BufferedInputStream(in);
    }


    private int parsePageHeader(InputStream in) throws IOException {
        byte[] capturePattern = new byte[4];

        IOUtils.readFully(in, capturePattern, 0, 4);
        if (!Arrays.equals(CAPTURE_PATTERN, capturePattern)) {
            throw new IOException("Invalid page header");
        }

        IOUtils.skipFully(in, HEADER_SKIP_LENGTH);

        int pageSegments = in.read();
        byte[] segmentTable = new byte[pageSegments];
        int pageLength = 0;
        IOUtils.readFully(in, segmentTable);
        for (byte segment:segmentTable) {
            pageLength += (segment & 0xff);
        }

        return pageLength;
    }


    // check and uptate remain bytes
    private void updateRemainBytes() throws IOException {
        if (this.pageRemainBytes == 0) {
            this.pageRemainBytes = this.parsePageHeader(this.bis);
        } else if (this.pageRemainBytes < 0) {
            throw new IOException("Page remain bytes less than 0");
        }
    }



    @Override
    public int read() throws IOException {
        updateRemainBytes();

        this.pageRemainBytes -= 1;
        return this.bis.read();
    }


    // called by IOUtils.skipFully
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        updateRemainBytes();

        int bytesToRead = Math.min(len, this.pageRemainBytes);
        IOUtils.readFully(this.bis, b, off, bytesToRead);

        this.pageRemainBytes -=  bytesToRead;

        return bytesToRead;
    }
}
