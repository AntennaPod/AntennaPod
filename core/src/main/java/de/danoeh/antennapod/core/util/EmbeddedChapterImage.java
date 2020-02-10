package de.danoeh.antennapod.core.util;

import de.danoeh.antennapod.core.util.playback.Playable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmbeddedChapterImage {
    private static final Pattern EMBEDDED_IMAGE_MATCHER = Pattern.compile("embedded-image://(.*)@(\\d+):(\\d+)");
    final String mime;
    final int position;
    final int length;
    final Playable media;

    public EmbeddedChapterImage(Playable media, String imageUrl) {
        this.media = media;
        Matcher m = EMBEDDED_IMAGE_MATCHER.matcher(imageUrl);
        if (m.find()) {
            this.mime = m.group(1);
            this.position = Integer.parseInt(m.group(2));
            this.length = Integer.parseInt(m.group(3));
        } else {
            throw new IllegalArgumentException("Not an embedded chapter");
        }
    }

    public static String makeUrl(String mime, int position, int length) {
        return "embedded-image://" + mime + "@" + position + ":" + length;
    }

    public String getMime() {
        return mime;
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
