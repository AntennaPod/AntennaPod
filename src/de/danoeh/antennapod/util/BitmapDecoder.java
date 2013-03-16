package de.danoeh.antennapod.util;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.util.Log;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.asynctask.ImageLoader;

public class BitmapDecoder {
	private static final String TAG = "BitmapDecoder";

	private static int calculateSampleSize(int preferredLength, int length) {
		int sampleSize = 1;
		if (length > preferredLength) {
			sampleSize = Math.round(((float) length / (float) preferredLength));
		}
		return sampleSize;
	}

	public static Bitmap decodeBitmapFromWorkerTaskResource(int preferredLength,
			ImageLoader.ImageWorkerTaskResource source) {
		InputStream input = source.openImageInputStream();
		if (input != null) {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(input, new Rect(), options);
			int srcWidth = options.outWidth;
			int srcHeight = options.outHeight;
			int length = Math.max(srcWidth, srcHeight);
			int sampleSize = calculateSampleSize(preferredLength, length);
			if (AppConfig.DEBUG)
				Log.d(TAG, "Using samplesize " + sampleSize);
			options.inJustDecodeBounds = false;
			options.inSampleSize = sampleSize;
			options.inPreferredConfig = Bitmap.Config.ARGB_8888;
			Bitmap decodedBitmap = BitmapFactory.decodeStream(source.reopenImageInputStream(input),
					null, options);
			if (decodedBitmap == null) {
				decodedBitmap = BitmapFactory.decodeStream(source.reopenImageInputStream(input));
			}
			IOUtils.closeQuietly(input);
			return decodedBitmap;
		}
		return null;
	}
}
