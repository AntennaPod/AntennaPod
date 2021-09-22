package de.danoeh.antennapod.parser.feed.util;

import java.util.ArrayList;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.parser.feed.UnsupportedFeedtypeException;

public class TypeResolver implements TypeGetter {
    private ArrayList<TypeGetter> typeGetters = new ArrayList<>();

    public TypeResolver() {
        typeGetters.add(new XmlTypeGetter());
    }

    @Override
    public Type getType(Feed feed) throws UnsupportedFeedtypeException {
        for (TypeGetter typeGetter : typeGetters) {
            try {
                return typeGetter.getType(feed);
            } catch (UnsupportedFeedtypeException unsupportedFeedtypeException) {
                continue;
            }
        }

        throw new UnsupportedFeedtypeException(Type.INVALID);
    }

    public enum Type {
        RSS20, RSS091, ATOM, JSON, INVALID
    }
}
