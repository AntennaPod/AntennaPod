package de.danoeh.antennapod.parser.feed.type;

import java.util.ArrayList;

public class TypeGetterRegistry {
    public static ArrayList<TypeGetter> getTypeGetters() {
        ArrayList<TypeGetter> typeGetters = new ArrayList<>();
        typeGetters.add(new XmlTypeGetter());
        typeGetters.add(new JsonTypeGetter());
        return typeGetters;
    }
}
