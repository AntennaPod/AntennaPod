package de.danoeh.antennapod.fragment;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

/**
 * Fragment with a ViewPager where the displayed items influence the top toolbar's menu.
 */
public abstract class PagedToolbarFragment extends Fragment {
    private Toolbar toolbar;
    private ViewPager2 viewPager;
    private int currentInflatedMenu = -1;

    void invalidateOptionsMenuIfActive(Fragment child) {
        Fragment visibleChild = getChildFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());
        if (visibleChild == child) {
            if (currentInflatedMenu != viewPager.getCurrentItem()) {
                currentInflatedMenu = viewPager.getCurrentItem();
                toolbar.getMenu().clear();
                child.onCreateOptionsMenu(toolbar.getMenu(), getActivity().getMenuInflater());
            }
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
                if (child != null && getActivity() != null) {
                    toolbar.getMenu().clear();
                    child.onCreateOptionsMenu(toolbar.getMenu(), getActivity().getMenuInflater());
                    currentInflatedMenu = position;

                    child.onPrepareOptionsMenu(toolbar.getMenu());
                }
            }
        });
    }
}
