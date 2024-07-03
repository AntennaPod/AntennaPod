package de.danoeh.antennapod.ui.common;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;

public abstract class OnCollapseChangeListener implements AppBarLayout.OnOffsetChangedListener {
    private final CollapsingToolbarLayout collapsingToolbar;
    private boolean wasCollapsed = false;

    public OnCollapseChangeListener(CollapsingToolbarLayout collapsingToolbar) {
        this.collapsingToolbar = collapsingToolbar;
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int offset) {
        boolean isCollapsed = (collapsingToolbar.getHeight() + offset)
                < collapsingToolbar.getScrimVisibleHeightTrigger();
        if (wasCollapsed != isCollapsed) {
            wasCollapsed = isCollapsed;
            onCollapseChanged(isCollapsed);
        }
    }

    public abstract void onCollapseChanged(boolean isCollapsed);
}
