package de.podfetcher.feed;

import java.util.ArrayList;




/**
 * Data Object for a whole feed
 * @author daniel
 *
 */
public class Feed extends FeedFile{
	private String title;
	private String link;
	private String description;
	private FeedImage image;
	private FeedCategory category;
	private ArrayList<FeedItem> items;
	
	
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

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public FeedImage getImage() {
		return image;
	}

	public void setImage(FeedImage image) {
		this.image = image;
	}

	public FeedCategory getCategory() {
		return category;
	}

	public void setCategory(FeedCategory category) {
		this.category = category;
	}

	public ArrayList<FeedItem> getItems() {
		return items;
	}

	public void setItems(ArrayList<FeedItem> items) {
		this.items = items;
	}

	

	
	
	
	
	

}
