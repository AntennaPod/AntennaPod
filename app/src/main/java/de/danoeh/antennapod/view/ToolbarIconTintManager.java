package de.danoeh.antennapod.view;

import android.content.Context;
import android.graphics.PorterDuff;
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
            //toolbar.getNavigationIcon().setColorFilter(0xffffffff, PorterDuff.Mode.SRC_ATOP);
            toolbar.getOverflowIcon().setColorFilter(0xffffffff, PorterDuff.Mode.SRC_ATOP);
        } else {
            doTint(context);
            //toolbar.getNavigationIcon().clearColorFilter();
            toolbar.getOverflowIcon().clearColorFilter();
        }
    }

    /**
     * View expansion was changed. Icons need to be tinted
     * @param themedContext ContextThemeWrapper with dark theme while expanded
     */
    protected abstract void doTint(Context themedContext);
}
