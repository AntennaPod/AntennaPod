package de.danoeh.antennapod.parser.media.vorbis;

class VorbisCommentHeader {
    private final String vendorString;
    private final long userCommentLength;

    public VorbisCommentHeader(String vendorString, long userCommentLength) {
        super();
        this.vendorString = vendorString;
        this.userCommentLength = userCommentLength;
    }

    @Override
    public String toString() {
        return "VorbisCommentHeader [vendorString=" + vendorString
                + ", userCommentLength=" + userCommentLength + "]";
    }

    public String getVendorString() {
        return vendorString;
    }

    public long getUserCommentLength() {
        return userCommentLength;
    }

}
