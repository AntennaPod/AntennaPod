package de.danoeh.antennapod.parser.media;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public final class MediaFormatDetector {
    private static final int PREFIX_READ_LIMIT = 64;

    private MediaFormatDetector() {
    }

    public enum Format {
        ID3, OGG, M4A, UNKNOWN
    }

    public static Result detect(InputStream input) throws IOException {
        byte[] prefix = new byte[PREFIX_READ_LIMIT];
        int bytesRead = input.read(prefix);
        if (bytesRead > 0) {
            prefix = Arrays.copyOf(prefix, bytesRead);
            return new Result(detectFormat(prefix), prefix);
        } else {
            return new Result(Format.UNKNOWN, new byte[0]);
        }
    }

    static Format detectFormat(byte[] prefix) {
        if (prefix.length >= 3
                && prefix[0] == 0x49 && prefix[1] == 0x44 && prefix[2] == 0x33) {
            return Format.ID3;
        } else if (prefix.length >= 4
                && prefix[0] == 0x4F && prefix[1] == 0x67
                && prefix[2] == 0x67 && prefix[3] == 0x53) {
            return Format.OGG;
        } else if (prefix.length >= 8
                && prefix[4] == 0x66 && prefix[5] == 0x74
                && prefix[6] == 0x79 && prefix[7] == 0x70) {
            return Format.M4A;
        }
        return Format.UNKNOWN;
    }

    public static class Result {
        public final Format format;
        public final byte[] bytes;

        Result(Format format, byte[] bytes) {
            this.format = format;
            this.bytes = bytes;
        }
    }
}
