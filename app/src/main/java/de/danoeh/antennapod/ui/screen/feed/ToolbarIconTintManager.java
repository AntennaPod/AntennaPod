package de.danoeh.antennapod.ui.screen.feed;

import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.view.Menu;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;

/**
 * A manager that automatically finds all icons in a collapsable toolbar and tints them according to the collapse state
 * of the toolbar.
 */
public class ToolbarIconTintManager implements AppBarLayout.OnOffsetChangedListener {
    private final CollapsingToolbarLayout collapsingToolbar;
    private final MaterialToolbar toolbar;
    private boolean isTinted = false;

    public ToolbarIconTintManager(MaterialToolbar toolbar, CollapsingToolbarLayout collapsingToolbar) {
        this.collapsingToolbar = collapsingToolbar;
        this.toolbar = toolbar;
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int offset) {
        boolean tint  = (collapsingToolbar.getHeight() + offset) > (2 * collapsingToolbar.getMinimumHeight());
        if (isTinted != tint) {
            isTinted = tint;
            updateTint();
        }
    }

    public void updateTint() {
        PorterDuffColorFilter filter = null;
        if (isTinted) {
            filter = new PorterDuffColorFilter(0xffffffff, Mode.SRC_ATOP);
        }

        safeSetColorFilter(toolbar.getNavigationIcon(), filter);
        safeSetColorFilter(toolbar.getOverflowIcon(), filter);
        safeSetColorFilter(toolbar.getCollapseIcon(), filter);

        Menu menu = toolbar.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            Drawable icon = menu.getItem(i).getIcon();
            safeSetColorFilter(icon, filter);
            menu.getItem(i).setIcon(icon);
        }
    }

    private void safeSetColorFilter(Drawable icon, PorterDuffColorFilter filter) {
        if (icon != null) {
            icon.setColorFilter(filter);
        }
    }
}
