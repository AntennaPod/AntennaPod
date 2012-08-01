package de.danoeh.antennapod.miroguide.model;

import java.util.Date;

public class MiroItem {
	private String name;
	private String description;
	private Date date;
	private String url;

	public MiroItem(String name, String description, Date date, String url) {
		super();
		this.name = name;
		this.description = description;
		this.date = date;
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
		return date;
	}

	public String getUrl() {
		return url;
	}

}
