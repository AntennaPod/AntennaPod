package de.danoeh.antennapod.core.util;

import android.content.Context;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import android.util.TypedValue;
import androidx.annotation.DrawableRes;

public class ThemeUtils {
    private ThemeUtils() {

    }

    public static @ColorInt int getColorFromAttr(Context context, @AttrRes int attr) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    public static @DrawableRes int getDrawableFromAttr(Context context, @AttrRes int attr) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.resourceId;
    }
}
