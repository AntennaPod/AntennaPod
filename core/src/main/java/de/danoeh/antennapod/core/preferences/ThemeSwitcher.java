package de.danoeh.antennapod.core.preferences;

import android.content.Context;
import android.content.res.Configuration;
import androidx.annotation.StyleRes;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

public abstract class ThemeSwitcher {
    @StyleRes
    public static int getTheme(Context context) {
        boolean dynamic = UserPreferences.getIsThemeColorTinted();
        switch (readThemeValue(context)) {
            case DARK:
                return dynamic ? R.style.Theme_AntennaPod_Dynamic_Dark : R.style.Theme_AntennaPod_Dark;
            case BLACK:
                return dynamic ? R.style.Theme_AntennaPod_Dynamic_TrueBlack : R.style.Theme_AntennaPod_TrueBlack;
            case LIGHT: // fall-through
            default:
                return dynamic ? R.style.Theme_AntennaPod_Dynamic_Light : R.style.Theme_AntennaPod_Light;
        }
    }

    @StyleRes
    public static int getNoTitleTheme(Context context) {
        boolean dynamic = UserPreferences.getIsThemeColorTinted();
        switch (readThemeValue(context)) {
            case DARK:
                return dynamic ? R.style.Theme_AntennaPod_Dynamic_Dark_NoTitle : R.style.Theme_AntennaPod_Dark_NoTitle;
            case BLACK:
                return dynamic ? R.style.Theme_AntennaPod_Dynamic_TrueBlack_NoTitle
                        : R.style.Theme_AntennaPod_TrueBlack_NoTitle;
            case LIGHT: // fall-through
            default:
                return dynamic ? R.style.Theme_AntennaPod_Dynamic_Light_NoTitle
                        : R.style.Theme_AntennaPod_Light_NoTitle;
        }
    }

    @StyleRes
    public static int getTranslucentTheme(Context context) {
        boolean dynamic = UserPreferences.getIsThemeColorTinted();
        switch (readThemeValue(context)) {
            case DARK:
                return dynamic ? R.style.Theme_AntennaPod_Dynamic_Dark_Translucent
                        : R.style.Theme_AntennaPod_Dark_Translucent;
            case BLACK:
                return dynamic ? R.style.Theme_AntennaPod_Dynamic_TrueBlack_Translucent
                        : R.style.Theme_AntennaPod_TrueBlack_Translucent;
            case LIGHT: // fall-through
            default:
                return dynamic ? R.style.Theme_AntennaPod_Dynamic_Light_Translucent
                        : R.style.Theme_AntennaPod_Light_Translucent;
        }
    }

    private static UserPreferences.ThemePreference readThemeValue(Context context) {
        UserPreferences.ThemePreference theme = UserPreferences.getTheme();
        if (theme == UserPreferences.ThemePreference.SYSTEM) {
            int nightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
                theme = UserPreferences.ThemePreference.DARK;
            } else {
                theme = UserPreferences.ThemePreference.LIGHT;

            }
        }
        if (theme == UserPreferences.ThemePreference.DARK && UserPreferences.getIsBlackTheme()) {
            theme = UserPreferences.ThemePreference.BLACK;
        }
        return theme;
    }
}
