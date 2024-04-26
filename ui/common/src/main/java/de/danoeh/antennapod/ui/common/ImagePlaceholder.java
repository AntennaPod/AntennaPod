package de.danoeh.antennapod.ui.common;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

import androidx.core.content.ContextCompat;


public class ImagePlaceholder {
    public static Drawable getDrawable(Context context, float cornerRadius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        int color = ContextCompat.getColor(context, R.color.light_gray);
        drawable.setColor(color);
        drawable.setCornerRadius(cornerRadius);
        return drawable;
    }
}
