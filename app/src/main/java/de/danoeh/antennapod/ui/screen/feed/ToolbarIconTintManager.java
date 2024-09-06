package de.danoeh.antennapod.ui.screen.feed;

import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.view.Menu;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;

import de.danoeh.antennapod.ui.common.OnCollapseChangeListener;

/**
 * A manager that automatically finds all icons in a collapsable toolbar and tints them according to the collapse state
 * of the toolbar.
 */
public class ToolbarIconTintManager extends OnCollapseChangeListener {
    private final MaterialToolbar toolbar;

    public ToolbarIconTintManager(MaterialToolbar toolbar, CollapsingToolbarLayout collapsingToolbar) {
        super(collapsingToolbar);
        this.toolbar = toolbar;
        this.onCollapseChanged(false);
    }

    @Override
    public void onCollapseChanged(boolean isCollapsed) {
        PorterDuffColorFilter filter = isCollapsed ? null : new PorterDuffColorFilter(0xffffffff, Mode.SRC_ATOP);

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
