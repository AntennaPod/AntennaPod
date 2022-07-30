package de.danoeh.antennapod.parser.media.id3;

import androidx.annotation.NonNull;
import de.danoeh.antennapod.parser.media.id3.model.FrameHeader;
import org.apache.commons.io.input.CountingInputStream;

import java.io.IOException;

/**
 * Reads general ID3 metadata like comment, which Android's MediaMetadataReceiver does not support.
 */
public class Id3MetadataReader extends ID3Reader {
    public static final String FRAME_ID_COMMENT = "COMM";

    private String comment = null;

    public Id3MetadataReader(CountingInputStream input) {
        super(input);
    }

    @Override
    protected void readFrame(@NonNull FrameHeader frameHeader) throws IOException, ID3ReaderException {
        if (FRAME_ID_COMMENT.equals(frameHeader.getId())) {
            long frameStart = getPosition();
            int encoding = readByte();
            skipBytes(3); // Language
            String shortDescription = readEncodedString(encoding, frameHeader.getSize() - 4);
            String longDescription = readEncodedString(encoding,
                    (int) (frameHeader.getSize() - (getPosition() - frameStart)));
            comment = shortDescription.length() > longDescription.length() ? shortDescription : longDescription;
        } else {
            super.readFrame(frameHeader);
        }
    }

    public String getComment() {
        return comment;
    }
}
