package de.danoeh.antennapod.core.syndication.namespace;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import org.xml.sax.Attributes;

import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.core.syndication.handler.HandlerState;

public class NSITunes extends Namespace {

    public static final String NSTAG = "itunes";
    public static final String NSURI = "http://www.itunes.com/dtds/podcast-1.0.dtd";

    private static final String IMAGE = "image";
    private static final String IMAGE_HREF = "href";

    private static final String AUTHOR = "author";
    public static final String DURATION = "duration";
    private static final String SUBTITLE = "subtitle";
    private static final String SUMMARY = "summary";


    @NonNull
    @Override
    public SyndElement handleElementStart(String localName, @NonNull HandlerState state,
                                          @NonNull Attributes attributes) {
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
    public void handleElementEnd(String localName, @NonNull HandlerState state) {
        if(state.getContentBuf() == null) {
            return;
        }
        SyndElement secondElement = state.getSecondTag();
        String second = secondElement.getName();

        if (AUTHOR.equals(localName)) {
            if (state.getFeed() != null) {
                String author = state.getContentBuf().toString();
                state.getFeed().setAuthor(author);
            }
        } else if (DURATION.equals(localName)) {
            String durationStr = state.getContentBuf().toString();
            if(TextUtils.isEmpty(durationStr)) {
                return;
            }
            String[] parts = durationStr.trim().split(":");
            try {
                int durationMs = 0;
                if (parts.length == 2) {
                    durationMs += TimeUnit.MINUTES.toMillis(Long.parseLong(parts[0])) +
                            TimeUnit.SECONDS.toMillis((long)Float.parseFloat(parts[1]));
                } else if (parts.length >= 3) {
                    durationMs += TimeUnit.HOURS.toMillis(Long.parseLong(parts[0])) +
                            TimeUnit.MINUTES.toMillis(Long.parseLong(parts[1])) +
                            TimeUnit.SECONDS.toMillis((long)Float.parseFloat(parts[2]));
                } else {
                    return;
                }
                state.getTempObjects().put(DURATION, durationMs);
            } catch (NumberFormatException e) {
                Log.e(NSTAG, "Duration \"" + durationStr + "\" could not be parsed");
            }
        } else if (SUBTITLE.equals(localName)) {
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
        } else if (SUMMARY.equals(localName)) {
            String summary = state.getContentBuf().toString();
            if (TextUtils.isEmpty(summary)) {
                return;
            }
            if (state.getCurrentItem() != null &&
                    (TextUtils.isEmpty(state.getCurrentItem().getDescription()) ||
                            state.getCurrentItem().getDescription().length() * 1.25 < summary.length())) {
                state.getCurrentItem().setDescription(summary);
            } else if (NSRSS20.CHANNEL.equals(second) && state.getFeed() != null) {
                state.getFeed().setDescription(summary);
            }
        }
    }
}
