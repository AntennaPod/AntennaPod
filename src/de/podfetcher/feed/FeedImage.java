package de.podfetcher.feed;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class FeedImage extends FeedFile {
	protected String title;
	protected Bitmap image_bitmap;

	public FeedImage(String download_url, String title) {
		super();
		this.download_url = download_url;
		this.title = title;
	}
	
	public FeedImage(long id, String title, String file_url, String download_url) {
		this.id = id;
		this.title = title;
		this.file_url = file_url;
		this.download_url = download_url;
	}

	public FeedImage() {
		
	}

	public String getTitle() {
		return title;
	}

	public Bitmap getImageBitmap() {
		if(image_bitmap == null) {
			image_bitmap = BitmapFactory.decodeFile(getFile_url());	
		}
		return image_bitmap;
	}
	
	
	
	
	
	
}
