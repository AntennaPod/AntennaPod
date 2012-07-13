package de.danoeh.antennapod.syndication.namespace.itunes;

import org.xml.sax.Attributes;

import de.danoeh.antennapod.feed.FeedImage;
import de.danoeh.antennapod.syndication.handler.HandlerState;
import de.danoeh.antennapod.syndication.namespace.Namespace;
import de.danoeh.antennapod.syndication.namespace.SyndElement;

public class NSITunes extends Namespace{
	public static final String NSTAG = "itunes";
	public static final String NSURI = "http://www.itunes.com/dtds/podcast-1.0.dtd";
	
	private static final String IMAGE = "image";
	private static final String IMAGE_TITLE = "image";
	private static final String IMAGE_HREF = "href";
	
	private static final String AUTHOR = "author";
	
	
	@Override
	public SyndElement handleElementStart(String localName, HandlerState state,
			Attributes attributes) {
		if (localName.equals(IMAGE) && state.getFeed().getImage() == null) {
			FeedImage image = new FeedImage();
			image.setTitle(IMAGE_TITLE);
			image.setDownload_url(attributes.getValue(IMAGE_HREF));
			state.getFeed().setImage(image);
		}
		
		return new SyndElement(localName, this);
	}

	@Override
	public void handleElementEnd(String localName, HandlerState state) {
		if (localName.equals(AUTHOR)) {
			state.getFeed().setAuthor(state.getContentBuf().toString());
		}
		
	}

}
