package de.danoeh.antennapod.util;

import android.util.Log;
import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.R;

public class ThemeUtils {
	private static final String TAG = "ThemeUtils";

	public static int getSelectionBackgroundColor() {
		switch (PodcastApp.getThemeResourceId()) {
		case R.style.Theme_AntennaPod_Dark:
			return R.color.selection_background_color_dark;
		case R.style.Theme_AntennaPod_Light:
			return R.color.selection_background_color_light;
		default:
			Log.e(TAG,
					"getSelectionBackgroundColor could not match the current theme to any color!");
			return R.color.selection_background_color_light;
		}
	}
}
