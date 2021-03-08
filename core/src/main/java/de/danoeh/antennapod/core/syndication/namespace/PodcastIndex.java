package de.danoeh.antennapod.core.syndication.namespace;

import org.jsoup.helper.StringUtil;
import org.xml.sax.Attributes;

import java.util.ArrayList;
import java.util.Map;

import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.syndication.handler.HandlerState;

public class PodcastIndex extends Namespace {

    public static final String NSTAG = "podcast";
    public static final String NSURI = "https://github.com/Podcastindex-org/podcast-namespace/blob/main/docs/1.0.md";
    public static final String NSURI2 = "https://podcastindex.org/namespace/1.0";
    private static final String URL = "url";

    private static final String FUNDING = "funding";
    private static Feed.Funding funding;

    @Override
    public SyndElement handleElementStart(String localName, HandlerState state,
                                          Attributes attributes) {
        if (FUNDING.equals(localName)) {
            String href = attributes.getValue(URL);
            if (funding == null) {
               funding = new Feed.Funding(href, "");
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
        if (FUNDING.equals(localName) && !StringUtil.isBlank(content) && funding != null) {
            funding.setContent(content);
            funding.setType(Feed.FundingType.PODCAST2_FUNDING)
            state.getFeed().addPayment(funding);
        }
    }
}
