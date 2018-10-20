package de.danoeh.antennapod.core.util.id3reader.model;

public class FrameHeader extends Header {

	private final char flags;

	public FrameHeader(String id, int size, char flags) {
		super(id, size);
		this.flags = flags;
	}

	@Override
	public String toString() {
        return String.format("FrameHeader [flags=%s, id=%s, size=%s]", Integer.toBinaryString(flags), id, Integer.toBinaryString(size));
    }

}
