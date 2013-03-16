package de.danoeh.antennapod.feed;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import de.danoeh.antennapod.asynctask.ImageLoader;

;

public class FeedImage extends FeedFile implements
		ImageLoader.ImageWorkerTaskResource {
	public static final int FEEDFILETYPE_FEEDIMAGE = 1;

	protected String title;
	protected Feed feed;

	public FeedImage(String download_url, String title) {
		super(null, download_url, false);
		this.download_url = download_url;
		this.title = title;
	}

	public FeedImage(long id, String title, String file_url,
			String download_url, boolean downloaded) {
		super(file_url, download_url, downloaded);
		this.id = id;
		this.title = title;
	}

	@Override
	public String getHumanReadableIdentifier() {
		if (feed != null && feed.getTitle() != null) {
			return feed.getTitle();
		} else {
			return download_url;
		}
	}

	@Override
	public int getTypeAsInt() {
		return FEEDFILETYPE_FEEDIMAGE;
	}

	public FeedImage() {
		super();
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Feed getFeed() {
		return feed;
	}

	public void setFeed(Feed feed) {
		this.feed = feed;
	}

	@Override
	public InputStream openImageInputStream() {
		if (file_url != null) {
			File file = new File(file_url);
			if (file.exists()) {
				try {
					return new FileInputStream(file_url);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	@Override
	public String getImageLoaderCacheKey() {
		return file_url;
	}

	@Override
	public InputStream reopenImageInputStream(InputStream input) {
		IOUtils.closeQuietly(input);
		return openImageInputStream();
	}

}
