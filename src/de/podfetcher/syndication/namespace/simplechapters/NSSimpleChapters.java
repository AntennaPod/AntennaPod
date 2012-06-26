package de.podfetcher.syndication.namespace.simplechapters;

import java.util.ArrayList;

import org.xml.sax.Attributes;

import de.podfetcher.feed.SimpleChapter;
import de.podfetcher.syndication.handler.HandlerState;
import de.podfetcher.syndication.namespace.Namespace;
import de.podfetcher.syndication.namespace.SyndElement;
import de.podfetcher.syndication.util.SyndDateUtils;

public class NSSimpleChapters extends Namespace {
	public static final String NSTAG = "sc";
	public static final String NSURI = "http://podlove.org/simple-chapters";

	public static final String CHAPTERS = "chapters";
	public static final String CHAPTER = "chapter";
	public static final String START = "start";
	public static final String TITLE = "title";

	@Override
	public SyndElement handleElementStart(String localName, HandlerState state,
			Attributes attributes) {
		if (localName.equals(CHAPTERS)) {
			state.getCurrentItem().setSimpleChapters(
					new ArrayList<SimpleChapter>());
		} else if (localName.equals(CHAPTER)) {
			state.getCurrentItem()
					.getSimpleChapters()
					.add(new SimpleChapter(SyndDateUtils
							.parseTimeString(attributes.getValue(START)),
							attributes.getValue(TITLE)));
		}

		return new SyndElement(localName, this);
	}

	@Override
	public void handleElementEnd(String localName, HandlerState state) {
	}

}
