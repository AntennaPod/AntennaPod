package de.danoeh.antennapod.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.event.DownloadEvent;
import de.danoeh.antennapod.core.menuhandler.MenuItemUtils;
import de.danoeh.antennapod.core.service.download.DownloadService;
import de.danoeh.antennapod.core.util.download.AutoUpdateManager;
import de.danoeh.antennapod.databinding.HomeFragmentBinding;
import de.danoeh.antennapod.event.FeedItemEvent;
import de.danoeh.antennapod.event.PlayerStatusEvent;
import de.danoeh.antennapod.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.fragment.SearchFragment;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.ui.home.sections.InboxSection;
import de.danoeh.antennapod.ui.home.sections.QueueSection;
import de.danoeh.antennapod.ui.home.sections.StatisticsSection;
import de.danoeh.antennapod.ui.home.sections.SubscriptionsSection;
import de.danoeh.antennapod.ui.home.sections.SurpriseSection;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Shows unread or recently published episodes
 */
public class HomeFragment extends Fragment implements Toolbar.OnMenuItemClickListener {

    public static final String TAG = "HomeFragment";
    public static final String PREF_NAME = "PrefHomeFragment";
    public static final String PREF_HIDDEN_SECTIONS = "PrefHomeSectionsString";
    public static final String PREF_FRAGMENT = "PrefHomeFragment";

    private FeedItem selectedItem = null;
    private static final String KEY_UP_ARROW = "up_arrow";
    private boolean displayUpArrow;
    private HomeFragmentBinding viewBinding;
    private boolean isUpdatingFeeds = false;

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
        ((MainActivity) requireActivity()).setupToolbarToggle(viewBinding.toolbar, displayUpArrow);
        refreshToolbarState();
        populateSectionList();
        return viewBinding.getRoot();
    }

    private void populateSectionList() {
        viewBinding.homeContainer.removeAllViews();
        List<String> hiddenSections = getHiddenSections(getContext());
        String[] sectionTags = getResources().getStringArray(R.array.home_section_tags);
        for (String sectionTag : sectionTags) {
            if (hiddenSections.contains(sectionTag)) {
                continue;
            }
            HomeSection section;
            switch (sectionTag) {
                case InboxSection.TAG:
                    section = new InboxSection(this);
                    break;
                default: // Fall-through
                case QueueSection.TAG:
                    section = new QueueSection(this);
                    break;
                case SubscriptionsSection.TAG:
                    section = new SubscriptionsSection(this);
                    break;
                case SurpriseSection.TAG:
                    section = new SurpriseSection(this);
                    break;
                case StatisticsSection.TAG:
                    section = new StatisticsSection(this);
                    break;
            }
            section.addSectionTo(viewBinding.homeContainer);
        }
    }

    public static List<String> getHiddenSections(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(HomeFragment.PREF_NAME, Context.MODE_PRIVATE);
        String hiddenSectionsString = prefs.getString(HomeFragment.PREF_HIDDEN_SECTIONS, "");
        return new ArrayList<>(Arrays.asList(TextUtils.split(hiddenSectionsString, ",")));
    }

    private final MenuItemUtils.UpdateRefreshMenuItemChecker updateRefreshMenuItemChecker =
            () -> DownloadService.isRunning && DownloadService.isDownloadingFeeds();

    private void refreshToolbarState() {
        isUpdatingFeeds = MenuItemUtils.updateRefreshMenuItem(viewBinding.toolbar.getMenu(),
                R.id.refresh_item, updateRefreshMenuItemChecker);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(DownloadEvent event) {
        Log.d(TAG, "onEventMainThread() called with DownloadEvent");
        if (event.hasChangedFeedUpdateStatus(isUpdatingFeeds)) {
            refreshToolbarState();
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.homesettings_items) {
            HomeSectionsSettingsDialog.open(getContext(), (dialogInterface, i) -> populateSectionList());
            return true;
        } else if (item.getItemId() == R.id.refresh_item) {
            AutoUpdateManager.runImmediate(requireContext());
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
    public boolean onContextItemSelected(@NonNull MenuItem menuItem) {
        if (selectedItem == null) {
            //can only happen if it comes from the fragment
            return getChildFragmentManager().findFragmentByTag(PREF_FRAGMENT).onContextItemSelected(menuItem);
        }
        final FeedItem item = selectedItem;
        //reset selectedItem so we know whether it came from the fragment next time
        selectedItem = null;
        return FeedItemMenuHandler.onMenuItemClicked(this, menuItem.getItemId(), item);
    }

    public void setSelectedItem(FeedItem feedItem) {
        selectedItem = feedItem;
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
    public void onEventMainThread(FeedItemEvent event) {
        //updateSections(HomeSection.UpdateEvents.FEED_ITEM);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUnreadItemsChanged(UnreadItemsUpdateEvent event) {
        //updateSections(HomeSection.UpdateEvents.UNREAD);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(PlaybackPositionEvent event) {
        //updateSections(HomeSection.UpdateEvents.QUEUE);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerStatusChanged(PlayerStatusEvent event) {
        //updateSections(HomeSection.UpdateEvents.QUEUE);
    }

    @Override
    public void onResume() {
        super.onResume();
        //update all sections, especially play/pauseStatus in QueueSection
        //updateSections(HomeSection.UpdateEvents.QUEUE, true);
    }
}