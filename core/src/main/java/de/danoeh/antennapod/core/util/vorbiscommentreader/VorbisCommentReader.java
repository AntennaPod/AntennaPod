package de.danoeh.antennapod.core.util.vorbiscommentreader;

import org.apache.commons.io.EndianUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;


public abstract class VorbisCommentReader {
	/** Length of first page in an ogg file in bytes. */
	private static final int FIRST_PAGE_LENGTH = 58;
	private static final int SECOND_PAGE_MAX_LENGTH = 64 * 1024 * 1024;
	private static final int PACKET_TYPE_IDENTIFICATION = 1;
	private static final int PACKET_TYPE_COMMENT = 3;

	/** Called when Reader finds identification header. */
	protected abstract void onVorbisCommentFound();

	protected abstract void onVorbisCommentHeaderFound(VorbisCommentHeader header);

	/**
	 * Is called every time the Reader finds a content vector. The handler
	 * should return true if it wants to handle the content vector.
	 */
	protected abstract boolean onContentVectorKey(String content);

	/**
	 * Is called if onContentVectorKey returned true for the key.
	 * 
	 * @throws VorbisCommentReaderException
	 */
	protected abstract void onContentVectorValue(String key, String value)
			throws VorbisCommentReaderException;

	protected abstract void onNoVorbisCommentFound();

	protected abstract void onEndOfComment();

	protected abstract void onError(VorbisCommentReaderException exception);

	public void readInputStream(InputStream input)
			throws VorbisCommentReaderException {
		try {
			// look for identification header
			if (findIdentificationHeader(input)) {
				
				onVorbisCommentFound();
				input = new OggInputStream(input);
				if (findCommentHeader(input)) {
					VorbisCommentHeader commentHeader = readCommentHeader(input);
					if (commentHeader != null) {
						onVorbisCommentHeaderFound(commentHeader);
						for (int i = 0; i < commentHeader
								.getUserCommentLength(); i++) {
							try {
								long vectorLength = EndianUtils
										.readSwappedUnsignedInteger(input);
								String key = readContentVectorKey(input,
										vectorLength).toLowerCase();
								boolean readValue = onContentVectorKey(key);
								if (readValue) {
									String value = readUTF8String(
											input,
											(int) (vectorLength - key.length() - 1));
									onContentVectorValue(key, value);
								} else {
									IOUtils.skipFully(input,
											vectorLength - key.length() - 1);
								}
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						onEndOfComment();
					}

				} else {
					onError(new VorbisCommentReaderException(
							"No comment header found"));
				}
			} else {
				onNoVorbisCommentFound();
			}
		} catch (IOException e) {
			onError(new VorbisCommentReaderException(e));
		}
	}

	private String readUTF8String(InputStream input, long length)
			throws IOException {
		byte[] buffer = new byte[(int) length];
		
		IOUtils.readFully(input, buffer);
		Charset charset = Charset.forName("UTF-8");
		return charset.newDecoder().decode(ByteBuffer.wrap(buffer)).toString();
	}

	/**
	 * Looks for an identification header in the first page of the file. If an
	 * identification header is found, it will be skipped completely and the
	 * method will return true, otherwise false.
	 * 
	 * @throws IOException
	 */
	private boolean findIdentificationHeader(InputStream input)
			throws IOException {
		byte[] buffer = new byte[FIRST_PAGE_LENGTH];
		IOUtils.readFully(input, buffer);
		int i;
		for (i = 6; i < buffer.length; i++) {
			if (buffer[i - 5] == 'v' && buffer[i - 4] == 'o'
					&& buffer[i - 3] == 'r' && buffer[i - 2] == 'b'
					&& buffer[i - 1] == 'i' && buffer[i] == 's'
					&& buffer[i - 6] == PACKET_TYPE_IDENTIFICATION) {
				return true;
			}
		}
		return false;
	}

	private boolean findCommentHeader(InputStream input) throws IOException {
		char[] buffer = new char["vorbis".length() + 1];
		for (int bytesRead = 0; bytesRead < SECOND_PAGE_MAX_LENGTH; bytesRead++) {
			char c = (char) input.read();
			int dest = -1;
			switch (c) {
			case PACKET_TYPE_COMMENT:
				dest = 0;
				break;
			case 'v':
				dest = 1;
				break;
			case 'o':
				dest = 2;
				break;
			case 'r':
				dest = 3;
				break;
			case 'b':
				dest = 4;
				break;
			case 'i':
				dest = 5;
				break;
			case 's':
				dest = 6;
				break;
			}
			if (dest >= 0) {
				buffer[dest] = c;
				if (buffer[1] == 'v' && buffer[2] == 'o' && buffer[3] == 'r'
						&& buffer[4] == 'b' && buffer[5] == 'i'
						&& buffer[6] == 's' && buffer[0] == PACKET_TYPE_COMMENT) {
					return true;
				}
			} else {
				Arrays.fill(buffer, (char) 0);
			}
		}
		return false;
	}

	private VorbisCommentHeader readCommentHeader(InputStream input)
			throws IOException, VorbisCommentReaderException {
		try {
			long vendorLength = EndianUtils.readSwappedUnsignedInteger(input);
			String vendorName = readUTF8String(input, vendorLength);
			long userCommentLength = EndianUtils
					.readSwappedUnsignedInteger(input);
			return new VorbisCommentHeader(vendorName, userCommentLength);
		} catch (UnsupportedEncodingException e) {
			throw new VorbisCommentReaderException(e);
		}
	}

	private String readContentVectorKey(InputStream input, long vectorLength)
			throws IOException {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < vectorLength; i++) {
			char c = (char) input.read();
			if (c == '=') {
				return builder.toString();
			} else {
				builder.append(c);
			}
		}
		return null; // no key found
	}
}
