package de.danoeh.antennapod.ui.screen.playback;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
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

import de.danoeh.antennapod.playback.base.PlayerStatus;
import de.danoeh.antennapod.playback.service.PlaybackController;
import de.danoeh.antennapod.playback.service.PlaybackService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.storage.preferences.SleepTimerPreferences;
import de.danoeh.antennapod.storage.preferences.SleepTimerType;
import de.danoeh.antennapod.ui.common.Converter;
import de.danoeh.antennapod.event.playback.SleepTimerUpdatedEvent;

public class SleepTimerDialog extends DialogFragment {
    private PlaybackController controller;
    private EditText etxtTime;
    private LinearLayout timeSetup;
    private LinearLayout timeDisplay;
    private TextView time;
    private Spinner sleepTimerType;
    private CheckBox chAutoEnable;

    Button extendSleepFiveMinutesButton;
    Button extendSleepTenMinutesButton;
    Button extendSleepTwentyMinutesButton;

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

        public SleepTimeConfig(
                int buttonTextResourceId,
                int displayTypeTextId,
                SleepEntryConfig first,
                SleepEntryConfig second, SleepEntryConfig third) {
            this.buttonTextResourceId = buttonTextResourceId;
            this.displayTypeTextId = displayTypeTextId;
            sleepEntries.add(0, first);
            sleepEntries.add(1, second);
            sleepEntries.add(2, third);
        }
    }

    private static <K, V> LinkedHashMap<K, V> linkedMapOf(Collection<Map.Entry<? extends K, ? extends V>> entries) {
        LinkedHashMap<K, V> map = new LinkedHashMap<>();
        for (Map.Entry<? extends K, ? extends V> entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    LinkedHashMap<SleepTimerType, SleepTimeConfig> allSleepConfig = linkedMapOf(List.of(
            Map.entry(SleepTimerType.CLOCK, new SleepTimeConfig(
                    R.string.extend_sleep_timer_label, R.string.time_minutes,
                    new SleepEntryConfig(5, 5 * 1000 * 60),
                    new SleepEntryConfig(10, 10 * 1000 * 60),
                    new SleepEntryConfig(30, 30 * 1000 * 60)
            )),
            Map.entry(SleepTimerType.EPISODES, new SleepTimeConfig(
                    R.plurals.extend_sleep_timer_episodes_quantified, R.string.episodes_label,
                    new SleepEntryConfig(1, 1),
                    new SleepEntryConfig(3, 3),
                    new SleepEntryConfig(5, 5)
            ))
    ));

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
        int count = 0;
        for (SleepTimerType type : allSleepConfig.keySet()) {
            if (type == sleepTimerType) {
                return count;
            }
            count++;
        }

        return 0;
    }

    private SleepTimerType getSleepTimerTypeFromIndex(int selection) {
        int count = 0;
        for (SleepTimerType type : allSleepConfig.keySet()) {
            if (count++ == selection) {
                return type;
            }
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
        for (SleepTimeConfig entry : allSleepConfig.values()) {
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
                refreshExtendButtons();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        etxtTime = content.findViewById(R.id.etxtTime);
        timeSetup = content.findViewById(R.id.timeSetup);
        timeDisplay = content.findViewById(R.id.timeDisplay);
        timeDisplay.setVisibility(View.GONE);
        time = content.findViewById(R.id.time);
        extendSleepFiveMinutesButton = content.findViewById(R.id.extendSleepFiveMinutesButton);
        extendSleepTenMinutesButton = content.findViewById(R.id.extendSleepTenMinutesButton);
        extendSleepTwentyMinutesButton = content.findViewById(R.id.extendSleepTwentyMinutesButton);

        refreshExtendButtons();

        etxtTime.setText(SleepTimerPreferences.lastTimerValue());
        etxtTime.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(etxtTime, InputMethodManager.SHOW_IMPLICIT);
        }, 100);

        final CheckBox cbShakeToReset = content.findViewById(R.id.cbShakeToReset);
        final CheckBox cbVibrate = content.findViewById(R.id.cbVibrate);
        chAutoEnable = content.findViewById(R.id.chAutoEnable);
        final ImageView changeTimesButton = content.findViewById(R.id.changeTimesButton);

        cbShakeToReset.setChecked(SleepTimerPreferences.shakeToReset());
        cbVibrate.setChecked(SleepTimerPreferences.vibrate());
        chAutoEnable.setChecked(SleepTimerPreferences.autoEnable());
        changeTimesButton.setEnabled(chAutoEnable.isChecked());
        changeTimesButton.setAlpha(chAutoEnable.isChecked() ? 1.0f : 0.5f);

        cbShakeToReset.setOnCheckedChangeListener((buttonView, isChecked)
                -> SleepTimerPreferences.setShakeToReset(isChecked));
        cbVibrate.setOnCheckedChangeListener((buttonView, isChecked)
                -> SleepTimerPreferences.setVibrate(isChecked));
        chAutoEnable.setOnCheckedChangeListener((compoundButton, isChecked)
                -> {
            SleepTimerPreferences.setAutoEnable(isChecked);
            changeTimesButton.setEnabled(isChecked);
            changeTimesButton.setAlpha(isChecked ? 1.0f : 0.5f);
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
                long time = Long.parseLong(etxtTime.getText().toString());
                if (time == 0) {
                    throw new NumberFormatException("Timer must not be zero");
                }
                SleepTimerPreferences.setLastTimer(etxtTime.getText().toString());
                if (controller != null) {
                    long value = SleepTimerPreferences.timerMillisOrEpisodes();
                    controller.setSleepTimer(value);
                }
                closeKeyboard(content);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                Snackbar.make(content, R.string.time_dialog_invalid_input, Snackbar.LENGTH_LONG).show();
            }
        });
        return builder.create();
    }

    private void refreshExtendButtons() {
        final SleepTimeConfig selectedConfig = allSleepConfig.get(SleepTimerPreferences.getSleepTimerType());

        int counter = 0;
        for (Button button : List.of(
                extendSleepFiveMinutesButton,
                extendSleepTenMinutesButton,
                extendSleepTwentyMinutesButton)) {
            final SleepEntryConfig entryConfig = Objects.requireNonNull(selectedConfig).sleepEntries.get(counter++);
            // button text resource can be either string or plural string
            try {
                button.setText(getString(selectedConfig.buttonTextResourceId, entryConfig.displayValue));
            } catch (Resources.NotFoundException ex) {
                button.setText(getContext().getResources()
                        .getQuantityString(
                                selectedConfig.buttonTextResourceId,
                                entryConfig.displayValue,
                                entryConfig.displayValue));
            }

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

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    public void timerUpdated(SleepTimerUpdatedEvent event) {
        timeDisplay.setVisibility(event.isOver() || event.isCancelled() ? View.GONE : View.VISIBLE);
        timeSetup.setVisibility(event.isOver() || event.isCancelled() ? View.VISIBLE : View.GONE);
        sleepTimerType.setEnabled(event.isOver() || event.isCancelled());

        switch (SleepTimerPreferences.getSleepTimerType()) {
            case EPISODES -> time.setText(String.valueOf(event.getTimeLeft()));
            default -> time.setText(Converter.getDurationStringLong((int) event.getTimeLeft()));
        }

    }

    private void closeKeyboard(View content) {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(content.getWindowToken(), 0);
    }
}
