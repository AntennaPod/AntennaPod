package de.danoeh.antennapod.core.util;

import android.text.TextUtils;
import de.danoeh.antennapod.model.playback.Playable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmbeddedChapterImage {
    private static final Pattern EMBEDDED_IMAGE_MATCHER = Pattern.compile("embedded-image://(\\d+)/(\\d+)");

    private final int position;
    private final int length;
    private final String imageUrl;
    private final Playable media;

    public EmbeddedChapterImage(Playable media, String imageUrl) {
        this.media = media;
        this.imageUrl = imageUrl;
        Matcher m = EMBEDDED_IMAGE_MATCHER.matcher(imageUrl);
        if (m.find()) {
            this.position = Integer.parseInt(m.group(1));
            this.length = Integer.parseInt(m.group(2));
        } else {
            throw new IllegalArgumentException("Not an embedded chapter");
        }
    }

    public static String makeUrl(int position, int length) {
        return "embedded-image://" + position + "/" + length;
    }

    public int getPosition() {
        return position;
    }

    public int getLength() {
        return length;
    }

    public Playable getMedia() {
        return media;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EmbeddedChapterImage that = (EmbeddedChapterImage) o;
        return TextUtils.equals(imageUrl, that.imageUrl);
    }

    @Override
    public int hashCode() {
        return imageUrl.hashCode();
    }

    private static boolean isEmbeddedChapterImage(String imageUrl) {
        return EMBEDDED_IMAGE_MATCHER.matcher(imageUrl).matches();
    }

    public static Object getModelFor(Playable media, int chapter) {
        String imageUrl = media.getChapters().get(chapter).getImageUrl();
        if (isEmbeddedChapterImage(imageUrl)) {
            return new EmbeddedChapterImage(media, imageUrl);
        } else {
            return imageUrl;
        }
    }
}
