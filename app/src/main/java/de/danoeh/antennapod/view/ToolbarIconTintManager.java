package de.danoeh.antennapod.view;

import android.content.Context;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.view.ContextThemeWrapper;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import de.danoeh.antennapod.R;

public abstract class ToolbarIconTintManager implements AppBarLayout.OnOffsetChangedListener {
    private final Context context;
    private final CollapsingToolbarLayout collapsingToolbar;
    private final Toolbar toolbar;
    private boolean isTinted = false;

    public ToolbarIconTintManager(Context context, Toolbar toolbar, CollapsingToolbarLayout collapsingToolbar) {
        this.context = context;
        this.collapsingToolbar = collapsingToolbar;
        this.toolbar = toolbar;
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int offset) {
        boolean tint  = (collapsingToolbar.getHeight() + offset) > (2 * ViewCompat.getMinimumHeight(collapsingToolbar));
        if (isTinted != tint) {
            isTinted = tint;
            updateTint();
        }
    }

    public void updateTint() {
        if (isTinted) {
            doTint(new ContextThemeWrapper(context, R.style.Theme_AntennaPod_Dark));
            safeSetColorFilter(toolbar.getNavigationIcon(), new PorterDuffColorFilter(0xffffffff, Mode.SRC_ATOP));
            safeSetColorFilter(toolbar.getOverflowIcon(), new PorterDuffColorFilter(0xffffffff, Mode.SRC_ATOP));
            safeSetColorFilter(toolbar.getCollapseIcon(), new PorterDuffColorFilter(0xffffffff, Mode.SRC_ATOP));
        } else {
            doTint(context);
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
