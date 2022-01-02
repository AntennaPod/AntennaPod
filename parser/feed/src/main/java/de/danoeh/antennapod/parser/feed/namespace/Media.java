package de.danoeh.antennapod.parser.feed.namespace;

import android.text.TextUtils;
import android.util.Log;

import de.danoeh.antennapod.parser.feed.HandlerState;
import de.danoeh.antennapod.parser.feed.element.SyndElement;
import org.xml.sax.Attributes;

import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.parser.feed.element.AtomText;
import de.danoeh.antennapod.parser.feed.util.SyndTypeUtils;

/** Processes tags from the http://search.yahoo.com/mrss/ namespace. */
public class Media extends Namespace {
    private static final String TAG = "NSMedia";

    public static final String NSTAG = "media";
    public static final String NSURI = "http://search.yahoo.com/mrss/";

    private static final String CONTENT = "content";
    private static final String DOWNLOAD_URL = "url";
    private static final String SIZE = "fileSize";
    private static final String MIME_TYPE = "type";
    private static final String DURATION = "duration";
    private static final String DEFAULT = "isDefault";
    private static final String MEDIUM = "medium";

    private static final String MEDIUM_IMAGE = "image";
    private static final String MEDIUM_AUDIO = "audio";
    private static final String MEDIUM_VIDEO = "video";

    private static final String IMAGE = "thumbnail";
    private static final String IMAGE_URL = "url";

    private static final String DESCRIPTION = "description";
    private static final String DESCRIPTION_TYPE = "type";

    @Override
    public SyndElement handleElementStart(String localName, HandlerState state,
                                          Attributes attributes) {
        if (CONTENT.equals(localName)) {
            String url = attributes.getValue(DOWNLOAD_URL);
            String type = attributes.getValue(MIME_TYPE);
            String defaultStr = attributes.getValue(DEFAULT);
            String medium = attributes.getValue(MEDIUM);
            boolean validTypeMedia = false;
            boolean validTypeImage = false;
            boolean isDefault = "true".equals(defaultStr);
            String guessedType = SyndTypeUtils.getMimeTypeFromUrl(url);

            if (MEDIUM_AUDIO.equals(medium)) {
                validTypeMedia = true;
                type = "audio/*";
            } else if (MEDIUM_VIDEO.equals(medium)) {
                validTypeMedia = true;
                type = "video/*";
            } else if (MEDIUM_IMAGE.equals(medium) && (guessedType == null
                    || (!guessedType.startsWith("audio/") && !guessedType.startsWith("video/")))) {
                // Apparently, some publishers explicitly specify the audio file as an image
                validTypeImage = true;
                type = "image/*";
            } else {
                if (type == null) {
                    type = guessedType;
                }

                if (SyndTypeUtils.enclosureTypeValid(type)) {
                    validTypeMedia = true;
                } else if (SyndTypeUtils.imageTypeValid(type)) {
                    validTypeImage = true;
                }
            }

            if (state.getCurrentItem() != null && (state.getCurrentItem().getMedia() == null || isDefault)
                    && url != null && validTypeMedia) {
                long size = 0;
                String sizeStr = attributes.getValue(SIZE);
                try {
                    size = Long.parseLong(sizeStr);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Size \"" + sizeStr + "\" could not be parsed.");
                }

                int durationMs = 0;
                String durationStr = attributes.getValue(DURATION);
                if (!TextUtils.isEmpty(durationStr)) {
                    try {
                        long duration = Long.parseLong(durationStr);
                        durationMs = (int) TimeUnit.MILLISECONDS.convert(duration, TimeUnit.SECONDS);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Duration \"" + durationStr + "\" could not be parsed");
                    }
                }
                FeedMedia media = new FeedMedia(state.getCurrentItem(), url, size, type);
                if (durationMs > 0) {
                    media.setDuration(durationMs);
                }
                state.getCurrentItem().setMedia(media);
            } else if (state.getCurrentItem() != null && url != null && validTypeImage) {
                state.getCurrentItem().setImageUrl(url);
            }
        } else if (IMAGE.equals(localName)) {
            String url = attributes.getValue(IMAGE_URL);
            if (url != null) {
                if (state.getCurrentItem() != null) {
                    state.getCurrentItem().setImageUrl(url);
                } else {
                    if (state.getFeed().getImageUrl() == null) {
                        state.getFeed().setImageUrl(url);
                    }
                }
            }
        } else if (DESCRIPTION.equals(localName)) {
            String type = attributes.getValue(DESCRIPTION_TYPE);
            return new AtomText(localName, this, type);
        }
        return new SyndElement(localName, this);
    }

    @Override
    public void handleElementEnd(String localName, HandlerState state) {
        if (DESCRIPTION.equals(localName)) {
            String content = state.getContentBuf().toString();
            if (state.getCurrentItem() != null) {
                state.getCurrentItem().setDescriptionIfLonger(content);
            }
        }
    }
}

