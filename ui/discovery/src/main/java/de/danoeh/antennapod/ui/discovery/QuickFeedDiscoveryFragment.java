package de.danoeh.antennapod.ui.discovery;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import androidx.fragment.app.Fragment;
import de.danoeh.antennapod.net.discovery.BuildConfig;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.event.DiscoveryDefaultUpdateEvent;
import de.danoeh.antennapod.net.discovery.ItunesTopListLoader;
import de.danoeh.antennapod.net.discovery.PodcastSearchResult;
import de.danoeh.antennapod.ui.appstartintent.MainActivityStarter;
import de.danoeh.antennapod.ui.appstartintent.OnlineFeedviewActivityStarter;
import de.danoeh.antennapod.ui.discovery.databinding.QuickFeedDiscoveryBinding;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;

public class QuickFeedDiscoveryFragment extends Fragment implements AdapterView.OnItemClickListener {
    private static final String TAG = "FeedDiscoveryFragment";
    private static final int NUM_SUGGESTIONS = 12;

    private Disposable disposable;
    private FeedDiscoverAdapter adapter;
    private QuickFeedDiscoveryBinding viewBinding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        viewBinding = QuickFeedDiscoveryBinding.inflate(inflater);
        viewBinding.discoverMore.setOnClickListener(v -> startActivity(new MainActivityStarter(getContext())
                .withFragmentLoaded(DiscoveryFragment.TAG)
                .withAddToBackStack()
                .getIntent()));

        adapter = new FeedDiscoverAdapter(getActivity());
        viewBinding.discoverGrid.setAdapter(adapter);
        viewBinding.discoverGrid.setOnItemClickListener(this);

        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        float screenWidthDp = displayMetrics.widthPixels / displayMetrics.density;
        if (screenWidthDp > 600) {
            viewBinding.discoverGrid.setNumColumns(6);
        } else {
            viewBinding.discoverGrid.setNumColumns(4);
        }

        // Fill with dummy elements to have a fixed height and
        // prevent the UI elements below from jumping on slow connections
        List<PodcastSearchResult> dummies = new ArrayList<>();
        for (int i = 0; i < NUM_SUGGESTIONS; i++) {
            dummies.add(PodcastSearchResult.dummy());
        }

        adapter.updateData(dummies);
        loadToplist();

        EventBus.getDefault().register(this);
        return viewBinding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewBinding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        if (disposable != null) {
            disposable.dispose();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    public void onDiscoveryDefaultUpdateEvent(DiscoveryDefaultUpdateEvent event) {
        loadToplist();
    }

    private void loadToplist() {
        viewBinding.errorContainer.setVisibility(View.GONE);
        viewBinding.errorRetryButton.setVisibility(View.INVISIBLE);
        viewBinding.errorRetryButton.setText(R.string.retry_label);
        viewBinding.poweredByLabel.setVisibility(View.VISIBLE);

        ItunesTopListLoader loader = new ItunesTopListLoader(getContext());
        SharedPreferences prefs = getActivity().getSharedPreferences(ItunesTopListLoader.PREFS, MODE_PRIVATE);
        String countryCode = prefs.getString(ItunesTopListLoader.PREF_KEY_COUNTRY_CODE,
                Locale.getDefault().getCountry());
        if (prefs.getBoolean(ItunesTopListLoader.PREF_KEY_HIDDEN_DISCOVERY_COUNTRY, false)) {
            viewBinding.errorLabel.setText(R.string.discover_is_hidden);
            viewBinding.errorContainer.setVisibility(View.VISIBLE);
            viewBinding.discoverGrid.setVisibility(View.GONE);
            viewBinding.errorRetryButton.setVisibility(View.GONE);
            viewBinding.poweredByLabel.setVisibility(View.GONE);
            return;
        }
        //noinspection ConstantConditions
        if (BuildConfig.FLAVOR.equals("free") && prefs.getBoolean(ItunesTopListLoader.PREF_KEY_NEEDS_CONFIRM, true)) {
            viewBinding.errorLabel.setText("");
            viewBinding.errorContainer.setVisibility(View.VISIBLE);
            viewBinding.discoverGrid.setVisibility(View.VISIBLE);
            viewBinding.errorRetryButton.setVisibility(View.VISIBLE);
            viewBinding.errorRetryButton.setText(R.string.discover_confirm);
            viewBinding.poweredByLabel.setVisibility(View.VISIBLE);
            viewBinding.errorRetryButton.setOnClickListener(v -> {
                prefs.edit().putBoolean(ItunesTopListLoader.PREF_KEY_NEEDS_CONFIRM, false).apply();
                loadToplist();
            });
            return;
        }

        disposable = Observable.fromCallable(() ->
                        loader.loadToplist(countryCode, NUM_SUGGESTIONS, DBReader.getFeedList()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    podcasts -> {
                        viewBinding.errorContainer.setVisibility(View.GONE);
                        if (podcasts.isEmpty()) {
                            viewBinding.errorLabel.setText(getResources().getText(R.string.search_status_no_results));
                            viewBinding.errorContainer.setVisibility(View.VISIBLE);
                            viewBinding.discoverGrid.setVisibility(View.INVISIBLE);
                        } else {
                            viewBinding.discoverGrid.setVisibility(View.VISIBLE);
                            adapter.updateData(podcasts);
                        }
                    }, error -> {
                        Log.e(TAG, Log.getStackTraceString(error));
                        viewBinding.errorLabel.setText(error.getLocalizedMessage());
                        viewBinding.errorContainer.setVisibility(View.VISIBLE);
                        viewBinding.discoverGrid.setVisibility(View.INVISIBLE);
                        viewBinding.errorRetryButton.setVisibility(View.VISIBLE);
                        viewBinding.errorRetryButton.setOnClickListener(v -> loadToplist());
                    });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
        PodcastSearchResult podcast = adapter.getItem(position);
        if (TextUtils.isEmpty(podcast.feedUrl)) {
            return;
        }
        startActivity(new OnlineFeedviewActivityStarter(getContext(), podcast.feedUrl).getIntent());
    }
}
