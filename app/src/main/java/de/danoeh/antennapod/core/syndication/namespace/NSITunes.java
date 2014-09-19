package de.danoeh.antennapod.core.syndication.namespace;

import de.danoeh.antennapod.core.feed.FeedImage;
import de.danoeh.antennapod.core.syndication.handler.HandlerState;
import org.xml.sax.Attributes;

public class NSITunes extends Namespace {
    public static final String NSTAG = "itunes";
    public static final String NSURI = "http://www.itunes.com/dtds/podcast-1.0.dtd";

    private static final String IMAGE = "image";
    private static final String IMAGE_TITLE = "image";
    private static final String IMAGE_HREF = "href";

    private static final String AUTHOR = "author";


    @Override
    public SyndElement handleElementStart(String localName, HandlerState state,
                                          Attributes attributes) {
        if (localName.equals(IMAGE)) {
            FeedImage image = new FeedImage();
            image.setTitle(IMAGE_TITLE);
            image.setDownload_url(attributes.getValue(IMAGE_HREF));

            if (state.getCurrentItem() != null) {
                // this is an items image
                image.setTitle(state.getCurrentItem().getTitle() + IMAGE_TITLE);
                state.getCurrentItem().setImage(image);

            } else {
                // this is the feed image
                if (state.getFeed().getImage() == null) {
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
        }

    }

}
