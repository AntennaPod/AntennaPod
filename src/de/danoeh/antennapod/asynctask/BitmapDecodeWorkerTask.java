package de.danoeh.antennapod.asynctask;

import android.content.res.TypedArray;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.asynctask.ImageLoader.ImageWorkerTaskResource;
import de.danoeh.antennapod.util.BitmapDecoder;

public class BitmapDecodeWorkerTask extends Thread {

	protected int PREFERRED_LENGTH;

	/** Can be thumbnail or cover */
	protected int imageType;

	private static final String TAG = "BitmapDecodeWorkerTask";
	private ImageView target;
	protected CachedBitmap cBitmap;

	protected ImageLoader.ImageWorkerTaskResource imageResource;

	private Handler handler;

	private final int defaultCoverResource;

	public BitmapDecodeWorkerTask(Handler handler, ImageView target,
			ImageWorkerTaskResource imageResource, int length, int imageType) {
		super();
		this.handler = handler;
		this.target = target;
		this.imageResource = imageResource;
		this.PREFERRED_LENGTH = length;
		this.imageType = imageType;
		TypedArray res = target.getContext().obtainStyledAttributes(
				new int[] { R.attr.default_cover });
		this.defaultCoverResource = res.getResourceId(0, 0);
		res.recycle();
	}

	/**
	 * Should return true if tag of the imageview is still the same it was
	 * before the bitmap was decoded
	 */
	protected boolean tagsMatching(ImageView target) {
		return target.getTag() == null
				|| target.getTag().equals(imageResource.getImageLoaderCacheKey());
	}

	protected void onPostExecute() {
		// check if imageview is still supposed to display this image
		if (tagsMatching(target) && cBitmap.getBitmap() != null) {
			target.setImageBitmap(cBitmap.getBitmap());
		} else {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Not displaying image");
		}
	}

	@Override
	public void run() {
		cBitmap = new CachedBitmap(BitmapDecoder.decodeBitmapFromWorkerTaskResource(
				PREFERRED_LENGTH, imageResource), PREFERRED_LENGTH);
		if (cBitmap.getBitmap() != null) {
			storeBitmapInCache(cBitmap);
		} else {
			Log.w(TAG, "Could not load bitmap. Using default image.");
			cBitmap = new CachedBitmap(BitmapFactory.decodeResource(
					target.getResources(), defaultCoverResource),
					PREFERRED_LENGTH);
		}
		if (AppConfig.DEBUG)
			Log.d(TAG, "Finished loading bitmaps");

		endBackgroundTask();
	}

	protected final void endBackgroundTask() {
		handler.post(new Runnable() {

			@Override
			public void run() {
				onPostExecute();
			}

		});
	}

	protected void onInvalidStream() {
		cBitmap = new CachedBitmap(BitmapFactory.decodeResource(
				target.getResources(), defaultCoverResource), PREFERRED_LENGTH);
	}

	protected void storeBitmapInCache(CachedBitmap cb) {
		ImageLoader loader = ImageLoader.getInstance();
		if (imageType == ImageLoader.IMAGE_TYPE_COVER) {
			loader.addBitmapToCoverCache(imageResource.getImageLoaderCacheKey(), cb);
		} else if (imageType == ImageLoader.IMAGE_TYPE_THUMBNAIL) {
			loader.addBitmapToThumbnailCache(imageResource.getImageLoaderCacheKey(), cb);
		}
	}

}
