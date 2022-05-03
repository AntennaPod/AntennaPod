package de.danoeh.antennapod.fragment.preferences;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.PreferenceActivity;
import de.danoeh.antennapod.dialog.SwipeActionsDialog;
import de.danoeh.antennapod.fragment.FeedItemlistFragment;
import de.danoeh.antennapod.fragment.InboxFragment;
import de.danoeh.antennapod.fragment.QueueFragment;

public class SwipePreferencesFragment extends PreferenceFragmentCompat {
    private static final String PREF_SWIPE_FEED = "prefSwipeFeed";
    private static final String PREF_SWIPE_QUEUE = "prefSwipeQueue";
    private static final String PREF_SWIPE_INBOX = "prefSwipeInbox";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_swipe);

        findPreference(PREF_SWIPE_FEED).setOnPreferenceClickListener(preference -> {
            new SwipeActionsDialog(requireContext(), FeedItemlistFragment.TAG).show(() -> { });
            return true;
        });
        findPreference(PREF_SWIPE_QUEUE).setOnPreferenceClickListener(preference -> {
            new SwipeActionsDialog(requireContext(), QueueFragment.TAG).show(() -> { });
            return true;
        });
        findPreference(PREF_SWIPE_INBOX).setOnPreferenceClickListener(preference -> {
            new SwipeActionsDialog(requireContext(), InboxFragment.TAG).show(() -> { });
            return true;
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        ((PreferenceActivity) getActivity()).getSupportActionBar().setTitle(R.string.swipeactions_label);
    }

}
