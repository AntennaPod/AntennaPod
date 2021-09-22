package de.danoeh.antennapod.parser.feed.util;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.parser.feed.UnsupportedFeedtypeException;

public interface TypeGetter {
    TypeResolver.Type getType(Feed feed) throws UnsupportedFeedtypeException;
}
