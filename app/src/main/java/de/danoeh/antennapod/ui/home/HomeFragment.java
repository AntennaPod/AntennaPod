package de.danoeh.antennapod.ui.home;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
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

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.menuhandler.MenuItemUtils;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.download.FeedUpdateManager;
import de.danoeh.antennapod.databinding.HomeFragmentBinding;
import de.danoeh.antennapod.event.FeedListUpdateEvent;
import de.danoeh.antennapod.event.FeedUpdateRunningEvent;
import de.danoeh.antennapod.fragment.SearchFragment;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.home.sections.AllowNotificationsSection;
import de.danoeh.antennapod.ui.home.sections.DownloadsSection;
import de.danoeh.antennapod.ui.home.sections.EpisodesSurpriseSection;
import de.danoeh.antennapod.ui.home.sections.InboxSection;
import de.danoeh.antennapod.ui.home.sections.QueueSection;
import de.danoeh.antennapod.ui.home.sections.SubscriptionsSection;
import de.danoeh.antennapod.view.LiftOnScrollListener;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Shows unread or recently published episodes
 */
public class HomeFragment extends Fragment implements Toolbar.OnMenuItemClickListener {

    public static final String TAG = "HomeFragment";
    public static final String PREF_NAME = "PrefHomeFragment";
    public static final String PREF_HIDDEN_SECTIONS = "PrefHomeSectionsString";
    public static final String PREF_DISABLE_NOTIFICATION_PERMISSION_NAG = "DisableNotificationPermissionNag";

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
        viewBinding.swipeRefresh.setOnRefreshListener(() -> {
            FeedUpdateManager.runOnceOrAsk(requireContext());
            new Handler(Looper.getMainLooper()).postDelayed(() -> viewBinding.swipeRefresh.setRefreshing(false),
                    getResources().getInteger(R.integer.swipe_to_refresh_duration_in_ms));
        });

        return viewBinding.getRoot();
    }

    private void populateSectionList() {
        viewBinding.homeContainer.removeAllViews();

        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            SharedPreferences prefs = getContext().getSharedPreferences(HomeFragment.PREF_NAME, Context.MODE_PRIVATE);
            if (!prefs.getBoolean(HomeFragment.PREF_DISABLE_NOTIFICATION_PERMISSION_NAG, false)) {
                addSection(new AllowNotificationsSection());
            }
        }

        List<String> hiddenSections = getHiddenSections(getContext());
        String[] sectionTags = getResources().getStringArray(R.array.home_section_tags);
        for (String sectionTag : sectionTags) {
            if (hiddenSections.contains(sectionTag)) {
                continue;
            }
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

    public static List<String> getHiddenSections(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(HomeFragment.PREF_NAME, Context.MODE_PRIVATE);
        String hiddenSectionsString = prefs.getString(HomeFragment.PREF_HIDDEN_SECTIONS, "");
        return new ArrayList<>(Arrays.asList(TextUtils.split(hiddenSectionsString, ",")));
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FeedUpdateRunningEvent event) {
        MenuItemUtils.updateRefreshMenuItem(viewBinding.toolbar.getMenu(),
                R.id.refresh_item, event.isFeedUpdateRunning);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.homesettings_items) {
            HomeSectionsSettingsDialog.open(getContext(), (dialogInterface, i) -> populateSectionList());
            return true;
        } else if (item.getItemId() == R.id.refresh_item) {
            FeedUpdateManager.runOnceOrAsk(requireContext());
            return true;
        } else if (item.getItemId() == R.id.action_search) {
            ((MainActivity) getActivity()).loadChildFragment(SearchFragment.newInstance());
            return true;
        }
        return super.onOptionsItemSelected(item);
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
        disposable = Observable.fromCallable(() ->
                        DBReader.getNavDrawerData(UserPreferences.getSubscriptionsFilter()).items.size())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(numSubscriptions -> {
                    viewBinding.welcomeContainer.setVisibility(numSubscriptions == 0 ? View.VISIBLE : View.GONE);
                    viewBinding.homeContainer.setVisibility(numSubscriptions == 0 ? View.GONE : View.VISIBLE);
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

}
