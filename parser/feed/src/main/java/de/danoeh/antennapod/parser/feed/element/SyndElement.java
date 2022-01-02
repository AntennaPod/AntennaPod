package de.danoeh.antennapod.parser.feed.element;

import de.danoeh.antennapod.parser.feed.namespace.Namespace;

/** Defines a XML Element that is pushed on the tagstack */
public class SyndElement {
    private final String name;
    private final Namespace namespace;
    
    public SyndElement(String name, Namespace namespace) {
        this.name = name;
        this.namespace = namespace;
    }
    
    public Namespace getNamespace() {
        return namespace;
    }

    public String getName() {
        return name;
    }
}
