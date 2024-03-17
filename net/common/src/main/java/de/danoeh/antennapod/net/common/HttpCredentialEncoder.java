package de.danoeh.antennapod.net.common;

import android.util.Base64;

import java.io.UnsupportedEncodingException;

public abstract class HttpCredentialEncoder {
    public static String encode(String username, String password, String charset) {
        try {
            String credentials = username + ":" + password;
            byte[] bytes = credentials.getBytes(charset);
            String encoded = Base64.encodeToString(bytes, Base64.NO_WRAP);
            return "Basic " + encoded;
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }
}
