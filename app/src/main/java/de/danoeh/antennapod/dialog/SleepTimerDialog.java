package de.danoeh.antennapod.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.snackbar.Snackbar;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.event.playback.SleepTimerUpdatedEvent;
import de.danoeh.antennapod.core.preferences.SleepTimerPreferences;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.playback.PlaybackController;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class SleepTimerDialog extends DialogFragment {
    private PlaybackController controller;
    private EditText etxtTime;
    private LinearLayout timeSetup;
    private LinearLayout timeDisplay;
    private TextView time;

    public SleepTimerDialog() {

    }

    @Override
    public void onStart() {
        super.onStart();
        controller = new PlaybackController(getActivity()) {
            @Override
            public void loadMediaInfo() {
            }
        };
        controller.init();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (controller != null) {
            controller.release();
        }
        EventBus.getDefault().unregister(this);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View content = View.inflate(getContext(), R.layout.time_dialog, null);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
        builder.setTitle(R.string.sleep_timer_label);
        builder.setView(content);
        builder.setPositiveButton(R.string.close_label, null);

        etxtTime = content.findViewById(R.id.etxtTime);
        timeSetup = content.findViewById(R.id.timeSetup);
        timeDisplay = content.findViewById(R.id.timeDisplay);
        timeDisplay.setVisibility(View.GONE);
        time = content.findViewById(R.id.time);
        Button extendSleepFiveMinutesButton = content.findViewById(R.id.extendSleepFiveMinutesButton);
        extendSleepFiveMinutesButton.setText(getString(R.string.extend_sleep_timer_label, 5));
        Button extendSleepTenMinutesButton = content.findViewById(R.id.extendSleepTenMinutesButton);
        extendSleepTenMinutesButton.setText(getString(R.string.extend_sleep_timer_label, 10));
        Button extendSleepTwentyMinutesButton = content.findViewById(R.id.extendSleepTwentyMinutesButton);
        extendSleepTwentyMinutesButton.setText(getString(R.string.extend_sleep_timer_label, 20));
        extendSleepFiveMinutesButton.setOnClickListener(v -> {
            if (controller != null) {
                controller.extendSleepTimer(5 * 1000 * 60);
            }
        });
        extendSleepTenMinutesButton.setOnClickListener(v -> {
            if (controller != null) {
                controller.extendSleepTimer(10 * 1000 * 60);
            }
        });
        extendSleepTwentyMinutesButton.setOnClickListener(v -> {
            if (controller != null) {
                controller.extendSleepTimer(20 * 1000 * 60);
            }
        });

        etxtTime.setText(SleepTimerPreferences.lastTimerValue());
        etxtTime.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(etxtTime, InputMethodManager.SHOW_IMPLICIT);
        }, 100);

        final CheckBox cbShakeToReset = content.findViewById(R.id.cbShakeToReset);
        final CheckBox cbVibrate = content.findViewById(R.id.cbVibrate);
        final CheckBox chAutoEnable = content.findViewById(R.id.chAutoEnable);
        final CheckBox chAutoEnableTimeBased = content.findViewById(R.id.chAutoEnableTimeBased);

        cbShakeToReset.setChecked(SleepTimerPreferences.shakeToReset());
        cbVibrate.setChecked(SleepTimerPreferences.vibrate());
        chAutoEnable.setChecked(SleepTimerPreferences.autoEnable());
        chAutoEnableTimeBased.setChecked(SleepTimerPreferences.autoEnableTimeBased());

        cbShakeToReset.setOnCheckedChangeListener((buttonView, isChecked)
                -> SleepTimerPreferences.setShakeToReset(isChecked));
        cbVibrate.setOnCheckedChangeListener((buttonView, isChecked)
                -> SleepTimerPreferences.setVibrate(isChecked));
        chAutoEnable.setOnCheckedChangeListener((compoundButton, isChecked)
                -> SleepTimerPreferences.setAutoEnable(isChecked));
        chAutoEnableTimeBased.setOnCheckedChangeListener((compoundButton, isChecked)
                -> SleepTimerPreferences.setAutoEnableTimeBased(isChecked));

        Button autoEnableTimeFrom = content.findViewById(R.id.autoEnableTimeFrom);
        autoEnableTimeFrom.setOnClickListener(v -> {
            Pair<Integer, Integer> currentSetting = SleepTimerPreferences.autoEnableTimeFrom();
            new TimePickerDialog(
                    requireContext(),
                    (view, hourOfDay, minute) -> {
                        SleepTimerPreferences.setAutoEnableTimeFrom(hourOfDay, minute);
                        autoEnableTimeFrom.setText(hourOfDay + ":" + minute);
                    },
                    currentSetting.first,
                    currentSetting.second,
                    true
            ).show();
        });

        Pair<Integer, Integer> fromTime = SleepTimerPreferences.autoEnableTimeFrom();
        autoEnableTimeFrom.setText(fromTime.first + ":" + fromTime.second);

        Button autoEnableTimeTo = content.findViewById(R.id.autoEnableTimeTo);
        autoEnableTimeTo.setOnClickListener(v -> {
            Pair<Integer, Integer> currentSetting = SleepTimerPreferences.autoEnableTimeTo();
            new TimePickerDialog(
                    requireContext(),
                    (view, hourOfDay, minute) -> {
                        SleepTimerPreferences.setAutoEnableTimeTo(hourOfDay, minute);
                        autoEnableTimeTo.setText(hourOfDay + ":" + minute);
                    },
                    currentSetting.first,
                    currentSetting.second,
                    true
            ).show();
        });

        Pair<Integer, Integer> toTime = SleepTimerPreferences.autoEnableTimeTo();
        autoEnableTimeTo.setText(toTime.first + ":" + toTime.second);

        Button disableButton = content.findViewById(R.id.disableSleeptimerButton);
        disableButton.setOnClickListener(v -> {
            if (controller != null) {
                controller.disableSleepTimer();
            }
        });
        Button setButton = content.findViewById(R.id.setSleeptimerButton);
        setButton.setOnClickListener(v -> {
            if (!PlaybackService.isRunning) {
                Snackbar.make(content, R.string.no_media_playing_label, Snackbar.LENGTH_LONG).show();
                return;
            }
            try {
                long time = Long.parseLong(etxtTime.getText().toString());
                if (time == 0) {
                    throw new NumberFormatException("Timer must not be zero");
                }
                SleepTimerPreferences.setLastTimer(etxtTime.getText().toString());
                if (controller != null) {
                    controller.setSleepTimer(SleepTimerPreferences.timerMillis());
                }
                closeKeyboard(content);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                Snackbar.make(content, R.string.time_dialog_invalid_input, Snackbar.LENGTH_LONG).show();
            }
        });
        return builder.create();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    public void timerUpdated(SleepTimerUpdatedEvent event) {
        timeDisplay.setVisibility(event.isOver() || event.isCancelled() ? View.GONE : View.VISIBLE);
        timeSetup.setVisibility(event.isOver() || event.isCancelled() ? View.VISIBLE : View.GONE);
        time.setText(Converter.getDurationStringLong((int) event.getTimeLeft()));
    }

    private void closeKeyboard(View content) {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(content.getWindowToken(), 0);
    }
}
