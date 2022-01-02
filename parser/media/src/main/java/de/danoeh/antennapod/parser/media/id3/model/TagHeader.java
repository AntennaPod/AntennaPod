package de.danoeh.antennapod.parser.media.id3.model;

import androidx.annotation.NonNull;

public class TagHeader extends Header {
    private final short version;
    private final byte flags;

    public TagHeader(String id, int size, short version, byte flags) {
        super(id, size);
        this.version = version;
        this.flags = flags;
    }

    @Override
    @NonNull
    public String toString() {
        return "TagHeader [version=" + version + ", flags=" + flags + ", id="
                + id + ", size=" + size + "]";
    }

    public short getVersion() {
        return version;
    }
}
