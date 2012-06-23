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
	
	private StringBuffer encoded;
	
	@Override
	public SyndElement handleElementStart(String localName, HandlerState state,
			Attributes attributes) {
		if (localName.equals(ENCODED)) {
			encoded = new StringBuffer();
		}
		return new SyndElement(localName, this);
	}

	@Override
	public void handleCharacters(HandlerState state, char[] ch, int start,
			int length) {
		if (state.getTagstack().size() >= 2) {
			String content = new String(ch, start, length);
			SyndElement topElement = state.getTagstack().peek();
			String top = topElement.getName();
			SyndElement secondElement = state.getSecondTag();
			String second = secondElement.getName();
			if (top.equals(ENCODED) && second.equals(NSRSS20.ITEM)) {
				encoded.append(content);
			}
		}

	}

	@Override
	public void handleElementEnd(String localName, HandlerState state) {
		if (localName.equals(ENCODED)) {
			state.getCurrentItem().setContentEncoded(StringEscapeUtils.unescapeHtml4(encoded.toString()));
			encoded = null;
		}
	}

}
