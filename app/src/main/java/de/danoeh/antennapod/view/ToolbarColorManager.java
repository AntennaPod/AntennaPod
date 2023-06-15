package de.danoeh.antennapod.view;

import android.app.Activity;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.TypedValue;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.appbar.AppBarLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.danoeh.antennapod.R;

public class ToolbarColorManager implements AppBarLayout.OnOffsetChangedListener {
    private final Activity activity;
    private final MaterialToolbar toolbar;
    private final int colorBackgroundToolbar;
    private final int colorToolbarIcons;
    private final List<Drawable> toolbarIconsToTint;
    private final int originalStatusBarColor;

    public ToolbarColorManager(Activity activity, MaterialToolbar toolbar, List<Drawable> toolbarIconsToTint) {
        this.activity = activity;
        this.toolbar = toolbar;

        //Get the background color of the toolbar (dependent on theme)
        Resources.Theme theme = activity.getTheme();
        TypedArray typedArray = theme.obtainStyledAttributes(new int[]{ R.attr.background_elevated});
        this.colorBackgroundToolbar = typedArray.getColor(0, 0);
        typedArray.recycle();

        //Get toolbar icon color (also dependant on theme)
        TypedValue typedValueToolbarIconColor = new TypedValue();
        theme.resolveAttribute(android.R.attr.colorForeground, typedValueToolbarIconColor, true);
        this.colorToolbarIcons = typedValueToolbarIconColor.data;

        //Compile list of all icons needing to be tinted from constructor + toolbar defaults
        this.toolbarIconsToTint = new ArrayList<>();
        this.toolbarIconsToTint.addAll(toolbarIconsToTint);
        this.toolbarIconsToTint.addAll(Arrays.asList(toolbar.getNavigationIcon(), toolbar.getOverflowIcon(),
                toolbar.getCollapseIcon()));

        //Save original status bar color so that it can be restored when the activity is destroyed
        originalStatusBarColor = activity.getWindow().getStatusBarColor();

        /*Reset status bar color whenever a new fragment come in foreground
        Check if activity is of AppCompatActivity to prevent ClassCastExceptions*/
        if (activity instanceof AppCompatActivity) {
            FragmentManager fragmentManager = ((AppCompatActivity) activity).getSupportFragmentManager();
            fragmentManager.addOnBackStackChangedListener(this::resetStatusBar);
        }
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int offset) {
        int alpha = Math.min(Math.abs((offset) * 2), 255);
        int newToolbarColor = ColorUtils.setAlphaComponent(colorBackgroundToolbar, alpha);

        //Update toolbar background color as well as update icon tint
        toolbar.setBackgroundColor(newToolbarColor);
        updateTint(alpha / 255f);

        /*Check if version is newer than marshmello because on older versions the bright toolbar with white icons
        would not create sufficient contrast, thus we must keep the default status bar*/
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.getWindow().setStatusBarColor(newToolbarColor);
        }
    }

    public void updateTint(float progress) {
        int color = ColorUtils.blendARGB(Color.WHITE, colorToolbarIcons, progress);
        ColorFilter colorFilter = new PorterDuffColorFilter(color, Mode.SRC_ATOP);
        for (Drawable drawable : toolbarIconsToTint) {
            if (drawable != null) {
                drawable.setColorFilter(colorFilter);
            }
        }
    }

    /**
     * Call this function whenever the activity this <class>toolbarIconTintManager</class> is onPause
     * Resets status bar color back to initial so that the status bar appearance does not permanently get altered
     */
    public void resetStatusBar() {
        activity.getWindow().setStatusBarColor(originalStatusBarColor);
    }
}
