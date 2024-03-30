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
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import de.danoeh.antennapod.net.discovery.BuildConfig;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.event.DiscoveryDefaultUpdateEvent;
import de.danoeh.antennapod.net.discovery.ItunesTopListLoader;
import de.danoeh.antennapod.net.discovery.PodcastSearchResult;
import de.danoeh.antennapod.ui.appstartintent.MainActivityStarter;
import de.danoeh.antennapod.ui.appstartintent.OnlineFeedviewActivityStarter;
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
    private GridView discoverGridLayout;
    private TextView errorTextView;
    private TextView poweredByTextView;
    private LinearLayout errorView;
    private Button errorRetry;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View root = inflater.inflate(R.layout.quick_feed_discovery, container, false);
        View discoverMore = root.findViewById(R.id.discover_more);
        discoverMore.setOnClickListener(v -> startActivity(new MainActivityStarter(getContext())
                .withFragmentLoaded(DiscoveryFragment.TAG)
                .withAddToBackStack()
                .getIntent()));

        discoverGridLayout = root.findViewById(R.id.discover_grid);
        errorView = root.findViewById(R.id.discover_error);
        errorTextView = root.findViewById(R.id.discover_error_txtV);
        errorRetry = root.findViewById(R.id.discover_error_retry_btn);
        poweredByTextView = root.findViewById(R.id.discover_powered_by_itunes);

        adapter = new FeedDiscoverAdapter(getActivity());
        discoverGridLayout.setAdapter(adapter);
        discoverGridLayout.setOnItemClickListener(this);

        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        float screenWidthDp = displayMetrics.widthPixels / displayMetrics.density;
        if (screenWidthDp > 600) {
            discoverGridLayout.setNumColumns(6);
        } else {
            discoverGridLayout.setNumColumns(4);
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
        return root;
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
        errorView.setVisibility(View.GONE);
        errorRetry.setVisibility(View.INVISIBLE);
        errorRetry.setText(R.string.retry_label);
        poweredByTextView.setVisibility(View.VISIBLE);

        ItunesTopListLoader loader = new ItunesTopListLoader(getContext());
        SharedPreferences prefs = getActivity().getSharedPreferences(ItunesTopListLoader.PREFS, MODE_PRIVATE);
        String countryCode = prefs.getString(ItunesTopListLoader.PREF_KEY_COUNTRY_CODE,
                Locale.getDefault().getCountry());
        if (prefs.getBoolean(ItunesTopListLoader.PREF_KEY_HIDDEN_DISCOVERY_COUNTRY, false)) {
            errorTextView.setText(R.string.discover_is_hidden);
            errorView.setVisibility(View.VISIBLE);
            discoverGridLayout.setVisibility(View.GONE);
            errorRetry.setVisibility(View.GONE);
            poweredByTextView.setVisibility(View.GONE);
            return;
        }
        //noinspection ConstantConditions
        if (BuildConfig.FLAVOR.equals("free") && prefs.getBoolean(ItunesTopListLoader.PREF_KEY_NEEDS_CONFIRM, true)) {
            errorTextView.setText("");
            errorView.setVisibility(View.VISIBLE);
            discoverGridLayout.setVisibility(View.VISIBLE);
            errorRetry.setVisibility(View.VISIBLE);
            errorRetry.setText(R.string.discover_confirm);
            poweredByTextView.setVisibility(View.VISIBLE);
            errorRetry.setOnClickListener(v -> {
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
                        errorView.setVisibility(View.GONE);
                        if (podcasts.size() == 0) {
                            errorTextView.setText(getResources().getText(R.string.search_status_no_results));
                            errorView.setVisibility(View.VISIBLE);
                            discoverGridLayout.setVisibility(View.INVISIBLE);
                        } else {
                            discoverGridLayout.setVisibility(View.VISIBLE);
                            adapter.updateData(podcasts);
                        }
                    }, error -> {
                        Log.e(TAG, Log.getStackTraceString(error));
                        errorTextView.setText(error.getLocalizedMessage());
                        errorView.setVisibility(View.VISIBLE);
                        discoverGridLayout.setVisibility(View.INVISIBLE);
                        errorRetry.setVisibility(View.VISIBLE);
                        errorRetry.setOnClickListener(v -> loadToplist());
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
