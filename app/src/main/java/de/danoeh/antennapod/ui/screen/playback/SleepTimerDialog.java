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
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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

public class SleepTimerDialog extends DialogFragment {
    private PlaybackController controller;
    private EditText etxtTime;
    private TextView sleepTimerHintText;
    private LinearLayout timeSetup;
    private LinearLayout timeDisplay;
    private TextView time;
    private Spinner sleepTimerType;
    private CheckBox chAutoEnable;
    private ImageView changeTimesButton;

    Button extendSleepFiveMinutesButton;
    Button extendSleepTenMinutesButton;
    Button extendSleepTwentyMinutesButton;

    private final SleepTimeConfig clockSleepTimeConfig = new SleepTimeConfig(0,
            R.string.extend_sleep_timer_label, R.string.time_minutes, false,
            new SleepEntryConfig(5, 5 * 1000 * 60),
            new SleepEntryConfig(10, 10 * 1000 * 60),
            new SleepEntryConfig(30, 30 * 1000 * 60)
    );

    private final SleepTimeConfig episodesSleepTimeConfig = new SleepTimeConfig(1,
            R.plurals.extend_sleep_timer_episodes_quantified, R.string.episodes_label, true,
            new SleepEntryConfig(1, 1),
            new SleepEntryConfig(2, 2),
            new SleepEntryConfig(3, 3)
    );

    static class SleepEntryConfig {
        public final int displayValue;
        public final int configuredValue;

        public SleepEntryConfig(int displayValue, int configuredValue) {
            this.displayValue = displayValue;
            this.configuredValue = configuredValue;
        }
    }

    static class SleepTimeConfig {
        public final int buttonTextResourceId;
        public final int displayTypeTextId;
        public final List<SleepEntryConfig> sleepEntries = new ArrayList<>(3);
        private final int index;
        private final boolean pluralText;

        public SleepTimeConfig(
                int index,
                int buttonTextResourceId,
                int displayTypeTextId,
                boolean pluralText,
                SleepEntryConfig first,
                SleepEntryConfig second, SleepEntryConfig third) {
            this.index = index;
            this.pluralText = pluralText;
            this.buttonTextResourceId = buttonTextResourceId;
            this.displayTypeTextId = displayTypeTextId;
            sleepEntries.add(0, first);
            sleepEntries.add(1, second);
            sleepEntries.add(2, third);
        }
    }

    private SleepTimeConfig getSleepTimeConfig(SleepTimerType timerType) {
        if (Objects.requireNonNull(timerType) == SleepTimerType.EPISODES) {
            return episodesSleepTimeConfig;
        }
        return clockSleepTimeConfig;
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

    private int getSleepTimerIndexFromType(SleepTimerType sleepTimerType) {
        return getSleepTimeConfig(sleepTimerType).index;
    }

    private SleepTimerType getSleepTimerTypeFromIndex(int selection) {
        if (episodesSleepTimeConfig.index == selection) {
            return SleepTimerType.EPISODES;
        }
        return SleepTimerType.CLOCK;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View content = View.inflate(getContext(), R.layout.time_dialog, null);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
        builder.setTitle(R.string.sleep_timer_label);
        builder.setView(content);
        builder.setPositiveButton(R.string.close_label, null);

        List<String> spinnerContent = new ArrayList<>();
        // add "title" for all options
        for (SleepTimeConfig entry : List.of(clockSleepTimeConfig, episodesSleepTimeConfig)) {
            spinnerContent.add(getString(entry.displayTypeTextId).toLowerCase(Locale.getDefault()));
        }

        sleepTimerType = content.findViewById(R.id.sleepTimerType);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item, spinnerContent);

        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sleepTimerType.setAdapter(spinnerAdapter);
        sleepTimerType.setSelection(getSleepTimerIndexFromType(SleepTimerPreferences.getSleepTimerType()));

        sleepTimerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SleepTimerPreferences.setSleepTimerType(getSleepTimerTypeFromIndex(position));
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

        final CheckBox cbShakeToReset = content.findViewById(R.id.cbShakeToReset);
        final CheckBox cbVibrate = content.findViewById(R.id.cbVibrate);
        chAutoEnable = content.findViewById(R.id.chAutoEnable);
        changeTimesButton = content.findViewById(R.id.changeTimesButton);
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
                Snackbar
                        .make(chAutoEnable, getString(R.string.sleep_timer_auto_activate), Snackbar.LENGTH_LONG)
                        .setTextMaxLines(5) // show multiple lines, text doesn't fit otherwise
                        .show();
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
        Button setButton = content.findViewById(R.id.setSleeptimerButton);
        setButton.setOnClickListener(v -> {
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

    private void setButtonText(Button button, int buttonTextResourceId, int value) {
        button.setText(getString(buttonTextResourceId, value));
    }

    private void setButtonTextWithPlurals(Button button, int buttonTextResourceId, int value) {
        button.setText("+" + getResources().getQuantityString(buttonTextResourceId, value, value));
    }

    private void refreshUiState() {
        final SleepTimeConfig selectedConfig = getSleepTimeConfig(SleepTimerPreferences.getSleepTimerType());

        int counter = 0;

        // if we're using episode timer and continuous playback is disabled, don't
        // let the user use anything other than 1 episode
        boolean noEpisodeSelection = SleepTimerPreferences.getSleepTimerType() == SleepTimerType.EPISODES
                && !UserPreferences.isFollowQueue();

        if (noEpisodeSelection) {
            //we're refreshing state on value changes, endless loop otherwise
            if (!etxtTime.getText().toString().equals("1")) {
                etxtTime.setText("1"); // always one episode
            }
            etxtTime.setEnabled(false);
            sleepTimerHintText.setText(R.string.multiple_sleep_episodes_while_continuous_playback_disabled);
            sleepTimerHintText.setVisibility(View.VISIBLE);
        } else {
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
                    final long remainingMinutes = TimeUnit.MILLISECONDS.toMinutes(remaining);
                    sleepTimerHintText
                            .setText(getString(
                                    R.string.timer_exceeds_remaining_time_while_continuous_playback_disabled,
                                    remainingMinutes
                                    ));
                    sleepTimerHintText.setVisibility(View.VISIBLE);
                } else {
                    // don't show it at all
                    sleepTimerHintText.setVisibility(View.GONE);
                }
            }
        }

        for (Button button : List.of(
                extendSleepFiveMinutesButton,
                extendSleepTenMinutesButton,
                extendSleepTwentyMinutesButton)) {
            final SleepEntryConfig entryConfig = Objects.requireNonNull(selectedConfig).sleepEntries.get(counter++);
            // button text resource can be either string or plural string
            if (selectedConfig.pluralText) {
                setButtonTextWithPlurals(button, selectedConfig.buttonTextResourceId, entryConfig.displayValue);
            } else {
                setButtonText(button, selectedConfig.buttonTextResourceId, entryConfig.displayValue);
            }
            button.setEnabled(!noEpisodeSelection); // disable extension for episodes if we're not moving to next one
            button.setOnClickListener(v -> {
                if (controller != null) {
                    controller.extendSleepTimer(entryConfig.configuredValue);
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
            // only change the state if true, don't change it regardless of flag (alghough we could)
            if (alwaysSelected) {
                chAutoEnable.setChecked(false);
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
