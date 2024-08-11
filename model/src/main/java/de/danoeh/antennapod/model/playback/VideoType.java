package de.danoeh.antennapod.model.playback;

import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class VideoType extends MediaType {

    @Override
    public boolean matches(String mimeType) {
        return mimeType != null && mimeType.startsWith("video");
    }
}