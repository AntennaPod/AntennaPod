package de.danoeh.antennapod.dialog;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import androidx.appcompat.app.AlertDialog;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.download.AutoUpdateManager;
import de.danoeh.antennapod.databinding.FeedRefreshDialogBinding;
import org.apache.commons.lang3.ArrayUtils;

import java.util.concurrent.TimeUnit;

public class FeedRefreshIntervalDialog {
    private static final int[] INTERVAL_VALUES_HOURS = {1, 2, 4, 8, 12, 24, 72};
    private final Context context;
    private FeedRefreshDialogBinding viewBinding;

    public FeedRefreshIntervalDialog(Context context) {
        this.context = context;
    }

    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.feed_refresh_title);
        builder.setMessage(R.string.feed_refresh_sum);
        viewBinding = FeedRefreshDialogBinding.inflate(LayoutInflater.from(context));
        builder.setView(viewBinding.getRoot());

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, buildSpinnerEntries());
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        viewBinding.spinner.setAdapter(spinnerArrayAdapter);
        viewBinding.timePicker.setIs24HourView(DateFormat.is24HourFormat(context));
        viewBinding.spinner.setSelection(ArrayUtils.indexOf(INTERVAL_VALUES_HOURS, 24));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            viewBinding.timePicker.setHour(8);
            viewBinding.timePicker.setMinute(0);
        } else {
            viewBinding.timePicker.setCurrentHour(8);
            viewBinding.timePicker.setCurrentMinute(0);
        }

        long currInterval = UserPreferences.getUpdateInterval();
        int[] updateTime = UserPreferences.getUpdateTimeOfDay();
        if (currInterval > 0) {
            viewBinding.spinner.setSelection(ArrayUtils.indexOf(INTERVAL_VALUES_HOURS,
                    (int) TimeUnit.MILLISECONDS.toHours(currInterval)));
            viewBinding.intervalRadioButton.setChecked(true);
        } else if (updateTime.length == 2 && updateTime[0] >= 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                viewBinding.timePicker.setHour(updateTime[0]);
                viewBinding.timePicker.setMinute(updateTime[1]);
            } else {
                viewBinding.timePicker.setCurrentHour(updateTime[0]);
                viewBinding.timePicker.setCurrentMinute(updateTime[1]);
            }
            viewBinding.timeRadioButton.setChecked(true);
        } else {
            viewBinding.disableRadioButton.setChecked(true);
        }
        updateVisibility();

        viewBinding.radioGroup.setOnCheckedChangeListener((radioGroup, i) -> updateVisibility());

        builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> {
            if (viewBinding.intervalRadioButton.isChecked()) {
                UserPreferences.setUpdateInterval(INTERVAL_VALUES_HOURS[viewBinding.spinner.getSelectedItemPosition()]);
                AutoUpdateManager.restartUpdateAlarm(context);
            } else if (viewBinding.timeRadioButton.isChecked()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    UserPreferences.setUpdateTimeOfDay(viewBinding.timePicker.getHour(),
                            viewBinding.timePicker.getMinute());
                } else {
                    UserPreferences.setUpdateTimeOfDay(viewBinding.timePicker.getCurrentHour(),
                            viewBinding.timePicker.getCurrentMinute());
                }
                AutoUpdateManager.restartUpdateAlarm(context);
            } else if (viewBinding.disableRadioButton.isChecked()) {
                UserPreferences.disableAutoUpdate();
                AutoUpdateManager.disableAutoUpdate(context);
            } else {
                throw new IllegalStateException("Unexpected error.");
            }
        });

        builder.setNegativeButton(R.string.cancel_label, null);
        builder.show();
    }

    private String[] buildSpinnerEntries() {
        final Resources res = context.getResources();
        String[] entries = new String[INTERVAL_VALUES_HOURS.length];
        for (int i = 0; i < INTERVAL_VALUES_HOURS.length; i++) {
            int hours = INTERVAL_VALUES_HOURS[i];
            entries[i] = res.getQuantityString(R.plurals.feed_refresh_every_x_hours, hours, hours);
        }
        return entries;
    }

    private void updateVisibility() {
        viewBinding.spinner.setVisibility(viewBinding.intervalRadioButton.isChecked() ? View.VISIBLE : View.GONE);
        viewBinding.timePicker.setVisibility(viewBinding.timeRadioButton.isChecked() ? View.VISIBLE : View.GONE);
    }
}
