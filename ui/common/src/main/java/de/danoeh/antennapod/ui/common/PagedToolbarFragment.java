package de.danoeh.antennapod.ui.common;

import com.google.android.material.appbar.MaterialToolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

/**
 * Fragment with a ViewPager where the displayed items influence the top toolbar's menu.
 * All items share the same general menu items and are just allowed to show/hide them.
 */
public abstract class PagedToolbarFragment extends Fragment {

    protected void setupPagedToolbar(final MaterialToolbar toolbar, final ViewPager2 viewPager) {

        toolbar.setOnMenuItemClickListener(item -> {
            if (this.onOptionsItemSelected(item)) {
                return true;
            }
            Fragment child = getChildFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());
            if (child != null) {
                return child.onOptionsItemSelected(item);
            }
            return false;
        });
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            private int selectedPosition = 0;

            @Override
            public void onPageSelected(int position) {
                selectedPosition = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    Fragment child = getChildFragmentManager().findFragmentByTag("f" + selectedPosition);
                    if (child != null) {
                        child.onPrepareOptionsMenu(toolbar.getMenu());
                    }
                }
            }
        });
    }
}
