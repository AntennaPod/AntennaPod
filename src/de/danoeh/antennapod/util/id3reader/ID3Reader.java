package de.danoeh.antennapod.util.id3reader;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

import de.danoeh.antennapod.util.id3reader.model.FrameHeader;
import de.danoeh.antennapod.util.id3reader.model.TagHeader;

/**
 * Reads the ID3 Tag of a given file. In order to use this class, you should
 * create a subclass of it and overwrite the onStart* - or onEnd* - methods.
 */
public class ID3Reader {
	private static final int HEADER_LENGTH = 10;
	private static final int ID3_LENGTH = 3;
	private static final int FRAME_ID_LENGTH = 4;

	protected static final int ACTION_SKIP = 1;
	protected static final int ACTION_DONT_SKIP = 2;

	protected int readerPosition;

	private static final char[] LITTLE_ENDIAN_BOM = { 0xff, 0xfe };
	private static final char[] BIG_ENDIAN_BOM = { 0xfe, 0xff };

	public ID3Reader() {
	}

	public final void readInputStream(InputStream input) throws IOException,
			ID3ReaderException {
		int rc;
		readerPosition = 0;
		char[] tagHeaderSource = readBytes(input, HEADER_LENGTH);
		TagHeader tagHeader = createTagHeader(tagHeaderSource);
		if (tagHeader == null) {
			onNoTagHeaderFound();
		} else {
			rc = onStartTagHeader(tagHeader);
			if (rc == ACTION_SKIP) {
				onEndTag();
			} else {
				while (readerPosition < tagHeader.getSize()) {
					FrameHeader frameHeader = createFrameHeader(readBytes(
							input, HEADER_LENGTH));
					if (checkForNullString(frameHeader.getId())) {
						break;
					} else {
						rc = onStartFrameHeader(frameHeader, input);
						if (rc == ACTION_SKIP) {
							skipBytes(input, frameHeader.getSize());
						}
					}
				}
				onEndTag();
			}
		}
	}

	/** Returns true if string only contains null-bytes. */
	private boolean checkForNullString(String s) {
		if (!s.isEmpty()) {
			int i = 0;
			if (s.charAt(i) == 0) {
				for (i = 1; i < s.length(); i++) {
					if (s.charAt(i) != 0) {
						return false;
					}
				}
				return true;
			}
			return false;
		} else {
			return true;
		}

	}

	/**
	 * Read a certain number of bytes from the given input stream. This method
	 * changes the readerPosition-attribute.
	 */
	protected char[] readBytes(InputStream input, int number)
			throws IOException, ID3ReaderException {
		char[] header = new char[number];
		for (int i = 0; i < number; i++) {
			int b = input.read();
			readerPosition++;
			if (b != -1) {
				header[i] = (char) b;
			} else {
				throw new ID3ReaderException("Unexpected end of stream");
			}
		}
		return header;
	}

	/**
	 * Skip a certain number of bytes on the given input stream. This method
	 * changes the readerPosition-attribute.
	 */
	protected void skipBytes(InputStream input, int number) throws IOException {
		int skipped = 0;
		while (skipped < number) {
			skipped += input.skip(number - skipped);
		}

		readerPosition += number;
	}

	private TagHeader createTagHeader(char[] source) throws ID3ReaderException {
		boolean hasTag = (source[0] == 0x49) && (source[1] == 0x44)
				&& (source[2] == 0x33);
		if (source.length != HEADER_LENGTH) {
			throw new ID3ReaderException("Length of header must be "
					+ HEADER_LENGTH);
		}
		if (hasTag) {
			String id = null;
			id = new String(source, 0, ID3_LENGTH);
			char version = (char) ((source[3] << 8) | source[4]);
			byte flags = (byte) source[5];
			int size = (source[6] << 24) | (source[7] << 16) | (source[8] << 8)
					| source[9];
			return new TagHeader(id, size, version, flags);
		} else {
			return null;
		}
	}

	private FrameHeader createFrameHeader(char[] source)
			throws ID3ReaderException {
		if (source.length != HEADER_LENGTH) {
			throw new ID3ReaderException("Length of header must be "
					+ HEADER_LENGTH);
		}
		String id = null;
		id = new String(source, 0, FRAME_ID_LENGTH);
		int size = (((int) source[4]) << 24) | (((int) source[5]) << 16)
				| (((int) source[6]) << 8) | source[7];
		char flags = (char) ((source[8] << 8) | source[9]);
		return new FrameHeader(id, size, flags);
	}

	protected String readString(InputStream input, int max) throws IOException,
			ID3ReaderException {
		char[] bom = readBytes(input, 2);
		if (bom == LITTLE_ENDIAN_BOM || bom == BIG_ENDIAN_BOM) {
			return readUnicodeString(input, bom, max);
		} else {
			PushbackInputStream pi = new PushbackInputStream(input, 2);
			pi.unread(bom[1]);
			pi.unread(bom[0]);
			return readISOString(pi, max);
		}
	}

	private String readISOString(InputStream input, int max)
			throws IOException, ID3ReaderException {
		int bytesRead = 0;
		StringBuilder builder = new StringBuilder();
		char c;
		while (++bytesRead <= max && (c = (char) input.read()) > 0) {
			builder.append(c);
		}
		return builder.toString();
	}

	private String readUnicodeString(InputStream input, char[] bom, int max)
			throws IOException, ID3ReaderException {
		StringBuffer builder = new StringBuffer();
		char c1 = (char) input.read();
		char c2 = (char) input.read();
		int bytesRead = 2;
		while ((c1 > 0 && c2 > 0) && ++bytesRead <= max) {

			builder.append(c1);
			c1 = c2;
			c2 = (char) input.read();
		}
		if (bom == LITTLE_ENDIAN_BOM) {
			builder.reverse();
		}
		return builder.toString();
	}

	public int onStartTagHeader(TagHeader header) {
		return ACTION_SKIP;
	}

	public int onStartFrameHeader(FrameHeader header, InputStream input)
			throws IOException, ID3ReaderException {
		return ACTION_SKIP;
	}

	public void onEndTag() {

	}

	public void onNoTagHeaderFound() {

	}

}
