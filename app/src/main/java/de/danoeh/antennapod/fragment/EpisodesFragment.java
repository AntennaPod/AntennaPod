package de.danoeh.antennapod.fragment;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTabHost;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.EpisodesPagerAdapter;

public class EpisodesFragment extends Fragment {

    public static final String TAG = "EpisodesFragment";

    //Mandatory Constructor
    public EpisodesFragment() {
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.episodes_fragment, container, false);
        ViewPager pager = (ViewPager)rootView.findViewById(R.id.viewpager);
        pager.setAdapter(new EpisodesPagerAdapter(getChildFragmentManager(), getActivity()));

        // Give the TabLayout the ViewPager
        TabLayout tabLayout = (TabLayout) rootView.findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(pager);

        return rootView;
    }

}
