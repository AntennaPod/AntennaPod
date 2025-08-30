package de.danoeh.antennapod.ui.screen.feed.preferences;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.appbar.MaterialToolbar;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.storage.database.DBReader;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.MaybeOnSubscribe;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Container fragment for feed settings fragment.
 * @see FeedSettingsPreferenceFragment for the actual preferences.
 */
public class FeedSettingsFragment extends Fragment {
    private static final String TAG = "FeedSettingsFragment";
    private static final String EXTRA_FEED_ID = "de.danoeh.antennapod.extra.feedId";

    private Disposable disposable;

    public static FeedSettingsFragment newInstance(Feed feed) {
        FeedSettingsFragment fragment = new FeedSettingsFragment();
        Bundle arguments = new Bundle();
        arguments.putLong(EXTRA_FEED_ID, feed.getId());
        fragment.setArguments(arguments);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.feedsettings, container, false);
        long feedId = getArguments().getLong(EXTRA_FEED_ID);

        MaterialToolbar toolbar = root.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        getParentFragmentManager().beginTransaction()
                .replace(R.id.settings_fragment_container,
                        FeedSettingsPreferenceFragment.newInstance(feedId), "settings_fragment")
                .commitAllowingStateLoss();

        disposable = Maybe.create((MaybeOnSubscribe<Feed>) emitter -> {
            Feed feed = DBReader.getFeed(feedId, false, 0, 0);
            if (feed != null) {
                emitter.onSuccess(feed);
            } else {
                emitter.onComplete();
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> toolbar.setSubtitle(result.getTitle()),
                        error -> Log.d(TAG, Log.getStackTraceString(error)),
                        () -> { });


        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (disposable != null) {
            disposable.dispose();
        }
    }
}
