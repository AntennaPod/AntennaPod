package de.danoeh.antennapod.asynctask;

import android.graphics.Bitmap;

/** Stores a bitmap and the length it was decoded with. */
public class CachedBitmap {

	private Bitmap bitmap;
	private int length;
	
	public CachedBitmap(Bitmap bitmap, int length) {
		super();
		this.bitmap = bitmap;
		this.length = length;
	}
	
	public Bitmap getBitmap() {
		return bitmap;
	}
	public int getLength() {
		return length;
	}
	
	
	
	
}
