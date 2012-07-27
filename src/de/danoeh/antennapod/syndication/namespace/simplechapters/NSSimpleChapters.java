package de.danoeh.antennapod.syndication.namespace.simplechapters;

import java.util.ArrayList;

import org.xml.sax.Attributes;

import de.danoeh.antennapod.feed.SimpleChapter;
import de.danoeh.antennapod.syndication.handler.HandlerState;
import de.danoeh.antennapod.syndication.namespace.Namespace;
import de.danoeh.antennapod.syndication.namespace.SyndElement;
import de.danoeh.antennapod.syndication.util.SyndDateUtils;

public class NSSimpleChapters extends Namespace {
	public static final String NSTAG = "sc";
	public static final String NSURI = "http://podlove.org/simple-chapters";

	public static final String CHAPTERS = "chapters";
	public static final String CHAPTER = "chapter";
	public static final String START = "start";
	public static final String TITLE = "title";
	public static final String HREF = "href";

	@Override
	public SyndElement handleElementStart(String localName, HandlerState state,
			Attributes attributes) {
		if (localName.equals(CHAPTERS)) {
			state.getCurrentItem().setSimpleChapters(
					new ArrayList<SimpleChapter>());
		} else if (localName.equals(CHAPTER)) {
			state.getCurrentItem()
					.getSimpleChapters()
					.add(new SimpleChapter(state.getCurrentItem(),
							SyndDateUtils.parseTimeString(attributes
									.getValue(START)), attributes
									.getValue(TITLE), attributes.getValue(HREF)));
		}

		return new SyndElement(localName, this);
	}

	@Override
	public void handleElementEnd(String localName, HandlerState state) {
	}

}
