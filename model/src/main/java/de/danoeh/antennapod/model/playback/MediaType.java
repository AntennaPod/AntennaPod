package de.danoeh.antennapod.model.playback;

import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


public abstract class MediaType {
    public abstract boolean matches(String mimeType);

    public static MediaType AUDIO = new AudioType();
    public static MediaType VIDEO = new VideoType();
    public static MediaType UNKNOWN = new UnknownType();

    public static MediaType fromMimeType(String mimeType) {
        if (AUDIO.matches(mimeType)) {
            return AUDIO;
        } else if (VIDEO.matches(mimeType)) {
            return VIDEO;
        }
        return UNKNOWN;
    }
}