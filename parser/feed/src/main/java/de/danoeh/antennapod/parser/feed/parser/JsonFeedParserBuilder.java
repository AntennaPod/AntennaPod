package de.danoeh.antennapod.parser.feed.parser;

import de.danoeh.antennapod.parser.feed.util.MimeTypeNonStaticWrapper;
import de.danoeh.antennapod.parser.feed.util.MimeTypeUtils;

public class JsonFeedParserBuilder {
    private MimeTypeUtils mimeTypeUtils;


    public JsonFeedParser createJsonFeedParser() {
        return new JsonFeedParser(new MimeTypeNonStaticWrapper());
    }
}