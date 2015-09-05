package de.danoeh.antennapod.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTabHost;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.danoeh.antennapod.R;

public class EpisodesFragment extends Fragment {

    public static final String TAG = "EpisodesFragment";

    private FragmentTabHost mTabHost;

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


        mTabHost = (FragmentTabHost) rootView.findViewById(android.R.id.tabhost);
        mTabHost.setup(getActivity(), getChildFragmentManager(), R.id.realtabcontent);


        mTabHost.addTab(mTabHost.newTabSpec(NewEpisodesFragment.TAG).setIndicator(
                        getResources().getString(R.string.new_episodes_label)),
                        NewEpisodesFragment.class, null);

        mTabHost.addTab(mTabHost.newTabSpec(AllEpisodesFragment.TAG).setIndicator(
                        getResources().getString(R.string.all_episodes_label)),
                        AllEpisodesFragment.class, null);

        return rootView;
    }

}
