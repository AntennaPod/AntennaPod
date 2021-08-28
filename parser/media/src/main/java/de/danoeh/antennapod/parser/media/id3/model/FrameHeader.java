package de.danoeh.antennapod.parser.media.id3.model;

import androidx.annotation.NonNull;

public class FrameHeader extends Header {
    private final short flags;

    public FrameHeader(String id, int size, short flags) {
        super(id, size);
        this.flags = flags;
    }

    @Override
    @NonNull
    public String toString() {
        return String.format("FrameHeader [flags=%s, id=%s, size=%s]", Integer.toBinaryString(flags), id, size);
    }
}
