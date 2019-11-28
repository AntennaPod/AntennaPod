package de.danoeh.antennapod.core.util;

import android.text.TextUtils;

import org.apache.commons.lang3.ArrayUtils;


/** Generates valid filenames for a given string. */
public class FileNameGenerator {

    private static final char[] validChars =
            ("abcdefghijklmnopqrstuvwxyz"
            + "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "0123456789"
            + " _-").toCharArray();

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
            if (Character.isSpaceChar(c)
                    && (buf.length() == 0 || Character.isSpaceChar(buf.charAt(buf.length() - 1)))) {
                continue;
            }
            if (ArrayUtils.contains(validChars, c)) {
                buf.append(c);
            }
        }
        String filename = buf.toString().trim();
        if (TextUtils.isEmpty(filename)) {
            return randomString(8);
        }
        return filename;
    }

    private static String randomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(validChars[(int) (Math.random() * validChars.length)]);
        }
        return sb.toString();
    }
}
