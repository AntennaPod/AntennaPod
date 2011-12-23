package de.podfetcher.feed;

import java.util.ArrayList;




/**
 * Data Object for a whole feed
 * @author daniel
 *
 */
public class Feed extends FeedFile{
	public String title;
	public String link;
	public String description;
	public FeedImage image;
	public FeedCategory category;
	public ArrayList<FeedItem> items;
	
	
	public Feed() {
		items = new ArrayList<FeedItem>();
	}
	
	public Feed(String url) {
		this.download_url = url;
	}
	
	public Feed(String title, String link, String description, String download_url,
			 FeedCategory category) {
		super();
		this.title = title;
		this.link = link;
		this.description = description;
		this.download_url = download_url;
		this.category = category;
		items = new ArrayList<FeedItem>();
	}
	
	
	
	

}
