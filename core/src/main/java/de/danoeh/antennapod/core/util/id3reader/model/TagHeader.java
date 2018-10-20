package de.danoeh.antennapod.core.util.id3reader.model;

public class TagHeader extends Header {

	private final char version;
	private final byte flags;

	public TagHeader(String id, int size, char version, byte flags) {
		super(id, size);
		this.version = version;
		this.flags = flags;
	}

	@Override
	public String toString() {
		return "TagHeader [version=" + version + ", flags=" + flags + ", id="
				+ id + ", size=" + size + "]";
	}

	public char getVersion() {
		return version;
	}

	

}
