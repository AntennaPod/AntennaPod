package de.danoeh.antennapod.activity.gpoddernet;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.fragment.gpodnet.PodcastTopListFragment;
import de.danoeh.antennapod.fragment.gpodnet.SuggestionListFragment;
import de.danoeh.antennapod.fragment.gpodnet.TagListFragment;
import de.danoeh.antennapod.preferences.GpodnetPreferences;

/**
 * Created by daniel on 22.08.13.
 */
public class GpodnetMainActivity extends GpodnetActivity {
    private static final String TAG = "GPodnetMainActivity";

    private static final int POS_TAGS = 0;
    private static final int POS_TOPLIST = 1;
    private static final int POS_SUGGESTIONS = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.gpodnet_main);
        ViewPager viewpager = (ViewPager) findViewById(R.id.viewpager);
        viewpager.setAdapter(new PagerAdapter(getSupportFragmentManager()));
    }

    private class PagerAdapter extends FragmentStatePagerAdapter {

        private static final int NUM_PAGES_LOGGED_OUT = 2;
        private static final int NUM_PAGES_LOGGED_IN = 3;
        private final int NUM_PAGES;

        public PagerAdapter(FragmentManager fm) {
            super(fm);
            NUM_PAGES = NUM_PAGES_LOGGED_OUT;
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case POS_TAGS:
                    return new TagListFragment();
                case POS_TOPLIST:
                    return new PodcastTopListFragment();
                case POS_SUGGESTIONS:
                    return new SuggestionListFragment();
                default:
                    return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case POS_TAGS:
                    return getString(R.string.gpodnet_taglist_header);
                case POS_TOPLIST:
                    return getString(R.string.gpodnet_toplist_header);
                case POS_SUGGESTIONS:
                    return getString(R.string.gpodnet_suggestions_header);
                default:
                    return super.getPageTitle(position);
            }
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }
}
