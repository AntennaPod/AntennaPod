package de.danoeh.antennapod.core.syndication.namespace;

import android.text.TextUtils;
import android.util.Log;

import androidx.core.text.HtmlCompat;

import org.xml.sax.Attributes;

import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.syndication.handler.HandlerState;
import de.danoeh.antennapod.core.syndication.parsers.DurationParser;

public class NSITunes extends Namespace {

    public static final String NSTAG = "itunes";
    public static final String NSURI = "http://www.itunes.com/dtds/podcast-1.0.dtd";

    private static final String IMAGE = "image";
    private static final String IMAGE_HREF = "href";

    private static final String AUTHOR = "author";
    public static final String DURATION = "duration";
    private static final String SUBTITLE = "subtitle";
    private static final String SUMMARY = "summary";

    @Override
    public SyndElement handleElementStart(String localName, HandlerState state,
                                          Attributes attributes) {
        if (IMAGE.equals(localName)) {
            String url = attributes.getValue(IMAGE_HREF);

            if (state.getCurrentItem() != null) {
                state.getCurrentItem().setImageUrl(url);
            } else {
                // this is the feed image
                // prefer to all other images
                if (!TextUtils.isEmpty(url)) {
                    state.getFeed().setImageUrl(url);
                }
            }
        }
        return new SyndElement(localName, this);
    }

    @Override
    public void handleElementEnd(String localName, HandlerState state) {
        if (state.getContentBuf() == null) {
            return;
        }

        if (AUTHOR.equals(localName)) {
            parseAuthor(state);
        } else if (DURATION.equals(localName)) {
            parseDuration(state);
        } else if (SUBTITLE.equals(localName)) {
            parseSubtitle(state);
        } else if (SUMMARY.equals(localName)) {
            SyndElement secondElement = state.getSecondTag();
            parseSummary(state, secondElement.getName());
        }
    }

    private void parseAuthor(HandlerState state) {
        if (state.getFeed() != null) {
            String author = state.getContentBuf().toString();
            state.getFeed().setAuthor(HtmlCompat.fromHtml(author,
                    HtmlCompat.FROM_HTML_MODE_LEGACY).toString());
        }
    }

    private void parseDuration(HandlerState state) {
        String durationStr = state.getContentBuf().toString();
        if (TextUtils.isEmpty(durationStr)) {
            return;
        }

        try {
            long durationMs = DurationParser.inMillis(durationStr);
            state.getTempObjects().put(DURATION, (int) durationMs);
        } catch (NumberFormatException e) {
            Log.e(NSTAG, String.format("Duration '%s' could not be parsed", durationStr));
        }
    }

    private void parseSubtitle(HandlerState state) {
        String subtitle = state.getContentBuf().toString();
        if (TextUtils.isEmpty(subtitle)) {
            return;
        }
        if (state.getCurrentItem() != null) {
            if (TextUtils.isEmpty(state.getCurrentItem().getDescription())) {
                state.getCurrentItem().setDescription(subtitle);
            }
        } else {
            if (state.getFeed() != null && TextUtils.isEmpty(state.getFeed().getDescription())) {
                state.getFeed().setDescription(subtitle);
            }
        }
    }

    private void parseSummary(HandlerState state, String secondElementName) {
        String summary = state.getContentBuf().toString();
        if (TextUtils.isEmpty(summary)) {
            return;
        }

        FeedItem currentItem = state.getCurrentItem();
        String description = getDescription(currentItem);
        if (currentItem != null && description.length() * 1.25 < summary.length()) {
            currentItem.setDescription(summary);
        } else if (NSRSS20.CHANNEL.equals(secondElementName) && state.getFeed() != null) {
            state.getFeed().setDescription(summary);
        }
    }

    private String getDescription(FeedItem item) {
        return (item != null && item.getDescription() != null) ? item.getDescription() : "";
    }
}
