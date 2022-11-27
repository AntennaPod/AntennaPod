package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.dialog.ConfirmationDialog;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.event.playback.PlaybackHistoryEvent;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class PlaybackHistoryFragment extends EpisodesListFragment {
    public static final String TAG = "PlaybackHistoryFragment";
    private static final String PREF_NAME = "PrefPlaybackHistory";
    private static final String PREF_FILTERED = "isFiltered";
    private static final String PREF_START_DATE = "startDate";
    private static final String PREF_END_DATE = "endDate";
    private boolean isFiltered = false;

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        toolbar.inflateMenu(R.menu.playback_history);
        toolbar.setTitle(R.string.playback_history_label);
        updateToolbar();
        emptyView.setIcon(R.drawable.ic_history);
        emptyView.setTitle(R.string.no_history_head_label);
        txtvInformation.setOnClickListener(v -> openDateRangePicker());
        SharedPreferences prefs = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.isFiltered = prefs.getBoolean(PREF_FILTERED, false);
        updateFilterUi();
        swipeActions.detach();
        return root;
    }

    @Override
    protected FeedItemFilter getFilter() {
        return FeedItemFilter.unfiltered();
    }

    @Override
    protected String getFragmentTag() {
        return TAG;
    }

    @Override
    protected String getPrefName() {
        return TAG;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (super.onOptionsItemSelected(item)) {
            return true;
        }
        if (item.getItemId() == R.id.clear_history_item) {

            ConfirmationDialog conDialog = new ConfirmationDialog(
                    getActivity(),
                    R.string.clear_history_label,
                    R.string.clear_playback_history_msg) {

                @Override
                public void onConfirmButtonPressed(DialogInterface dialog) {
                    dialog.dismiss();
                    DBWriter.clearPlaybackHistory();
                }
            };
            conDialog.createNewDialog().show();

            return true;
        } else if (item.getItemId() == R.id.playback_remove_filter) {
            setPrefFiltered(false);
            updateFilterUi();
            loadItems();
            return true;
        } else if (item.getItemId() == R.id.playback_filter_by_date_range) {
            openDateRangePicker();
            return true;
        }
        return false;
    }

    private void openDateRangePicker() {
        CalendarConstraints calendarConstraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now()).build();

        MaterialDatePicker.Builder<Pair<Long, Long>> materialBuilder = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText(R.string.choose_date_filter)
                .setCalendarConstraints(calendarConstraints);
        setDefaultSelection(materialBuilder);

        MaterialDatePicker<Pair<Long, Long>> dateRangePicker = materialBuilder.build();
        dateRangePicker.addOnPositiveButtonClickListener((value) -> {
            setPrefFiltered(true);
            // Converting the timestamps from UTC to the local time.
            Calendar calendar = Calendar.getInstance(Locale.getDefault());
            long start = value.first - calendar.getTimeZone().getOffset(value.first);
            long end = value.second - calendar.getTimeZone().getOffset(value.second);
            // Adding a day to the end date so that podcasts listened to on the same day are also displayed
            onFilterChanged(start, addDayToTimestamp(end, 1));
            loadItems();
        });

        dateRangePicker.show(this.getParentFragmentManager(), TAG);
    }

    private void setDefaultSelection(MaterialDatePicker.Builder<Pair<Long, Long>> builder) {
        if (!isFiltered) {
            return;
        }

        Pair<Long, Long> timeframe = getTimeframe();

        // Converting the timestamps from local time to UTC.
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        long start = timeframe.first + calendar.getTimeZone().getOffset(timeframe.first);;
        long end = timeframe.second + calendar.getTimeZone().getOffset(timeframe.second);;
        builder.setSelection(new Pair<>(start, addDayToTimestamp(end, -1)));
    }

    private void onFilterChanged(long start, long end) {
        SharedPreferences prefs = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(PREF_START_DATE, start).apply();
        prefs.edit().putLong(PREF_END_DATE, end).apply();
        updateFilterUi();
    }

    private void setPrefFiltered(boolean isFiltered) {
        SharedPreferences prefs = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_FILTERED, isFiltered).apply();
        this.isFiltered = isFiltered;
    }

    @Override
    protected void updateToolbar() {
        // Not calling super, as we do not have a refresh button that could be updated
        toolbar.getMenu().findItem(R.id.clear_history_item).setVisible(!episodes.isEmpty());
        toolbar.getMenu().findItem(R.id.playback_remove_filter).setVisible(isFiltered);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onHistoryUpdated(PlaybackHistoryEvent event) {
        loadItems();
        updateToolbar();
    }

    @NonNull
    @Override
    protected List<FeedItem> loadData() {
        return DBReader.getPlaybackHistory(0, page * EPISODES_PER_PAGE, getTimeframe());
    }

    @NonNull
    @Override
    protected List<FeedItem> loadMoreData(int page) {
        return DBReader.getPlaybackHistory((page - 1) * EPISODES_PER_PAGE, EPISODES_PER_PAGE, getTimeframe());
    }

    @Override
    protected int loadTotalItemCount() {
        return (int) DBReader.getPlaybackHistoryLength(getTimeframe());
    }

    private long addDayToTimestamp(long timestamp, int day) {
        // Calculation needed to account for daylight savings time
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.add(Calendar.DAY_OF_MONTH, day);
        return calendar.getTime().getTime();
    }

    private void updateFilterUi() {
        if (isFiltered) {
            txtvInformation.setVisibility(View.VISIBLE);
            emptyView.setMessage(R.string.no_all_history_filtered_label);
        } else {
            txtvInformation.setVisibility(View.GONE);
            emptyView.setMessage(R.string.no_history_label);
        }
    }

    @NonNull
    private Pair<Long, Long> getTimeframe() {
        SharedPreferences prefs = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(PREF_FILTERED, false)) {
            // Adding a day to the end date so that podcasts listened to on the same day are also displayed
            return new Pair<>(0L, Long.MAX_VALUE);
        }

        long start = prefs.getLong(PREF_START_DATE, 0);
        long end = prefs.getLong(PREF_END_DATE, System.currentTimeMillis());

        return new Pair<>(start, end);
    }
}
