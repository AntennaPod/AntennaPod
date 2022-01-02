package de.danoeh.antennapod.parser.media.vorbis;

public class VorbisCommentReaderException extends Exception {
    private static final long serialVersionUID = 1L;

    public VorbisCommentReaderException() {
        super();
    }

    public VorbisCommentReaderException(String message, Throwable cause) {
        super(message, cause);
    }

    public VorbisCommentReaderException(String message) {
        super(message);
    }

    public VorbisCommentReaderException(Throwable message) {
        super(message);
    }
}
