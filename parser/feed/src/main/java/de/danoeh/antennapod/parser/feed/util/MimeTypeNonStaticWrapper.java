package de.danoeh.antennapod.parser.feed.util;

public class MimeTypeNonStaticWrapper {
    public String getMimeTypeFromUrl(String url) {
        return MimeTypeUtils.getMimeTypeFromUrl(url);
    }
}
