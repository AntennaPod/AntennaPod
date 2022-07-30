package de.danoeh.antennapod.parser.media.vorbis;

import java.io.InputStream;

public class VorbisCommentMetadataReader extends VorbisCommentReader {
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_COMMENT = "comment";

    private String description = null;

    public VorbisCommentMetadataReader(InputStream input) {
        super(input);
    }

    @Override
    public boolean handles(String key) {
        return KEY_DESCRIPTION.equals(key) || KEY_COMMENT.equals(key);
    }

    @Override
    public void onContentVectorValue(String key, String value) {
        if (KEY_DESCRIPTION.equals(key) || KEY_COMMENT.equals(key)) {
            if (description == null || value.length() > description.length()) {
                description = value;
            }
        }
    }

    public String getDescription() {
        return description;
    }
}
