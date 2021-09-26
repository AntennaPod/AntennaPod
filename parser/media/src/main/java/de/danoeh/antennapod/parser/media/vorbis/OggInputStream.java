package de.danoeh.antennapod.parser.media.vorbis;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

class OggInputStream extends InputStream {
    private final InputStream input;

    /** True if OggInputStream is currently inside an Ogg page. */
    private boolean isInPage;
    private long bytesLeft;

    public OggInputStream(InputStream input) {
        super();
        isInPage = false;
        this.input = input;
    }

    @Override
    public int read() throws IOException {
        if (!isInPage) {
            readOggPage();
        }
        
        if (isInPage && bytesLeft > 0) {
            int result = input.read();
            bytesLeft -= 1;
            if (bytesLeft == 0) {
                isInPage = false;
            }
            return result;
        } 
        return -1;
    }

    private void readOggPage() throws IOException {
        // find OggS
        int[] buffer = new int[4];
        int c;
        boolean isInOggS = false;
        while ((c = input.read()) != -1) {
            switch (c) {
                case 'O':
                    isInOggS = true;
                    buffer[0] = c;
                    break;
                case 'g':
                    if (buffer[1] != c) {
                        buffer[1] = c;
                    } else {
                        buffer[2] = c;
                    }
                    break;
                case 'S':
                    buffer[3] = c;
                    break;
                default:
                    if (isInOggS) {
                        Arrays.fill(buffer, 0);
                        isInOggS = false;
                    }
            }
            if (buffer[0] == 'O' && buffer[1] == 'g' && buffer[2] == 'g'
                    && buffer[3] == 'S') {
                break;
            }
        }
        // read segments
        IOUtils.skipFully(input, 22);
        bytesLeft = 0;
        int numSegments = input.read();
        for (int i = 0; i < numSegments; i++) {
            bytesLeft += input.read();
        }
        isInPage = true;
    }

}
