package de.danoeh.antennapod.core.util.id3reader;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import de.danoeh.antennapod.core.util.id3reader.model.FrameHeader;
import de.danoeh.antennapod.core.util.id3reader.model.TagHeader;

/**
 * Reads the ID3 Tag of a given file. In order to use this class, you should
 * create a subclass of it and overwrite the onStart* - or onEnd* - methods.
 */
public class ID3Reader {
	private static final int HEADER_LENGTH = 10;
	private static final int ID3_LENGTH = 3;
	private static final int FRAME_ID_LENGTH = 4;

	private static final int ACTION_SKIP = 1;
	static final int ACTION_DONT_SKIP = 2;

	private int readerPosition;

	private static final byte ENCODING_UTF16_WITH_BOM = 1;
    private static final byte ENCODING_UTF16_WITHOUT_BOM = 2;
    private static final byte ENCODING_UTF8 = 3;

    private TagHeader tagHeader;

	ID3Reader() {
	}

	public final void readInputStream(InputStream input) throws IOException,
			ID3ReaderException {
		int rc;
		readerPosition = 0;
		char[] tagHeaderSource = readBytes(input, HEADER_LENGTH);
		tagHeader = createTagHeader(tagHeaderSource);
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

							if (frameHeader.getSize() + readerPosition > tagHeader
									.getSize()) {
								break;
							} else {
								skipBytes(input, frameHeader.getSize());
							}
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
    char[] readBytes(InputStream input, int number)
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
    void skipBytes(InputStream input, int number) throws IOException {
		if (number <= 0) {
			number = 1;
		}
		IOUtils.skipFully(input, number);

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
			String id = new String(source, 0, ID3_LENGTH);
			char version = (char) ((source[3] << 8) | source[4]);
			byte flags = (byte) source[5];
			int size = (source[6] << 24) | (source[7] << 16) | (source[8] << 8)
					| source[9];
            size = unsynchsafe(size);
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
		String id = new String(source, 0, FRAME_ID_LENGTH);

        int size = (((int) source[4]) << 24) | (((int) source[5]) << 16)
                    | (((int) source[6]) << 8) | source[7];
        if (tagHeader != null && tagHeader.getVersion() >= 0x0400) {
            size = unsynchsafe(size);
        }
		char flags = (char) ((source[8] << 8) | source[9]);
		return new FrameHeader(id, size, flags);
	}

    private int unsynchsafe(int in) {
        int out = 0;
        int mask = 0x7F000000;

        while (mask != 0) {
            out >>= 1;
            out |= in & mask;
            mask >>= 8;
        }

        return out;
    }

	protected int readString(StringBuilder buffer, InputStream input, int max) throws IOException,
			ID3ReaderException {
		if (max > 0) {
			char[] encoding = readBytes(input, 1);
			max--;
			
			if (encoding[0] == ENCODING_UTF16_WITH_BOM || encoding[0] == ENCODING_UTF16_WITHOUT_BOM) {
                return readUnicodeString(buffer, input, max, Charset.forName("UTF-16")) + 1; // take encoding byte into account
			} else if (encoding[0] == ENCODING_UTF8) {
                return readUnicodeString(buffer, input, max, Charset.forName("UTF-8")) + 1; // take encoding byte into account
            } else {
				return readISOString(buffer, input, max) + 1; // take encoding byte into account
			}
		} else {
            if (buffer != null) {
                buffer.append("");
            }
			return 0;
		}
	}

	protected int readISOString(StringBuilder buffer, InputStream input, int max)
			throws IOException, ID3ReaderException {
		int bytesRead = 0;
		char c;
		while (++bytesRead <= max && (c = (char) input.read()) > 0) {
            if (buffer != null) {
			    buffer.append(c);
            }
		}
		return bytesRead;
	}

	private int readUnicodeString(StringBuilder strBuffer, InputStream input, int max, Charset charset)
			throws IOException, ID3ReaderException {
		byte[] buffer = new byte[max];
        int c, cZero = -1;
        int i = 0;
        for (; i < max; i++) {
            c = input.read();
            if (c == -1) {
                break;
            } else if (c == 0) {
                if (cZero == 0) {
                    // termination character found
                    break;
                } else {
                    cZero = 0;
                }
            } else {
                buffer[i] = (byte) c;
                cZero = -1;
            }
        }
        if (strBuffer != null) {
		    strBuffer.append(charset.newDecoder().decode(ByteBuffer.wrap(buffer)).toString());
        }
        return i;
	}

	int onStartTagHeader(TagHeader header) {
		return ACTION_SKIP;
	}

	int onStartFrameHeader(FrameHeader header, InputStream input)
			throws IOException, ID3ReaderException {
		return ACTION_SKIP;
	}

	void onEndTag() {

	}

	void onNoTagHeaderFound() {

	}

}
