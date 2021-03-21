package de.danoeh.antennapod.core.feed;

public class FeedFunding {
    public static final String SUPPORT_INTERNAL_SPLIT = "\u001e";
    public static final String SUPPORT_INTERNAL_EQUAL = "\u001f";

    public String url;
    public String content;

    public FeedFunding(String url, String content) {
        this.url = url;
        this.content = content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}