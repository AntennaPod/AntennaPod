package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.content.SharedPreferences;
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
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.event.playback.PlaybackHistoryEvent;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Calendar;
import java.util.List;

public class PlaybackHistoryFragment extends EpisodesListFragment {
    public static final String TAG = "PlaybackHistoryFragment";
    private static final String PREF_NAME = "PrefPlaybackHistory";
    private static final String PREF_START_DATE = "startDate";
    private static final String PREF_END_DATE = "endDate";

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        toolbar.inflateMenu(R.menu.playback_history);
        toolbar.setTitle(R.string.playback_history_label);
        updateToolbar();
        emptyView.setIcon(R.drawable.ic_history);
        emptyView.setTitle(R.string.no_history_head_label);
        emptyView.setMessage(R.string.no_history_label);
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
            DBWriter.clearPlaybackHistory();
            return true;
        } else if (item.getItemId() == R.id.playback_remove_filter) {
            // Adding a day to the end date so that podcasts listened to on the same day are also displayed
            onFilterChanged(0, addDayToTimestamp(MaterialDatePicker.todayInUtcMilliseconds(), 1));
            loadItems();
            return true;
        } else if (item.getItemId() == R.id.playback_filter_by_date_range) {
            openDateRangePicker();
            return true;
        }
        return false;
    }

    private void onFilterChanged(long start, long end) {
        SharedPreferences prefs = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(PREF_START_DATE, start).apply();
        prefs.edit().putLong(PREF_END_DATE, end).apply();
    }

    private void openDateRangePicker() {
        CalendarConstraints calendarConstraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now()).build();

        MaterialDatePicker.Builder<Pair<Long, Long>> materialBuilder = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText(R.string.date_range_picker_title)
                .setCalendarConstraints(calendarConstraints);
        setDefaultSelection(materialBuilder);

        MaterialDatePicker<Pair<Long, Long>> dateRangePicker = materialBuilder.build();
        dateRangePicker.addOnPositiveButtonClickListener((value) -> {
            // Add one day to the end timestamp so that podcast listened on the same day shows up as well
            onFilterChanged(value.first, addDayToTimestamp(value.second, 1));
            loadItems();
        });

        dateRangePicker.show(this.getParentFragmentManager(), TAG);
    }

    private void setDefaultSelection(MaterialDatePicker.Builder<Pair<Long, Long>> builder){
        long[] timeframe = getTimeframe();
        long start = timeframe[0];
        long end = timeframe[1];

        if(start != 0){
            builder.setSelection(new Pair<>(start, addDayToTimestamp(end, -1)));
        }
    }

    @Override
    protected void updateToolbar() {
        // Not calling super, as we do not have a refresh button that could be updated
        toolbar.getMenu().findItem(R.id.clear_history_item).setVisible(!episodes.isEmpty());
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
        return (int) DBReader.getPlaybackHistoryLength();
    }

    private long addDayToTimestamp(long timestamp, int day){
        // Calculation needed to account for daylight savings time
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.add(Calendar.DAY_OF_MONTH, day);
        return calendar.getTime().getTime();
    }

    @NonNull
    private long[] getTimeframe() {
        SharedPreferences prefs = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long start = prefs.getLong(PREF_START_DATE, 0);
        long end = prefs.getLong(PREF_END_DATE, System.currentTimeMillis());

        return new long[]{start,end};
    }
}
