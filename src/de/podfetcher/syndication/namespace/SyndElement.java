package de.podfetcher.syndication.namespace;

/** Defines a XML Element that is pushed on the tagstack */
public class SyndElement {
	protected String name;
	protected Namespace namespace;
	
	public SyndElement(String name, Namespace namespace) {
		super();
		this.name = name;
	}
	
	public Namespace getNamespace() {
		return null;
	}

	public String getName() {
		return name;
	}
	
	
}
