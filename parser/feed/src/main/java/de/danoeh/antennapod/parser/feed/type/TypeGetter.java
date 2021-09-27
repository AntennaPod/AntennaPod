package de.danoeh.antennapod.parser.feed.type;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.parser.feed.UnsupportedFeedtypeException;

public interface TypeGetter {
    TypeResolver.Type getType(Feed feed) throws UnsupportedFeedtypeException;
}
