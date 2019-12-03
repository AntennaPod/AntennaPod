package de.danoeh.antennapodSA.core.syndication.handler;

import de.danoeh.antennapodSA.core.syndication.handler.TypeGetter.Type;

public class UnsupportedFeedtypeException extends Exception {
    private static final long serialVersionUID = 9105878964928170669L;
    private final Type type;
    private String rootElement;
    private String message = null;

    public UnsupportedFeedtypeException(Type type) {
        super();
        this.type = type;
    }

    public UnsupportedFeedtypeException(Type type, String rootElement) {
        this.type = type;
        this.rootElement = rootElement;
    }

    public UnsupportedFeedtypeException(String message) {
        this.message = message;
        type = Type.INVALID;
    }

    public Type getType() {
        return type;
    }

    public String getRootElement() {
        return rootElement;
    }

    @Override
    public String getMessage() {
        if (message != null) {
            return message;
        } else if (type == Type.INVALID) {
            return "Invalid type";
        } else {
            return "Type " + type + " not supported";
        }
    }
}
