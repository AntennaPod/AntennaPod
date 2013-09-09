package de.danoeh.antennapod.miroguide.model;

import java.util.Date;

public class MiroGuideItem {
	private String name;
	private String description;
	private Date date;
	private String url;

	public MiroGuideItem(String name, String description, Date date, String url) {
		super();
		this.name = name;
		this.description = description;
		this.date = (Date) date.clone();
		this.url = url;
	}

	@Override
	public String toString() {
		return name + " " + date.toString();
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public Date getDate() {
		return (Date) date.clone();
	}

	public String getUrl() {
		return url;
	}

}
