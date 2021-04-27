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

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.util.ChapterUtils;
import de.danoeh.antennapod.core.util.playback.Playable;
import de.danoeh.antennapod.core.util.playback.PlaybackController;
import io.reactivex.Maybe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Hosts Cover and Description Fragment
 */
public class EpisodeFragment extends Fragment {

    private static final String TAG = "EpisodeFragment";
    private static final int POS_DESCR = 0;
    private static final int POS_TRANSC = 1;
    private int NUM_CONTENT_FRAGMENTS() { return tabs.size(); }
    public ArrayList<Integer> tabs = new ArrayList();

    private TabLayout tabLayout;
    private TabLayoutMediator tabLayoutMediator;

    private PlaybackController controller;
    private Disposable disposable;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.episode_fragment, container, false);
        ViewPager2 pager = root.findViewById(R.id.horizontalpager);
        pager.setAdapter(new EpisodeFragment.EpisodePagerAdapter(this));
        // Required for getChildAt(int) in ViewPagerBottomSheetBehavior to return the correct page
        pager.setOffscreenPageLimit((int) NUM_CONTENT_FRAGMENTS());
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

        tabs.add(POS_DESCR);

        tabLayout = root.findViewById(R.id.sliding_tabs);
        tabLayoutMediator = new TabLayoutMediator(tabLayout, pager, (tab, position) -> {
            tab.view.setAlpha(1.0f);
            switch (tabs.get(position)) {
                case POS_DESCR:
                    tab.setText(R.string.episode_label);
                    break;
                case POS_TRANSC:
                    //TODO
                    break;
                default:
                    break;
            }
        });
        tabLayoutMediator.attach();
        
        return root;
    }

    private void reattachTabLayout() {
        if (tabs.size() > 1) {
            tabLayoutMediator.detach();
            tabLayoutMediator.attach();

            tabLayout.setVisibility(View.VISIBLE);
        } else {
            tabLayout.setVisibility(View.GONE);
        }
    }

    private void updateUi(Playable media) {
        tabs.clear();
        tabs.add(POS_DESCR);
        /*if (media != null && media.getTranscript()) {
            tabs.add(POS_TRANSC);
        }*/
        reattachTabLayout();
    }

    private class EpisodePagerAdapter extends FragmentStateAdapter {
        private static final String TAG = "AudioPlayerPagerAdapter";

        public EpisodePagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Log.d(TAG, "getItem(" + position + ")");
            switch (tabs.get(position)) {
                case POS_TRANSC:
                    //return new TranscriptFragment();
                default:
                case POS_DESCR:
                    return new ItemDescriptionFragment();
            }
        }

        @Override
        public int getItemCount() {
            return NUM_CONTENT_FRAGMENTS();
        }
    }

    private void loadMediaInfo() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Maybe.create(emitter -> {
            Playable media = controller.getMedia();
            if (media != null) {
                emitter.onSuccess(media);
            } else {
                emitter.onComplete();
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(media -> updateUi((Playable) media),
                        error -> Log.e(TAG, Log.getStackTraceString(error)),
                        () -> updateUi(null));
    }

    @Override
    public void onStart() {
        super.onStart();
        controller = new PlaybackController(getActivity()) {
            @Override
            public void loadMediaInfo() {
                EpisodeFragment.this.loadMediaInfo();
            }
        };
        controller.init();
        loadMediaInfo();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        if (disposable != null) {
            disposable.dispose();
        }
        controller.release();
        controller = null;
        EventBus.getDefault().unregister(this);
    }
}

