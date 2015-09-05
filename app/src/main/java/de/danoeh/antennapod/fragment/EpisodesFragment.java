package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.EpisodesPagerAdapter;

public class EpisodesFragment extends Fragment {

    public static final String TAG = "EpisodesFragment";
    private static final String PREF_LAST_TAB_POSITION = "tab_position";
    private TabLayout tabLayout;
    private ViewPager viewPager;

    //Mandatory Constructor
    public EpisodesFragment() {
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        ((MainActivity) getActivity()).getSupportActionBar().setTitle(R.string.episodes_label);

        View rootView = inflater.inflate(R.layout.episodes_fragment, container, false);
        viewPager = (ViewPager)rootView.findViewById(R.id.viewpager);
        viewPager.setAdapter(new EpisodesPagerAdapter(getChildFragmentManager(), getActivity()));

        // Give the TabLayout the ViewPager
        tabLayout = (TabLayout) rootView.findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(viewPager);

        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
        // save our tab selection
        SharedPreferences prefs = getActivity().getSharedPreferences(TAG, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_LAST_TAB_POSITION, tabLayout.getSelectedTabPosition());
        editor.apply();
    }

    @Override
    public void onStart() {
        super.onStart();

        // restore our last position
        SharedPreferences prefs = getActivity().getSharedPreferences(TAG, Context.MODE_PRIVATE);
        int lastPosition = prefs.getInt(PREF_LAST_TAB_POSITION, 0);
        viewPager.setCurrentItem(lastPosition);
    }

}
