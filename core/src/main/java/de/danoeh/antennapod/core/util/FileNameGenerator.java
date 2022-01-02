package de.danoeh.antennapod.core.util;

import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;


/** Generates valid filenames for a given string. */
public class FileNameGenerator {
    @VisibleForTesting
    public static final int MAX_FILENAME_LENGTH = 242; // limited by CircleCI
    private static final int MD5_HEX_LENGTH = 32;

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
        string = StringUtils.stripAccents(string);
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
        } else if (filename.length() >= MAX_FILENAME_LENGTH) {
            return filename.substring(0, MAX_FILENAME_LENGTH - MD5_HEX_LENGTH - 1) + "_" + md5(filename);
        } else {
            return filename;
        }
    }

    private static String randomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(validChars[(int) (Math.random() * validChars.length)]);
        }
        return sb.toString();
    }

    private static String md5(String md5) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : array) {
                sb.append(Integer.toHexString((b & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            return null;
        }
    }
}
