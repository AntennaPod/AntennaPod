package de.danoeh.antennapod.core.util.vorbiscommentreader;
public class VorbisCommentHeader {
	private String vendorString;
	private long userCommentLength;

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
