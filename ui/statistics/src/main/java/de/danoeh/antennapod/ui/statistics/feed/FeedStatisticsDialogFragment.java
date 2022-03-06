package de.danoeh.antennapod.ui.statistics.feed;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import de.danoeh.antennapod.ui.statistics.R;

public class FeedStatisticsDialogFragment extends DialogFragment {
    private static final String EXTRA_FEED_ID = "de.danoeh.antennapod.extra.feedId";
    private static final String EXTRA_FEED_TITLE = "de.danoeh.antennapod.extra.feedTitle";

    public static FeedStatisticsDialogFragment newInstance(long feedId, String feedTitle) {
        FeedStatisticsDialogFragment fragment = new FeedStatisticsDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putLong(EXTRA_FEED_ID, feedId);
        arguments.putString(EXTRA_FEED_TITLE, feedTitle);
        fragment.setArguments(arguments);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        dialog.setPositiveButton(android.R.string.ok, null);
        dialog.setTitle(getArguments().getString(EXTRA_FEED_TITLE));
        dialog.setView(R.layout.feed_statistics_dialog);
        return dialog.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        long feedId = getArguments().getLong(EXTRA_FEED_ID);
        getChildFragmentManager().beginTransaction().replace(R.id.statisticsContainer,
                        FeedStatisticsFragment.newInstance(feedId, true), "feed_statistics_fragment")
                .commitAllowingStateLoss();
    }
}
