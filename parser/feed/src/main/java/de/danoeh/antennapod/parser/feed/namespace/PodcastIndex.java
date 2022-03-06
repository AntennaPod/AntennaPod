package de.danoeh.antennapod.parser.feed.namespace;

import android.text.TextUtils;
import de.danoeh.antennapod.parser.feed.HandlerState;
import de.danoeh.antennapod.parser.feed.element.SyndElement;
import org.xml.sax.Attributes;
import de.danoeh.antennapod.model.feed.FeedFunding;

public class PodcastIndex extends Namespace {

    public static final String NSTAG = "podcast";
    public static final String NSURI = "https://github.com/Podcastindex-org/podcast-namespace/blob/main/docs/1.0.md";
    public static final String NSURI2 = "https://podcastindex.org/namespace/1.0";
    private static final String URL = "url";
    private static final String FUNDING = "funding";
    private static final String CHAPTERS = "chapters";

    @Override
    public SyndElement handleElementStart(String localName, HandlerState state,
                                          Attributes attributes) {
        if (FUNDING.equals(localName)) {
            String href = attributes.getValue(URL);
            FeedFunding funding = new FeedFunding(href, "");
            state.setCurrentFunding(funding);
            state.getFeed().addPayment(state.getCurrentFunding());
        } else if (CHAPTERS.equals(localName)) {
            String href = attributes.getValue(URL);
            if (!TextUtils.isEmpty(href)) {
                state.getCurrentItem().setPodcastIndexChapterUrl(href);
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
        if (FUNDING.equals(localName) && state.getCurrentFunding() != null && !TextUtils.isEmpty(content)) {
            state.getCurrentFunding().setContent(content);
        }
    }
}
