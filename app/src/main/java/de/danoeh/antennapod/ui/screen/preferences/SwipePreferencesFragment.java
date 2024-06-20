package de.danoeh.antennapod.ui.screen.preferences;

import android.os.Bundle;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.ui.preferences.screen.AnimatedPreferenceFragment;
import de.danoeh.antennapod.ui.swipeactions.SwipeActionsDialog;
import de.danoeh.antennapod.ui.screen.AllEpisodesFragment;
import de.danoeh.antennapod.ui.screen.download.CompletedDownloadsFragment;
import de.danoeh.antennapod.ui.screen.feed.FeedItemlistFragment;
import de.danoeh.antennapod.ui.screen.InboxFragment;
import de.danoeh.antennapod.ui.screen.PlaybackHistoryFragment;
import de.danoeh.antennapod.ui.screen.queue.QueueFragment;

public class SwipePreferencesFragment extends AnimatedPreferenceFragment {
    private static final String PREF_SWIPE_QUEUE = "prefSwipeQueue";
    private static final String PREF_SWIPE_INBOX = "prefSwipeInbox";
    private static final String PREF_SWIPE_EPISODES = "prefSwipeEpisodes";
    private static final String PREF_SWIPE_DOWNLOADS = "prefSwipeDownloads";
    private static final String PREF_SWIPE_FEED = "prefSwipeFeed";
    private static final String PREF_SWIPE_HISTORY = "prefSwipeHistory";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_swipe);

        findPreference(PREF_SWIPE_QUEUE).setOnPreferenceClickListener(preference -> {
            new SwipeActionsDialog(requireContext(), QueueFragment.TAG).show(() -> { });
            return true;
        });
        findPreference(PREF_SWIPE_INBOX).setOnPreferenceClickListener(preference -> {
            new SwipeActionsDialog(requireContext(), InboxFragment.TAG).show(() -> { });
            return true;
        });
        findPreference(PREF_SWIPE_EPISODES).setOnPreferenceClickListener(preference -> {
            new SwipeActionsDialog(requireContext(), AllEpisodesFragment.TAG).show(() -> { });
            return true;
        });
        findPreference(PREF_SWIPE_DOWNLOADS).setOnPreferenceClickListener(preference -> {
            new SwipeActionsDialog(requireContext(), CompletedDownloadsFragment.TAG).show(() -> { });
            return true;
        });
        findPreference(PREF_SWIPE_FEED).setOnPreferenceClickListener(preference -> {
            new SwipeActionsDialog(requireContext(), FeedItemlistFragment.TAG).show(() -> { });
            return true;
        });
        findPreference(PREF_SWIPE_HISTORY).setOnPreferenceClickListener(preference -> {
            new SwipeActionsDialog(requireContext(), PlaybackHistoryFragment.TAG).show(() -> { });
            return true;
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        ((PreferenceActivity) getActivity()).getSupportActionBar().setTitle(R.string.swipeactions_label);
    }

}
