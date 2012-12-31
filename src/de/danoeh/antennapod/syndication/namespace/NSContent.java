package de.danoeh.antennapod.syndication.namespace;

import org.xml.sax.Attributes;

import de.danoeh.antennapod.syndication.handler.HandlerState;

public class NSContent extends Namespace {
	public static final String NSTAG = "content";
	public static final String NSURI = "http://purl.org/rss/1.0/modules/content/";
	
	private static final String ENCODED = "encoded";
	
	@Override
	public SyndElement handleElementStart(String localName, HandlerState state,
			Attributes attributes) {
		return new SyndElement(localName, this);
	}

	@Override
	public void handleElementEnd(String localName, HandlerState state) {
		if (localName.equals(ENCODED)) {
			state.getCurrentItem().setContentEncoded(state.getContentBuf().toString());
		}
	}

}
