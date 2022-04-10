package de.danoeh.antennapod.parser.feed.namespace;

import android.text.TextUtils;
import android.util.Log;

import androidx.core.text.HtmlCompat;

import de.danoeh.antennapod.parser.feed.HandlerState;
import de.danoeh.antennapod.parser.feed.element.SyndElement;
import de.danoeh.antennapod.parser.feed.util.DurationParser;
import org.xml.sax.Attributes;


public class Itunes extends Namespace {

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

        String content = state.getContentBuf().toString();
        String contentFromHtml = HtmlCompat.fromHtml(content, HtmlCompat.FROM_HTML_MODE_COMPACT).toString();
        if (TextUtils.isEmpty(content)) {
            return;
        }

        if (AUTHOR.equals(localName) && state.getFeed() != null && state.getTagstack().size() <= 3) {
            state.getFeed().setAuthor(contentFromHtml);
        } else if (DURATION.equals(localName)) {
            try {
                long durationMs = DurationParser.inMillis(content);
                state.getTempObjects().put(DURATION, (int) durationMs);
            } catch (NumberFormatException e) {
                Log.e(NSTAG, String.format("Duration '%s' could not be parsed", content));
            }
        } else if (SUBTITLE.equals(localName)) {
            if (state.getCurrentItem() != null && TextUtils.isEmpty(state.getCurrentItem().getDescription())) {
                state.getCurrentItem().setDescriptionIfLonger(content);
            } else if (state.getFeed() != null && TextUtils.isEmpty(state.getFeed().getDescription())) {
                state.getFeed().setDescription(content);
            }
        } else if (SUMMARY.equals(localName)) {
            if (state.getCurrentItem() != null) {
                state.getCurrentItem().setDescriptionIfLonger(content);
            } else if (Rss20.CHANNEL.equals(state.getSecondTag().getName()) && state.getFeed() != null) {
                state.getFeed().setDescription(content);
            }
        }
    }
}
