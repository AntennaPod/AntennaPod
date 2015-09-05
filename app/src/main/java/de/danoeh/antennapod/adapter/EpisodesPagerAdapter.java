package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.fragment.AllEpisodesFragment;
import de.danoeh.antennapod.fragment.NewEpisodesFragment;

public class EpisodesPagerAdapter extends FragmentPagerAdapter {

    private final Context context;

    private String tabTags[] = new String[] {AllEpisodesFragment.TAG, NewEpisodesFragment.TAG};
    private String tabTitles[] = new String[tabTags.length];

    public EpisodesPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
        tabTitles = new String[tabTags.length];
        for (int i = 0; i < tabTags.length; i++) {
            String title = null;
            switch (tabTags[i]) {
                case AllEpisodesFragment.TAG:
                    title = context.getResources().getString(R.string.all_episodes_label);
                    break;
                case NewEpisodesFragment.TAG:
                    title = context.getResources().getString(R.string.new_episodes_label);
                    break;
            }
            tabTitles[i] = title;
        }
    }

    @Override
    public Fragment getItem(int position) {
        String tag = tabTags[position];
        switch (tag) {
            case AllEpisodesFragment.TAG:
                return new AllEpisodesFragment();
            case NewEpisodesFragment.TAG:
                return new NewEpisodesFragment();
        }
        return null;
    }

    @Override
    public int getCount() {
        return tabTags.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabTitles[position];
    }
}
