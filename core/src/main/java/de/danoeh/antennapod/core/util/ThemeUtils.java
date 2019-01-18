package de.danoeh.antennapod.core.util;

import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.util.Log;
import android.util.TypedValue;

import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;

public class ThemeUtils {
    private ThemeUtils(){}

    private static final String TAG = "ThemeUtils";

    public static int getSelectionBackgroundColor() {
        int theme = UserPreferences.getTheme();
        if (theme == R.style.Theme_AntennaPod_Dark) {
            return R.color.selection_background_color_dark;
        } else if (theme == R.style.Theme_AntennaPod_TrueBlack){
            return R.color.selection_background_color_trueblack;
        } else if (theme == R.style.Theme_AntennaPod_Light) {
            return R.color.selection_background_color_light;
        } else {
            Log.e(TAG,
                    "getSelectionBackgroundColor could not match the current theme to any color!");
            return R.color.selection_background_color_light;
        }
    }

    public static int getSelectionDrawerActivatedColor() {
        int theme = UserPreferences.getTheme();
        if (theme == R.style.Theme_AntennaPod_Dark) {
            return R.color.overlay_dark;
        } else if (theme == R.style.Theme_AntennaPod_TrueBlack){
            return R.color.highlight_trueblack;
        } else if (theme == R.style.Theme_AntennaPod_Light) {
            return R.color.highlight_light;
        } else {
            Log.e(TAG,
                    "getSelectionBackgroundColor could not match the current theme to any color!");
            return R.color.highlight_light;
        }

    }

    public static int getSelectionDrawerNotActivatedColor() {
        int theme = UserPreferences.getTheme();
        if (theme == R.style.Theme_AntennaPod_Dark) {
            return R.color.darktheme_drawer;
        } else if (theme == R.style.Theme_AntennaPod_TrueBlack){
            return R.color.black;
        } else if (theme == R.style.Theme_AntennaPod_Light) {
            return R.color.primary_light;
        } else {
            Log.e(TAG,
                    "getSelectionBackgroundColor could not match the current theme to any color!");
            return R.color.highlight_light;
        }
    }


    public static @ColorInt int getColorFromAttr(Context context, @AttrRes int attr) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }
}
