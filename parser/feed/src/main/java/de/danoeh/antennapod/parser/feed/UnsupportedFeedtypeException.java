package de.danoeh.antennapod.parser.feed;

public class UnsupportedFeedtypeException extends Exception {
    private static final long serialVersionUID = 9105878964928170669L;
    private String rootElement;
    private String message;

    public UnsupportedFeedtypeException(String rootElement, String message) {
        this.rootElement = rootElement;
        this.message = message;
    }

    public UnsupportedFeedtypeException(String message) {
        this.message = message;
    }

    public String getRootElement() {
        return rootElement;
    }

    @Override
    public String getMessage() {
        if (message != null) {
            return message;
        } else if (rootElement != null) {
            return "Server returned " + rootElement;
        } else {
            return "Unknown type";
        }
    }
}
