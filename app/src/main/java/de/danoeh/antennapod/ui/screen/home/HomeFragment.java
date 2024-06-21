package de.danoeh.antennapod.ui.screen.home;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.databinding.HomeFragmentBinding;
import de.danoeh.antennapod.event.FeedListUpdateEvent;
import de.danoeh.antennapod.event.FeedUpdateRunningEvent;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.net.download.serviceinterface.FeedUpdateManager;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.ui.echo.EchoConfig;
import de.danoeh.antennapod.ui.screen.SearchFragment;
import de.danoeh.antennapod.ui.screen.home.sections.AllowNotificationsSection;
import de.danoeh.antennapod.ui.screen.home.sections.DownloadsSection;
import de.danoeh.antennapod.ui.screen.home.sections.EchoSection;
import de.danoeh.antennapod.ui.screen.home.sections.EpisodesSurpriseSection;
import de.danoeh.antennapod.ui.screen.home.sections.InboxSection;
import de.danoeh.antennapod.ui.screen.home.sections.QueueSection;
import de.danoeh.antennapod.ui.screen.home.sections.SubscriptionsSection;
import de.danoeh.antennapod.ui.screen.home.settingsdialog.HomePreferences;
import de.danoeh.antennapod.ui.screen.home.settingsdialog.HomeSectionsSettingsDialog;
import de.danoeh.antennapod.ui.view.LiftOnScrollListener;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Calendar;
import java.util.List;

/**
 * Shows unread or recently published episodes
 */
public class HomeFragment extends Fragment implements Toolbar.OnMenuItemClickListener {

    public static final String TAG = "HomeFragment";
    public static final String PREF_NAME = "PrefHomeFragment";
    public static final String PREF_DISABLE_NOTIFICATION_PERMISSION_NAG = "DisableNotificationPermissionNag";
    public static final String PREF_HIDE_ECHO = "HideEcho";

    private static final String KEY_UP_ARROW = "up_arrow";
    private boolean displayUpArrow;
    private HomeFragmentBinding viewBinding;
    private Disposable disposable;

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        viewBinding = HomeFragmentBinding.inflate(inflater);
        viewBinding.toolbar.inflateMenu(R.menu.home);
        viewBinding.toolbar.setOnMenuItemClickListener(this);
        if (savedInstanceState != null) {
            displayUpArrow = savedInstanceState.getBoolean(KEY_UP_ARROW);
        }
        viewBinding.homeScrollView.setOnScrollChangeListener(new LiftOnScrollListener(viewBinding.appbar));
        ((MainActivity) requireActivity()).setupToolbarToggle(viewBinding.toolbar, displayUpArrow);
        populateSectionList();
        updateWelcomeScreenVisibility();

        viewBinding.swipeRefresh.setDistanceToTriggerSync(getResources().getInteger(R.integer.swipe_refresh_distance));
        viewBinding.swipeRefresh.setOnRefreshListener(() ->
                FeedUpdateManager.getInstance().runOnceOrAsk(requireContext()));

        return viewBinding.getRoot();
    }

    private void populateSectionList() {
        viewBinding.homeContainer.removeAllViews();

        SharedPreferences prefs = getContext().getSharedPreferences(HomeFragment.PREF_NAME, Context.MODE_PRIVATE);
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            if (!prefs.getBoolean(HomeFragment.PREF_DISABLE_NOTIFICATION_PERMISSION_NAG, false)) {
                addSection(new AllowNotificationsSection());
            }
        }
        if (Calendar.getInstance().get(Calendar.YEAR) == EchoConfig.RELEASE_YEAR
                && Calendar.getInstance().get(Calendar.MONTH) == Calendar.DECEMBER
                && Calendar.getInstance().get(Calendar.DAY_OF_MONTH) >= 10
                && prefs.getInt(PREF_HIDE_ECHO, 0) != EchoConfig.RELEASE_YEAR) {
            addSection(new EchoSection());
        }

        List<String> sectionTags = HomePreferences.getSortedSectionTags(getContext());
        for (String sectionTag : sectionTags) {
            addSection(getSection(sectionTag));
        }
    }

    private void addSection(Fragment section) {
        FragmentContainerView containerView = new FragmentContainerView(getContext());
        containerView.setId(View.generateViewId());
        viewBinding.homeContainer.addView(containerView);
        getChildFragmentManager().beginTransaction().add(containerView.getId(), section).commit();
    }

    private Fragment getSection(String tag) {
        switch (tag) {
            case QueueSection.TAG:
                return new QueueSection();
            case InboxSection.TAG:
                return new InboxSection();
            case EpisodesSurpriseSection.TAG:
                return new EpisodesSurpriseSection();
            case SubscriptionsSection.TAG:
                return new SubscriptionsSection();
            case DownloadsSection.TAG:
                return new DownloadsSection();
            default:
                return null;
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FeedUpdateRunningEvent event) {
        viewBinding.swipeRefresh.setRefreshing(event.isFeedUpdateRunning);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.homesettings_items) {
            HomeSectionsSettingsDialog.open(getContext(), this::populateSectionList);
            return true;
        } else if (item.getItemId() == R.id.refresh_item) {
            FeedUpdateManager.getInstance().runOnceOrAsk(requireContext());
            return true;
        } else if (item.getItemId() == R.id.action_search) {
            ((MainActivity) getActivity()).loadChildFragment(SearchFragment.newInstance());
            return true;
        }
        return false;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(KEY_UP_ARROW, displayUpArrow);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFeedListChanged(FeedListUpdateEvent event) {
        updateWelcomeScreenVisibility();
    }

    private void updateWelcomeScreenVisibility() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(() -> DBReader.getTotalEpisodeCount(FeedItemFilter.unfiltered()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(numEpisodes -> {
                    viewBinding.welcomeContainer.setVisibility(numEpisodes == 0 ? View.VISIBLE : View.GONE);
                    viewBinding.homeContainer.setVisibility(numEpisodes == 0 ? View.GONE : View.VISIBLE);
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

}
