package de.danoeh.antennapod.miroguide.model;

import java.util.ArrayList;

public class MiroChannel {
	private long id;
	private String name;
	private String thumbnailUrl;
	private String downloadUrl;
	private String websiteUrl;
	private String description;
	private ArrayList<MiroItem> items;

	public MiroChannel(long id, String name, String thumbnailUrl,
			String downloadUrl, String websiteUrl, String description) {
		super();
		this.id = id;
		this.name = name;
		this.thumbnailUrl = thumbnailUrl;
		this.downloadUrl = downloadUrl;
		this.websiteUrl = websiteUrl;
		this.description = description;
	}

	public MiroChannel(long id, String name, String thumbnailUrl,
			String downloadUrl, String websiteUrl, String description,
			ArrayList<MiroItem> items) {
		super();
		this.id = id;
		this.name = name;
		this.thumbnailUrl = thumbnailUrl;
		this.downloadUrl = downloadUrl;
		this.websiteUrl = websiteUrl;
		this.description = description;
		this.items = items;
	}
	
	

	@Override
	public String toString() {
		return id + " " + name;
	}

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

	public String getDownloadUrl() {
		return downloadUrl;
	}

	public String getWebsiteUrl() {
		return websiteUrl;
	}

	public String getDescription() {
		return description;
	}

	public ArrayList<MiroItem> getItems() {
		return items;
	}
	
	

}
