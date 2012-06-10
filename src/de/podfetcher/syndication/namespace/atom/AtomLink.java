package de.podfetcher.syndication.namespace.atom;

import de.podfetcher.syndication.namespace.Namespace;
import de.podfetcher.syndication.namespace.SyndElement;

/** Represents a "link" - Element in an Atom Feed. */
public class AtomLink extends SyndElement {
	private String href, rel, title, type, length;
	
	public AtomLink(String name, Namespace namespace) {
		super(name, namespace);
		// TODO Auto-generated constructor stub
	}

}
