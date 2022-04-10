package de.danoeh.antennapod.ui.statistics.subscriptions;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Pair;
import de.danoeh.antennapod.event.StatisticsEvent;
import de.danoeh.antennapod.ui.statistics.R;
import de.danoeh.antennapod.ui.statistics.StatisticsFragment;
import de.danoeh.antennapod.ui.statistics.databinding.StatisticsFilterDialogBinding;
import org.greenrobot.eventbus.EventBus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class StatisticsFilterDialog {
    private final Context context;
    private final SharedPreferences prefs;
    private boolean includeMarkedAsPlayed;
    private long timeFilterFrom;
    private long timeFilterTo;
    private final Pair<String[], Long[]> filterDatesFrom;
    private final Pair<String[], Long[]> filterDatesTo;

    public StatisticsFilterDialog(Context context, long oldestDate) {
        this.context = context;
        prefs = context.getSharedPreferences(StatisticsFragment.PREF_NAME, Context.MODE_PRIVATE);
        includeMarkedAsPlayed = prefs.getBoolean(StatisticsFragment.PREF_INCLUDE_MARKED_PLAYED, false);
        timeFilterFrom = prefs.getLong(StatisticsFragment.PREF_FILTER_FROM, 0);
        timeFilterTo = prefs.getLong(StatisticsFragment.PREF_FILTER_TO, Long.MAX_VALUE);
        filterDatesFrom = makeMonthlyList(oldestDate, false);
        filterDatesTo = makeMonthlyList(oldestDate, true);
    }

    public void show() {
        StatisticsFilterDialogBinding dialogBinding = StatisticsFilterDialogBinding.inflate(
                LayoutInflater.from(context));
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogBinding.getRoot());
        builder.setTitle(R.string.filter);
        dialogBinding.includeMarkedCheckbox.setOnCheckedChangeListener((compoundButton, checked) -> {
            dialogBinding.timeToSpinner.setEnabled(!checked);
            dialogBinding.timeFromSpinner.setEnabled(!checked);
            dialogBinding.pastYearButton.setEnabled(!checked);
            dialogBinding.allTimeButton.setEnabled(!checked);
            dialogBinding.dateSelectionContainer.setAlpha(checked ? 0.5f : 1f);
        });
        dialogBinding.includeMarkedCheckbox.setChecked(includeMarkedAsPlayed);


        ArrayAdapter<String> adapterFrom = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, filterDatesFrom.first);
        adapterFrom.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dialogBinding.timeFromSpinner.setAdapter(adapterFrom);
        for (int i = 0; i < filterDatesFrom.second.length; i++) {
            if (filterDatesFrom.second[i] >= timeFilterFrom) {
                dialogBinding.timeFromSpinner.setSelection(i);
                break;
            }
        }

        ArrayAdapter<String> adapterTo = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, filterDatesTo.first);
        adapterTo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dialogBinding.timeToSpinner.setAdapter(adapterTo);
        for (int i = 0; i < filterDatesTo.second.length; i++) {
            if (filterDatesTo.second[i] >= timeFilterTo) {
                dialogBinding.timeToSpinner.setSelection(i);
                break;
            }
        }

        dialogBinding.allTimeButton.setOnClickListener(v -> {
            dialogBinding.timeFromSpinner.setSelection(0);
            dialogBinding.timeToSpinner.setSelection(filterDatesTo.first.length - 1);
        });
        dialogBinding.pastYearButton.setOnClickListener(v -> {
            dialogBinding.timeFromSpinner.setSelection(Math.max(0, filterDatesFrom.first.length - 13));
            dialogBinding.timeToSpinner.setSelection(filterDatesTo.first.length - 2);
        });

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            includeMarkedAsPlayed = dialogBinding.includeMarkedCheckbox.isChecked();
            if (includeMarkedAsPlayed) {
                // We do not know the date at which something was marked as played, so filtering does not make sense
                timeFilterFrom = 0;
                timeFilterTo = Long.MAX_VALUE;
            } else {
                timeFilterFrom = filterDatesFrom.second[dialogBinding.timeFromSpinner.getSelectedItemPosition()];
                timeFilterTo = filterDatesTo.second[dialogBinding.timeToSpinner.getSelectedItemPosition()];
            }
            prefs.edit()
                    .putBoolean(StatisticsFragment.PREF_INCLUDE_MARKED_PLAYED, includeMarkedAsPlayed)
                    .putLong(StatisticsFragment.PREF_FILTER_FROM, timeFilterFrom)
                    .putLong(StatisticsFragment.PREF_FILTER_TO, timeFilterTo)
                    .apply();
            EventBus.getDefault().post(new StatisticsEvent());
        });
        builder.show();
    }

    private Pair<String[], Long[]> makeMonthlyList(long oldestDate, boolean inclusive) {
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(oldestDate);
        date.set(Calendar.DAY_OF_MONTH, 1);
        ArrayList<String> names = new ArrayList<>();
        ArrayList<Long> timestamps = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
        while (date.getTimeInMillis() < System.currentTimeMillis()) {
            names.add(dateFormat.format(new Date(date.getTimeInMillis())));
            if (!inclusive) {
                timestamps.add(date.getTimeInMillis());
            }
            if (date.get(Calendar.MONTH) == Calendar.DECEMBER) {
                date.set(Calendar.MONTH, Calendar.JANUARY);
                date.set(Calendar.YEAR, date.get(Calendar.YEAR) + 1);
            } else {
                date.set(Calendar.MONTH, date.get(Calendar.MONTH) + 1);
            }
            if (inclusive) {
                timestamps.add(date.getTimeInMillis());
            }
        }
        if (inclusive) {
            names.add(context.getString(R.string.statistics_today));
            timestamps.add(Long.MAX_VALUE);
        }
        return new Pair<>(names.toArray(new String[0]), timestamps.toArray(new Long[0]));
    }
}
