package de.danoeh.antennapod.parser.media.vorbis;

import java.io.IOException;
import java.io.InputStream;

public class VorbisCommentMetadataReader extends VorbisCommentReader {
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_COMMENT = "comment";
    private static final String KEY_SYNOPSIS = "synopsis";

    private String description = null;

    public VorbisCommentMetadataReader(InputStream input) {
        super(input);
    }

    @Override
    protected void onContentVector(String key) throws IOException, VorbisCommentReaderException {
        if (KEY_DESCRIPTION.equals(key) || KEY_COMMENT.equals(key) || KEY_SYNOPSIS.equals(key)) {
            String value = readValue();
            if (description == null || value.length() > description.length()) {
                description = value;
            }
        } else {
            super.onContentVector(key);
        }
    }

    public String getDescription() {
        return description;
    }
}
