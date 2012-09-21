package de.danoeh.antennapod.util.id3reader.model;

public class FrameHeader extends Header {

	protected char flags;

	public FrameHeader(String id, int size, char flags) {
		super(id, size);
		this.flags = flags;
	}

	@Override
	public String toString() {
		return "FrameHeader [flags=" + Integer.toString(flags) + ", id=" + id + ", size=" + size
				+ "]";
	}

}
