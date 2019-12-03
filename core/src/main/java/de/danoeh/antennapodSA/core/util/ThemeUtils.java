package de.danoeh.antennapodSA.core.util;

import android.content.Context;
import android.util.TypedValue;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;

public class ThemeUtils {
    private ThemeUtils() {

    }

    public static @ColorInt int getColorFromAttr(Context context, @AttrRes int attr) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }
}
