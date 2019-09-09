package de.danoeh.antennapod.core.util;

import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.util.TypedValue;

public class ThemeUtils {
    private ThemeUtils() {

    }

    public static @ColorInt int getColorFromAttr(Context context, @AttrRes int attr) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }
}
