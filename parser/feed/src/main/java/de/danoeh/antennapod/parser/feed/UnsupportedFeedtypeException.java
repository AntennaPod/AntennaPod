package de.danoeh.antennapod.parser.feed;

import de.danoeh.antennapod.parser.feed.type.TypeResolver;

public class UnsupportedFeedtypeException extends Exception {
    private static final long serialVersionUID = 9105878964928170669L;
    private final TypeResolver.Type type;
    private String rootElement;
    private String message = null;

    public UnsupportedFeedtypeException(TypeResolver.Type type) {
        super();
        this.type = type;
    }

    public UnsupportedFeedtypeException(TypeResolver.Type type, String rootElement) {
        this.type = type;
        this.rootElement = rootElement;
    }

    public UnsupportedFeedtypeException(String message) {
        this.message = message;
        type = TypeResolver.Type.INVALID;
    }

    public TypeResolver.Type getType() {
        return type;
    }

    public String getRootElement() {
        return rootElement;
    }

    @Override
    public String getMessage() {
        if (message != null) {
            return message;
        } else if (type == TypeResolver.Type.INVALID) {
            return "Invalid type";
        } else {
            return "Type " + type + " not supported";
        }
    }
}
