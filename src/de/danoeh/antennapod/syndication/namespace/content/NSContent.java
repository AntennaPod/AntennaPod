package de.danoeh.antennapod.syndication.namespace.content;

import org.xml.sax.Attributes;

import de.danoeh.antennapod.syndication.handler.HandlerState;
import de.danoeh.antennapod.syndication.namespace.Namespace;
import de.danoeh.antennapod.syndication.namespace.SyndElement;
import de.danoeh.antennapod.syndication.namespace.rss20.NSRSS20;

import org.apache.commons.lang3.StringEscapeUtils;

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
