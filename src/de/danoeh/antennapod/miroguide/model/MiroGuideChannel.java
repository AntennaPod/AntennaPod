package de.danoeh.antennapod.miroguide.model;

import java.util.ArrayList;

public class MiroGuideChannel {
	private long id;
	private String name;
	private String thumbnailUrl;
	private String downloadUrl;
	private String websiteUrl;
	private String description;
	private ArrayList<MiroGuideItem> items;

	public MiroGuideChannel(long id, String name, String thumbnailUrl,
			String downloadUrl, String websiteUrl, String description) {
		super();
		this.id = id;
		this.name = name;
		this.thumbnailUrl = thumbnailUrl;
		this.downloadUrl = downloadUrl;
		this.websiteUrl = websiteUrl;
		this.description = description;
	}

	public MiroGuideChannel(long id, String name, String thumbnailUrl,
			String downloadUrl, String websiteUrl, String description,
			ArrayList<MiroGuideItem> items) {
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

	public ArrayList<MiroGuideItem> getItems() {
		return items;
	}

	public void setThumbnailUrl(String thumbnailUrl) {
		this.thumbnailUrl = thumbnailUrl;
	}

}
