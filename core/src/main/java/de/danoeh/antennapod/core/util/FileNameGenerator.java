package de.danoeh.antennapod.core.util;

import java.util.Arrays;

/** Generates valid filenames for a given string. */
public class FileNameGenerator {
	
	private static final char[] ILLEGAL_CHARACTERS = { '/', '\\', '?', '%',
			'*', ':', '|', '"', '<', '>', '\n' };
	static {
		Arrays.sort(ILLEGAL_CHARACTERS);
	}
	
	private FileNameGenerator() {

	}

	/**
	 * This method will return a new string that doesn't contain any illegal
	 * characters of the given string.
	 */
	public static String generateFileName(String string) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			if (Arrays.binarySearch(ILLEGAL_CHARACTERS, c) < 0) {
				builder.append(c);
			}
		}
		return builder.toString().replaceFirst("  *$","");
	}

	public static long generateLong(final String str) {
		return str.hashCode();
	}
}
