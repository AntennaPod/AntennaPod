package de.danoeh.antennapod.core.syndication.namespace;

import de.danoeh.antennapod.core.feed.FeedImage;
import de.danoeh.antennapod.core.syndication.handler.HandlerState;
import org.xml.sax.Attributes;

import java.util.concurrent.TimeUnit;

public class NSITunes extends Namespace {
    public static final String NSTAG = "itunes";
    public static final String NSURI = "http://www.itunes.com/dtds/podcast-1.0.dtd";

    private static final String IMAGE = "image";
    private static final String IMAGE_TITLE = "image";
    private static final String IMAGE_HREF = "href";

    private static final String AUTHOR = "author";
    public static final String DURATION = "duration";


    @Override
    public SyndElement handleElementStart(String localName, HandlerState state,
                                          Attributes attributes) {
        if (localName.equals(IMAGE)) {
            FeedImage image = new FeedImage();
            image.setTitle(IMAGE_TITLE);
            image.setDownload_url(attributes.getValue(IMAGE_HREF));

            if (state.getCurrentItem() != null) {
                // this is an items image
                image.setTitle(state.getCurrentItem().getTitle()+IMAGE_TITLE);
                image.setOwner(state.getCurrentItem());
                state.getCurrentItem().setImage(image);

            } else  {
                // this is the feed image
                if (state.getFeed().getImage() == null) {
                    image.setOwner(state.getFeed());
                    state.getFeed().setImage(image);
                }
            }

        }

        return new SyndElement(localName, this);
    }

    @Override
    public void handleElementEnd(String localName, HandlerState state) {
        if (localName.equals(AUTHOR)) {
            state.getFeed().setAuthor(state.getContentBuf().toString());
        } else if (localName.equals(DURATION)) {
            String[] parts = state.getContentBuf().toString().trim().split(":");
            try {
                int duration = 0;
                if (parts.length == 2) {
                    duration += TimeUnit.MINUTES.toMillis(Long.valueOf(parts[0])) +
                            TimeUnit.SECONDS.toMillis(Long.valueOf(parts[1]));
                } else if (parts.length >= 3) {
                    duration += TimeUnit.HOURS.toMillis(Long.valueOf(parts[0])) +
                            TimeUnit.MINUTES.toMillis(Long.valueOf(parts[1])) +
                            TimeUnit.SECONDS.toMillis(Long.valueOf(parts[2]));
                } else {
                    return;
                }

                state.getTempObjects().put(DURATION, duration);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

    }

}
