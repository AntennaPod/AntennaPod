package de.danoeh.antennapod.fragment.gpodnet;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.danoeh.antennapod.R;

/**
 * Main navigation hub for gpodder.net podcast directory
 */
public class GpodnetMainFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View root = inflater.inflate(R.layout.pager_fragment, container, false);
        ViewPager pager = (ViewPager) root.findViewById(R.id.pager);
        GpodnetPagerAdapter pagerAdapter = new GpodnetPagerAdapter(getChildFragmentManager(), getResources());
        pager.setAdapter(pagerAdapter);
        return root;
    }

    public class GpodnetPagerAdapter extends FragmentPagerAdapter {


        private static final int NUM_PAGES = 2;
        private static final int POS_TAGS = 0;
        private static final int POS_TOPLIST = 1;
        private static final int POS_SUGGESTIONS = 2;

        Resources resources;

        public GpodnetPagerAdapter(FragmentManager fm, Resources resources) {
            super(fm);
            this.resources = resources;
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
