package de.danoeh.antennapod.core.util;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Generates valid filenames for a given string. */
public class FileNameGenerator {
	
	private static final char[] validChars = (
			"abcdefghijklmnopqrstuvwxyz" +
			"ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
			"0123456789" +
			" _-").toCharArray();
	
	private FileNameGenerator() {
	}

	/**
	 * This method will return a new string that doesn't contain any illegal
	 * characters of the given string.
	 */
	public static String generateFileName(String string) {
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			if(Character.isSpaceChar(c) && (buf.length() == 0 || Character.isSpaceChar(buf.charAt(buf.length()-1)))) {
				continue;
			}
			if (ArrayUtils.contains(validChars, c)) {
				buf.append(c);
			}
		}
		return buf.toString().trim();
	}

}
