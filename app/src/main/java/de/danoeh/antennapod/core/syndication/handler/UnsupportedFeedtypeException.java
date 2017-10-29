package de.danoeh.antennapod.core.syndication.handler;

import de.danoeh.antennapod.core.syndication.handler.TypeGetter.Type;

public class UnsupportedFeedtypeException extends Exception {
	private static final long serialVersionUID = 9105878964928170669L;
	private Type type;
    private String rootElement;

	public UnsupportedFeedtypeException(Type type) {
		super();
		this.type = type;
	}

    public UnsupportedFeedtypeException(Type type, String rootElement) {
        this.type = type;
        this.rootElement = rootElement;
    }

    public Type getType() {
		return type;
	}

    public String getRootElement() {
        return rootElement;
    }

    @Override
	public String getMessage() {
		if (type == Type.INVALID) {
			return "Invalid type";
		} else {
			return "Type " + type + " not supported";
		}
	}
	
	
}
