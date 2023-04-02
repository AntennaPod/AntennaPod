package de.danoeh.antennapod.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.format.DateFormat;
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
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.event.playback.SleepTimerUpdatedEvent;
import de.danoeh.antennapod.core.preferences.SleepTimerPreferences;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.playback.PlaybackController;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SleepTimerDialog extends DialogFragment {
    private PlaybackController controller;
    private EditText etxtTime;
    private LinearLayout timeSetup;
    private LinearLayout timeDisplay;
    private TextView time;
    private CheckBox chAutoEnable;

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
        chAutoEnable = content.findViewById(R.id.chAutoEnable);
        final Button changeTimesButton = content.findViewById(R.id.changeTimes);

        cbShakeToReset.setChecked(SleepTimerPreferences.shakeToReset());
        cbVibrate.setChecked(SleepTimerPreferences.vibrate());
        chAutoEnable.setChecked(SleepTimerPreferences.autoEnable());
        changeTimesButton.setEnabled(chAutoEnable.isChecked());

        cbShakeToReset.setOnCheckedChangeListener((buttonView, isChecked)
                -> SleepTimerPreferences.setShakeToReset(isChecked));
        cbVibrate.setOnCheckedChangeListener((buttonView, isChecked)
                -> SleepTimerPreferences.setVibrate(isChecked));
        chAutoEnable.setOnCheckedChangeListener((compoundButton, isChecked)
                -> {
            SleepTimerPreferences.setAutoEnable(isChecked);
            changeTimesButton.setEnabled(isChecked);
        });
        updateAutoEnableText();


        final int timeFormat = DateFormat.is24HourFormat(getContext()) ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H;
        changeTimesButton.setOnClickListener(changeTimesBtn -> {
            Pair<Integer, Integer> from = SleepTimerPreferences.autoEnableTimeFrom();
            MaterialTimePicker dialogFrom = new MaterialTimePicker.Builder()
                    .setHour(from.first)
                    .setMinute(from.second)
                    .setTimeFormat(timeFormat)
                    .setTitleText(R.string.auto_enable_label_from)
                    .setPositiveButtonText(R.string.auto_enable_label_next)
                    .build();
            dialogFrom.addOnPositiveButtonClickListener(dialog -> {
                Pair<Integer, Integer> to = SleepTimerPreferences.autoEnableTimeTo();
                MaterialTimePicker dialogTo = new MaterialTimePicker.Builder()
                        .setHour(to.first)
                        .setMinute(to.second)
                        .setTimeFormat(timeFormat)
                        .setTitleText(R.string.auto_enable_label_to)
                        .build();
                dialogTo.addOnPositiveButtonClickListener(dialog2 -> {
                    SleepTimerPreferences.setAutoEnableTimeTo(dialogTo.getHour(), dialogTo.getMinute());
                    updateAutoEnableText();
                });
                dialogTo.show(getParentFragmentManager(), "SleepTimerAutoEnableTo");
            });
            dialogFrom.addOnPositiveButtonClickListener(dialog -> {
                SleepTimerPreferences.setAutoEnableTimeFrom(dialogFrom.getHour(), dialogFrom.getMinute());
                updateAutoEnableText();
            });
            dialogFrom.show(getParentFragmentManager(), "SleepTimerAutoEnableFrom");
        });

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

    private void updateAutoEnableText() {
        String text;
        Pair<Integer, Integer> fromSetting = SleepTimerPreferences.autoEnableTimeFrom();
        Pair<Integer, Integer> toSetting = SleepTimerPreferences.autoEnableTimeTo();
        if (fromSetting.equals(toSetting)) {
            text = getString(R.string.auto_enable_label);
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            try {
                Date from = sdf.parse(fromSetting.first + ":" + fromSetting.second);
                Date to = sdf.parse(toSetting.first + ":" + toSetting.second);
                text = getString(
                        R.string.auto_enable_label_with_times,
                        SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(from),
                        SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(to)
                );
            } catch (Exception e) {
                return;
            }
        }
        chAutoEnable.setText(text);
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
