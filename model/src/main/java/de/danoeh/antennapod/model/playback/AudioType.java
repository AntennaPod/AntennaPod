package de.danoeh.antennapod.model.playback;

import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AudioType extends MediaType {

    private static final Set<String> AUDIO_APPLICATION_MIME_STRINGS = new HashSet<>(Arrays.asList(
            "application/ogg",
            "application/opus",
            "application/x-flac"
    ));

    @Override
    public boolean matches(String mimeType) {
        return mimeType != null && (mimeType.startsWith("audio") || AUDIO_APPLICATION_MIME_STRINGS.contains(mimeType));
    }
}
