package de.danoeh.antennapod.ui.common;

import android.content.Context;
import android.util.AttributeSet;
import android.view.CollapsibleActionView;
import android.view.ViewGroup;

import androidx.appcompat.widget.SearchView;

/**
 * A SearchView implementation that can be used as an expanded action view.
 * It overrides the default behavior to take the full width of the screen.
 */
public class CollapsibleSearchView extends SearchView implements CollapsibleActionView {
    public CollapsibleSearchView(Context context) {
        super(context);
    }

    public CollapsibleSearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CollapsibleSearchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onActionViewExpanded() {
        super.onActionViewExpanded();
        ViewGroup.LayoutParams params = getLayoutParams();
        if (params != null) {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            setLayoutParams(params);
        }
        setIconifiedByDefault(false);
    }
}
