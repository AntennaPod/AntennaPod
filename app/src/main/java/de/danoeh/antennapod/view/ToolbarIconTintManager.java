package de.danoeh.antennapod.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.ContextThemeWrapper;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import de.danoeh.antennapod.R;

public abstract class ToolbarIconTintManager implements AppBarLayout.OnOffsetChangedListener {
    private static final int defaultStatusBarHeight = 24; //See https://m2.material.io/design/platform-guidance/android-bars.html#status-bar
    private final Activity activity;
    private final CollapsingToolbarLayout collapsingToolbar;
    private final MaterialToolbar toolbar;
    private boolean isTinted = false;
    private boolean isWhiteIconsStatusBar = false;

    public ToolbarIconTintManager(Activity activity, MaterialToolbar toolbar,
                                  CollapsingToolbarLayout collapsingToolbar) {
        this.activity = activity;
        this.collapsingToolbar = collapsingToolbar;
        this.toolbar = toolbar;
    }

    /**
     * Change appearance of status bar and toolbar in order to fix issue #6274.
     * Sets them black or white depending on whether these elements are currently over header or not.
     * @param appBarLayout the {@link AppBarLayout} which offset has changed
     * @param offset the vertical offset for the parent {@link AppBarLayout}, in px
     */
    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int offset) {
        //Convert status bar height from dp to px
        int statusBarHeightPx = defaultStatusBarHeight *
                activity.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT;
        double ratio = (float) (collapsingToolbar.getHeight() + offset) / collapsingToolbar.getMinimumHeight();

        //Check if status bar and/or toolbar need to be changed to reflect appearance
        boolean whiteIconsStatusBar = -offset / 2 < statusBarHeightPx || ratio < 1.5;
        boolean tintToolbar = ratio > 1.5;

        //Change appearance of status bar only when needed to reduce overhead
        if (isWhiteIconsStatusBar != whiteIconsStatusBar) {
            isWhiteIconsStatusBar = whiteIconsStatusBar;
            WindowInsetsControllerCompat windowInsetController =
                    WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView());
            windowInsetController.setAppearanceLightStatusBars(whiteIconsStatusBar);
        }

        //Tint toolbar if needed
        if (isTinted != tintToolbar) {
            isTinted = tintToolbar;
            updateTint();
        }
    }

    public void updateTint() {
        if (isTinted) {
            doTint(new ContextThemeWrapper(activity, R.style.Theme_AntennaPod_Dark));
            safeSetColorFilter(toolbar.getNavigationIcon(), new PorterDuffColorFilter(0xffffffff, Mode.SRC_ATOP));
            safeSetColorFilter(toolbar.getOverflowIcon(), new PorterDuffColorFilter(0xffffffff, Mode.SRC_ATOP));
            safeSetColorFilter(toolbar.getCollapseIcon(), new PorterDuffColorFilter(0xffffffff, Mode.SRC_ATOP));
        } else {
            doTint(activity);
            safeSetColorFilter(toolbar.getNavigationIcon(), null);
            safeSetColorFilter(toolbar.getOverflowIcon(), null);
            safeSetColorFilter(toolbar.getCollapseIcon(), null);
        }
    }

    private void safeSetColorFilter(Drawable icon, PorterDuffColorFilter filter) {
        if (icon != null) {
            icon.setColorFilter(filter);
        }
    }

    /**
     * View expansion was changed. Icons need to be tinted
     * @param themedContext ContextThemeWrapper with dark theme while expanded
     */
    protected abstract void doTint(Context themedContext);
}
