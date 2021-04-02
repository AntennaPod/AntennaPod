package de.danoeh.antennapod.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;

/**
 * Hosts Cover and Description Fragment
 */
public class EpisodeFragment extends Fragment {

    private static final String TAG = "EpisodeFragment";
    private static final int POS_COVER = 0;
    private static final int POS_DESCR = 1;
    private static final int NUM_CONTENT_FRAGMENTS = 2;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.episode_fragment, container, false);
        ViewPager2 pager = root.findViewById(R.id.verticalpager);
        pager.setAdapter(new EpisodeFragment.EpisodePagerAdapter(this));
        // Required for getChildAt(int) in ViewPagerBottomSheetBehavior to return the correct page
        pager.setOffscreenPageLimit((int) NUM_CONTENT_FRAGMENTS);
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                pager.post(() -> {
                    if (getActivity() != null) {
                        // By the time this is posted, the activity might be closed again.
                        ((MainActivity) getActivity()).getBottomSheet().updateScrollingChild();
                    }
                });
            }
        });
        return root;
    }

    private static class EpisodePagerAdapter extends FragmentStateAdapter {
        private static final String TAG = "AudioPlayerPagerAdapter";

        public EpisodePagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Log.d(TAG, "getItem(" + position + ")");
            switch (position) {
                case POS_COVER:
                    return new CoverFragment();
                default:
                case POS_DESCR:
                    return new ItemDescriptionFragment();
            }
        }

        @Override
        public int getItemCount() {
            return NUM_CONTENT_FRAGMENTS;
        }
    }
}

