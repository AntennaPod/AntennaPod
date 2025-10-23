package de.danoeh.antennapod.ui.screen.playback;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.event.playback.SleepTimerUpdatedEvent;
import de.danoeh.antennapod.playback.base.PlayerStatus;
import de.danoeh.antennapod.playback.service.PlaybackController;
import de.danoeh.antennapod.playback.service.PlaybackService;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import de.danoeh.antennapod.storage.preferences.SleepTimerPreferences;
import de.danoeh.antennapod.storage.preferences.SleepTimerType;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.common.Converter;
import de.danoeh.antennapod.ui.common.ThemeUtils;

public class SleepTimerDialog extends DialogFragment {
    private PlaybackController controller;
    private EditText etxtTime;
    private TextView sleepTimerHintText;
    private LinearLayout timeSetup;
    private LinearLayout timeDisplay;
    private TextView time;
    private Spinner sleepTimerType;
    private boolean sleepTimerTypeInitialized = false;
    private CheckBox chAutoEnable;
    private ImageView changeTimesButton;
    private CheckBox cbVibrate;
    private CheckBox cbShakeToReset;
    private Button setTimerButton;

    Button extendSleepFiveMinutesButton;
    Button extendSleepTenMinutesButton;
    Button extendSleepTwentyMinutesButton;

    private static final int EXTEND_FEW_MINUTES_DISPLAY_VALUE = 5;
    private static final int EXTEND_FEW_MINUTES = 5 * 1000 * 60;
    private static final int EXTEND_MID_MINUTES_DISPLAY_VALUE = 10;
    private static final int EXTEND_MID_MINUTES = 10 * 1000 * 60;
    private static final int EXTEND_LOTS_MINUTES_DISPLAY_VALUE = 30;
    private static final int EXTEND_LOTS_MINUTES = 30 * 1000 * 60;
    private static final int EXTEND_FEW_EPISODES = 1;
    private static final int EXTEND_MID_EPISODES = 2;
    private static final int EXTEND_LOTS_EPISODES = 3;

    static class SleepEntryConfig {
        public final int displayValue;
        public final int configuredValue;

        public SleepEntryConfig(int displayValue, int configuredValue) {
            this.displayValue = displayValue;
            this.configuredValue = configuredValue;
        }
    }

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
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(R.string.sleep_timer_label);
        builder.setView(content);
        builder.setPositiveButton(R.string.close_label, null);

        List<String> spinnerContent = new ArrayList<>();
        // add "title" for all options
        spinnerContent.add(getString(R.string.time_minutes));
        spinnerContent.add(getString(R.string.episodes_label).toLowerCase(Locale.getDefault()));

        sleepTimerType = content.findViewById(R.id.sleepTimerType);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item, spinnerContent);

        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sleepTimerType.setAdapter(spinnerAdapter);
        sleepTimerType.setSelection(SleepTimerPreferences.getSleepTimerType().index);

        sleepTimerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SleepTimerPreferences.setSleepTimerType(SleepTimerType.fromIndex(position));
                // this callback is called even when the spinner is first initialized
                // we need to differentiate these calls
                if (sleepTimerTypeInitialized) {
                    chAutoEnable.setChecked(false);
                } else {
                    sleepTimerTypeInitialized = true;
                }
                refreshUiState();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        etxtTime = content.findViewById(R.id.etxtTime);
        sleepTimerHintText = content.findViewById(R.id.sleepTimerHintText);
        timeSetup = content.findViewById(R.id.timeSetup);
        timeDisplay = content.findViewById(R.id.timeDisplay);
        timeDisplay.setVisibility(View.GONE);
        time = content.findViewById(R.id.time);

        cbShakeToReset = content.findViewById(R.id.cbShakeToReset);
        cbVibrate = content.findViewById(R.id.cbVibrate);
        chAutoEnable = content.findViewById(R.id.chAutoEnable);
        changeTimesButton = content.findViewById(R.id.changeTimesButton);
        setTimerButton = content.findViewById(R.id.setSleeptimerButton);

        extendSleepFiveMinutesButton = content.findViewById(R.id.extendSleepFiveMinutesButton);
        extendSleepTenMinutesButton = content.findViewById(R.id.extendSleepTenMinutesButton);
        extendSleepTwentyMinutesButton = content.findViewById(R.id.extendSleepTwentyMinutesButton);

        refreshUiState();

        etxtTime.setText(SleepTimerPreferences.lastTimerValue());
        etxtTime.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                refreshUiState();
            }
        });

        etxtTime.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(etxtTime, InputMethodManager.SHOW_IMPLICIT);
        }, 100);

        chAutoEnable.setChecked(SleepTimerPreferences.autoEnable());

        cbShakeToReset.setChecked(SleepTimerPreferences.shakeToReset());
        cbVibrate.setChecked(SleepTimerPreferences.vibrate());
        refreshAutoEnableControls(SleepTimerPreferences.autoEnable());

        cbShakeToReset.setOnCheckedChangeListener((buttonView, isChecked)
                -> SleepTimerPreferences.setShakeToReset(isChecked));
        cbVibrate.setOnCheckedChangeListener((buttonView, isChecked)
                -> SleepTimerPreferences.setVibrate(isChecked));
        chAutoEnable.setOnCheckedChangeListener((compoundButton, isChecked)
                -> {
            boolean always = SleepTimerPreferences.autoEnableFrom() == SleepTimerPreferences.autoEnableTo();
            if (isChecked && always) {
                confirmAlwaysSleepTimerDialog();
            }
            refreshAutoEnableControls(isChecked);
        });
        updateAutoEnableText();

        changeTimesButton.setOnClickListener(changeTimesBtn -> {
            int from = SleepTimerPreferences.autoEnableFrom();
            int to = SleepTimerPreferences.autoEnableTo();
            showTimeRangeDialog(getContext(), from, to);
        });

        Button disableButton = content.findViewById(R.id.disableSleeptimerButton);
        disableButton.setOnClickListener(v -> {
            if (controller != null) {
                controller.disableSleepTimer();
            }
        });

        setTimerButton.setOnClickListener(v -> {
            if (!PlaybackService.isRunning
                    || (controller != null && controller.getStatus() != PlayerStatus.PLAYING)) {
                Snackbar.make(content, R.string.no_media_playing_label, Snackbar.LENGTH_LONG).show();
                return;
            }
            try {
                SleepTimerPreferences.setLastTimer(etxtTime.getText().toString());
                if (controller != null) {
                    controller.setSleepTimer(SleepTimerPreferences.timerMillisOrEpisodes());
                }
                closeKeyboard(content);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                Snackbar.make(content, R.string.time_dialog_invalid_input, Snackbar.LENGTH_LONG).show();
            }
        });
        return builder.create();
    }

    private void confirmAlwaysSleepTimerDialog() {
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.sleep_timer_without_continuous_playback_title)
                .setMessage(R.string.sleep_timer_without_continuous_playback_message)
                .setNegativeButton(R.string.sleep_timer_without_continuous_playback_disable_playback,
                        (dialogInterface, i) -> {
                            // disable continuous playback and also disable the auto sleep timer
                            UserPreferences.setFollowQueue(false);
                            chAutoEnable.setChecked(false);
                            refreshUiState();
                        })
                .create();

        dialog.setOnCancelListener(dialogInterface -> chAutoEnable.setChecked(false));

        dialog.show();
        // mark the disable continuous playback option in red
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(ThemeUtils.getColorFromAttr(requireContext(), R.attr.colorError));
    }

    private long getSelectedSleepTime() throws NumberFormatException {
        long time = Long.parseLong(etxtTime.getText().toString());
        if (time == 0) {
            throw new NumberFormatException("Timer must not be zero");
        }
        return time;
    }

    private long getSelectedSleepTimeWithDefault(long defaultValue) {
        try {
            return getSelectedSleepTime();
        } catch (NumberFormatException nex) {
            return defaultValue;
        }
    }

    private void refreshAutoEnableControls(boolean enabled) {
        SleepTimerPreferences.setAutoEnable(enabled);
        changeTimesButton.setAlpha(enabled ? 1.0f : 0.5f);
    }

    private void refreshUiState() {
        // if we're using episode timer and continuous playback is disabled, don't
        // let the user use anything other than 1 episode
        boolean noEpisodeSelection = SleepTimerPreferences.getSleepTimerType() == SleepTimerType.EPISODES
                && !UserPreferences.isFollowQueue();

        if (noEpisodeSelection) {
            etxtTime.setEnabled(false);
            sleepTimerHintText.setText(R.string.multiple_sleep_episodes_while_continuous_playback_disabled);
            sleepTimerHintText.setVisibility(View.VISIBLE);
            chAutoEnable.setVisibility(View.GONE);
            changeTimesButton.setVisibility(View.GONE);
            cbShakeToReset.setVisibility(View.GONE);
            cbVibrate.setVisibility(View.GONE);
            setTimerButton.setEnabled(false);
        } else {
            chAutoEnable.setVisibility(View.VISIBLE);
            changeTimesButton.setVisibility(View.VISIBLE);
            cbShakeToReset.setVisibility(View.VISIBLE);
            cbVibrate.setVisibility(View.VISIBLE);
            setTimerButton.setEnabled(true);
            etxtTime.setEnabled(true);
            if (SleepTimerPreferences.getSleepTimerType() == SleepTimerType.EPISODES) {
                // for episode timers check if the queue length exceeds the number of sleep episodes we have
                final int queueSize = DBReader.getRemainingQueueSize(
                        PlaybackPreferences.getCurrentlyPlayingFeedMediaId());
                final long episodes = getSelectedSleepTimeWithDefault(0);
                if (episodes > queueSize) {
                    sleepTimerHintText.setText(getResources().getQuantityString(
                            R.plurals.episodes_sleep_timer_exceeds_queue,
                            queueSize,
                            queueSize));
                    sleepTimerHintText.setVisibility(View.VISIBLE);
                } else {
                    sleepTimerHintText.setVisibility(View.GONE); // could maybe show duration in minutes
                }
            } else {
                // for time sleep timers check if the selected value exceeds the remaining play time in the episode
                final int remaining = controller != null ? controller.getDuration() - controller.getPosition() :
                        Integer.MAX_VALUE;
                final long timer = TimeUnit.MINUTES.toMillis(getSelectedSleepTimeWithDefault(0));
                if ((timer > remaining) && !UserPreferences.isFollowQueue()) {
                    final int remainingMinutes = Math.toIntExact(TimeUnit.MILLISECONDS.toMinutes(remaining));
                    sleepTimerHintText
                            .setText(getResources().getQuantityString(
                                    R.plurals.timer_exceeds_remaining_time_while_continuous_playback_disabled,
                                    remainingMinutes,
                                    remainingMinutes
                            ));
                    sleepTimerHintText.setVisibility(View.VISIBLE);
                } else {
                    // don't show it at all
                    sleepTimerHintText.setVisibility(View.GONE);
                }
            }
        }

        // disable extension for episodes if we're not moving to next one
        extendSleepFiveMinutesButton.setEnabled(!noEpisodeSelection);
        extendSleepTenMinutesButton.setEnabled(!noEpisodeSelection);
        extendSleepTwentyMinutesButton.setEnabled(!noEpisodeSelection);

        if (SleepTimerType.fromIndex(sleepTimerType.getSelectedItemPosition()) == SleepTimerType.CLOCK) {
            extendSleepFiveMinutesButton.setText(
                    getString(R.string.extend_sleep_timer_label, EXTEND_FEW_MINUTES_DISPLAY_VALUE));
            extendSleepFiveMinutesButton.setOnClickListener(v -> {
                if (controller != null) {
                    controller.extendSleepTimer(EXTEND_FEW_MINUTES);
                }
            });

            extendSleepTenMinutesButton.setText(
                    getString(R.string.extend_sleep_timer_label, EXTEND_MID_MINUTES_DISPLAY_VALUE));
            extendSleepTenMinutesButton.setOnClickListener(v -> {
                if (controller != null) {
                    controller.extendSleepTimer(EXTEND_MID_MINUTES);
                }
            });

            extendSleepTwentyMinutesButton.setText(
                    getString(R.string.extend_sleep_timer_label, EXTEND_LOTS_MINUTES_DISPLAY_VALUE));
            extendSleepTwentyMinutesButton.setOnClickListener(v -> {
                if (controller != null) {
                    controller.extendSleepTimer(EXTEND_LOTS_MINUTES);
                }
            });
        } else {
            extendSleepFiveMinutesButton.setText(
                    "+" + getResources().getQuantityString(
                            R.plurals.extend_sleep_timer_episodes_quantified,
                            EXTEND_FEW_EPISODES));
            extendSleepFiveMinutesButton.setOnClickListener(v -> {
                if (controller != null) {
                    controller.extendSleepTimer(EXTEND_FEW_EPISODES);
                }
            });

            extendSleepTenMinutesButton.setText(
                    "+" + getResources().getQuantityString(
                            R.plurals.extend_sleep_timer_episodes_quantified,
                            EXTEND_MID_EPISODES));
            extendSleepTenMinutesButton.setOnClickListener(v -> {
                if (controller != null) {
                    controller.extendSleepTimer(EXTEND_MID_EPISODES);
                }
            });

            extendSleepTwentyMinutesButton.setText(
                    "+" + getResources().getQuantityString(
                            R.plurals.extend_sleep_timer_episodes_quantified,
                            EXTEND_LOTS_EPISODES));
            extendSleepTwentyMinutesButton.setOnClickListener(v -> {
                if (controller != null) {
                    controller.extendSleepTimer(EXTEND_LOTS_EPISODES);
                }
            });
        }
    }

    private void showTimeRangeDialog(Context context, int from, int to) {
        TimeRangeDialog dialog = new TimeRangeDialog(context, from, to);
        dialog.setOnDismissListener(v -> {
            SleepTimerPreferences.setAutoEnableFrom(dialog.getFrom());
            SleepTimerPreferences.setAutoEnableTo(dialog.getTo());
            boolean alwaysSelected = dialog.getFrom() == dialog.getTo();
            // disable the checkbox if they've selected always
            // only change the state if true, don't change it regardless of flag (although we could)
            if (alwaysSelected) {
                confirmAlwaysSleepTimerDialog();
            } else if (!chAutoEnable.isChecked()) { // if it's not checked, then make sure it's checked in UI too
                chAutoEnable.setChecked(true);
            }
            updateAutoEnableText();
        });
        dialog.show();
    }

    private void updateAutoEnableText() {
        String text;
        int from = SleepTimerPreferences.autoEnableFrom();
        int to = SleepTimerPreferences.autoEnableTo();

        if (from == to) {
            text = getString(R.string.auto_enable_label);
        } else if (DateFormat.is24HourFormat(getContext())) {
            String formattedFrom = String.format(Locale.getDefault(), "%02d:00", from);
            String formattedTo = String.format(Locale.getDefault(), "%02d:00", to);
            text = getString(R.string.auto_enable_label_with_times, formattedFrom, formattedTo);
        } else {
            String formattedFrom = String.format(Locale.getDefault(), "%02d:00 %s",
                    from % 12, from >= 12 ? "PM" : "AM");
            String formattedTo = String.format(Locale.getDefault(), "%02d:00 %s",
                    to % 12, to >= 12 ? "PM" : "AM");
            text = getString(R.string.auto_enable_label_with_times, formattedFrom, formattedTo);

        }
        chAutoEnable.setText(text);
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    @SuppressWarnings("unused")
    public void timerUpdated(SleepTimerUpdatedEvent event) {
        timeDisplay.setVisibility(event.isOver() || event.isCancelled() ? View.GONE : View.VISIBLE);
        timeSetup.setVisibility(event.isOver() || event.isCancelled() ? View.VISIBLE : View.GONE);
        sleepTimerType.setEnabled(event.isOver() || event.isCancelled());

        switch (SleepTimerPreferences.getSleepTimerType()) {
            case EPISODES -> time.setText(
                    getResources().getQuantityString(
                            R.plurals.extend_sleep_timer_episodes_quantified,
                            (int) event.getDisplayTimeLeft(),
                            (int) event.getDisplayTimeLeft()));
            default -> time.setText(Converter.getDurationStringLong((int) event.getDisplayTimeLeft()));
        }
    }

    private void closeKeyboard(View content) {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(content.getWindowToken(), 0);
    }
}
