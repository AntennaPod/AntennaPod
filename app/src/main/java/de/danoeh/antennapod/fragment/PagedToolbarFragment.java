package de.danoeh.antennapod.fragment;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

/**
 * Fragment with a ViewPager where the displayed items influence the top toolbar's menu.
 * All items share the same general menu items and are just allowed to show/hide them.
 */
public abstract class PagedToolbarFragment extends Fragment {
    private Toolbar toolbar;
    private ViewPager2 viewPager;

    /**
     * Invalidate the toolbar menu if the current child fragment is visible.
     * @param child The fragment, or null to force-refresh whatever the active fragment is.
     */
    void invalidateOptionsMenuIfActive(Fragment child) {
        Fragment visibleChild = getChildFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());
        if (visibleChild == child || child == null) {
            child.onPrepareOptionsMenu(toolbar.getMenu());
        }
    }

    protected void setupPagedToolbar(Toolbar toolbar, ViewPager2 viewPager) {
        this.toolbar = toolbar;
        this.viewPager = viewPager;

        toolbar.setOnMenuItemClickListener(item -> {
            Fragment child = getChildFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());
            if (child != null) {
                return child.onOptionsItemSelected(item);
            }
            return false;
        });
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                Fragment child = getChildFragmentManager().findFragmentByTag("f" + position);
                if (child != null) {
                    child.onPrepareOptionsMenu(toolbar.getMenu());
                }
            }
        });
    }
}
