package de.danoeh.antennapod.core.util.id3reader;

public class ID3ReaderException extends Exception {
    private static final long serialVersionUID = 1L;

    public ID3ReaderException() {
    }

    public ID3ReaderException(String message) {
        super(message);
    }

    public ID3ReaderException(Throwable message) {
        super(message);
    }

    public ID3ReaderException(String message, Throwable cause) {
        super(message, cause);
    }
}
