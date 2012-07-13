package de.danoeh.antennapod.syndication.handler;

import de.danoeh.antennapod.syndication.handler.TypeGetter.Type;

public class UnsupportedFeedtypeException extends Exception {
	private static final long serialVersionUID = 9105878964928170669L;
	private TypeGetter.Type type;
	
	public UnsupportedFeedtypeException(Type type) {
		super();
		this.type = type;
		
	}
	
	public TypeGetter.Type getType() {
		return type;
	}
	
	@Override
	public String getMessage() {
		if (type == TypeGetter.Type.INVALID) {
			return "Invalid type";
		} else {
			return "Type " + type + " not supported";
		}
	}
	
	
}
