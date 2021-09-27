package de.danoeh.antennapod.parser.feed.type;

import java.util.ArrayList;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.parser.feed.UnsupportedFeedtypeException;

public class TypeResolver implements TypeGetter {

    private final ArrayList<TypeGetter> typeGetters;

    public TypeResolver(ArrayList<TypeGetter> typeGetters) {
        this.typeGetters = typeGetters;
    }

    @Override
    public Type getType(Feed feed) throws UnsupportedFeedtypeException {
        for (TypeGetter typeGetter : typeGetters) {
            try {
                return typeGetter.getType(feed);
            } catch (UnsupportedFeedtypeException ignored) {
            }
        }

        throw new UnsupportedFeedtypeException(Type.INVALID);
    }

    public enum Type {
        RSS20, RSS091, ATOM, JSON, INVALID
    }
}
