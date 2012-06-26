package de.podfetcher.syndication.namespace.itunes;

import org.xml.sax.Attributes;

import de.podfetcher.feed.FeedImage;
import de.podfetcher.syndication.handler.HandlerState;
import de.podfetcher.syndication.namespace.Namespace;
import de.podfetcher.syndication.namespace.SyndElement;

public class NSITunes extends Namespace{
	public static final String NSTAG = "itunes";
	public static final String NSURI = "http://www.itunes.com/dtds/podcast-1.0.dtd";
	
	private static final String IMAGE = "image";
	private static final String IMAGE_TITLE = "image";
	private static final String IMAGE_HREF = "href";
	
	
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
		
		
	}

}
