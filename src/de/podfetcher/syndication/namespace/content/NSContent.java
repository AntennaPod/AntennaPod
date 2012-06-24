package de.podfetcher.syndication.namespace.content;

import org.xml.sax.Attributes;

import de.podfetcher.syndication.handler.HandlerState;
import de.podfetcher.syndication.namespace.Namespace;
import de.podfetcher.syndication.namespace.SyndElement;
import de.podfetcher.syndication.namespace.rss20.NSRSS20;
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
	public void handleCharacters(HandlerState state, char[] ch, int start,
			int length) {

	}

	@Override
	public void handleElementEnd(String localName, HandlerState state) {
		if (localName.equals(ENCODED)) {
			state.getCurrentItem().setContentEncoded(StringEscapeUtils.unescapeHtml4(state.getContentBuf().toString()));
		}
	}

}
