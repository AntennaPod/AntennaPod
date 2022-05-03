package de.danoeh.antennapod.ui.home.sections;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.databinding.HomeWelcomeBannerBinding;
import de.danoeh.antennapod.event.FeedListUpdateEvent;
import de.danoeh.antennapod.fragment.AddFeedFragment;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class WelcomeBannerSection extends Fragment {
    public static final String TAG = "WelcomeBannerSection";
    private HomeWelcomeBannerBinding viewBinding;
    private Disposable disposable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        viewBinding = HomeWelcomeBannerBinding.inflate(inflater);
        viewBinding.addFeedButton.setOnClickListener(v ->
                ((MainActivity) requireActivity()).loadChildFragment(new AddFeedFragment()));
        loadItems();
        return viewBinding.getRoot();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFeedListChanged(FeedListUpdateEvent event) {
        loadItems();
    }

    private void loadItems() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(() -> DBReader.getNavDrawerData().items.size())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(numSubscriptions ->
                            viewBinding.getRoot().setVisibility(numSubscriptions == 0 ? View.VISIBLE : View.GONE),
                        error -> Log.e(TAG, Log.getStackTraceString(error)));
    }
}
