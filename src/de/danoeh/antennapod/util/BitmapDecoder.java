package de.danoeh.antennapod.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class BitmapDecoder {
	private static final String TAG = "BitmapDecoder";

	private static int calculateSampleSize(int preferredLength, int width,
			int height) {
		int max = Math.max(width, height);
		if (max < preferredLength) {
			return 1;
		} else {
			// find first sample size where max / sampleSize <
			// PREFERRED_LENGTH
			for (int sampleSize = 1, power = 0;; power++, sampleSize = (int) Math
					.pow(2, power)) {
				int newLength = max / sampleSize;
				if (newLength <= preferredLength) {
					if (newLength > 0) {
						return sampleSize;
					} else {
						return sampleSize - 1;
					}
				}
			}
		}
	}

	public static Bitmap decodeBitmap(int preferredLength, String fileUrl) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(fileUrl, options);
		int srcWidth = options.outWidth;
		int srcHeight = options.outHeight;
		int sampleSize = calculateSampleSize(preferredLength, srcWidth,
				srcHeight);

		options.inJustDecodeBounds = false;
		options.inSampleSize = sampleSize;
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		options.inScaled = false;
		
		Bitmap decodedBitmap = BitmapFactory.decodeFile(fileUrl, options);
		if (decodedBitmap == null) {
			Log.i(TAG,
					"Bitmap could not be decoded in custom sample size. Trying default sample size (path was "
							+ fileUrl + ")");
			decodedBitmap = BitmapFactory.decodeFile(fileUrl);
		}
		if (decodedBitmap != null) {
			return decodedBitmap;
			/*
				return Bitmap.createScaledBitmap(decodedBitmap,
						preferredLength, preferredLength, false);
			*/
		} else {
			return null;
		}
	}
}
