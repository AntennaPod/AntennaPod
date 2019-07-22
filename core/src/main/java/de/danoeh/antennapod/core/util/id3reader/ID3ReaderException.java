package de.danoeh.antennapod.core.util.id3reader;

public class ID3ReaderException extends Exception {

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
