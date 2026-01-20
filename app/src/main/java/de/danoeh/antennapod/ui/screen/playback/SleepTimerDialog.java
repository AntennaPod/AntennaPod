package de.danoeh.antennapod.ui.screen.playback;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.ui.common.Keyboard;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.TimeDialogBinding;
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
import de.danoeh.antennapod.ui.screen.preferences.PreferenceActivity;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class SleepTimerDialog extends BottomSheetDialogFragment {
    private static final int EXTEND_FEW_MINUTES_DISPLAY_VALUE = 5;
    private static final int EXTEND_FEW_MINUTES = 5 * 1000 * 60;
    private static final int EXTEND_MID_MINUTES_DISPLAY_VALUE = 10;
    private static final int EXTEND_MID_MINUTES = 10 * 1000 * 60;
    private static final int EXTEND_LOTS_MINUTES_DISPLAY_VALUE = 30;
    private static final int EXTEND_LOTS_MINUTES = 30 * 1000 * 60;
    private static final int EXTEND_FEW_EPISODES = 1;
    private static final int EXTEND_MID_EPISODES = 2;
    private static final int EXTEND_LOTS_EPISODES = 3;
    private static final int SLEEP_DURATION_DAILY_HOURS_CUTOFF = 12;

    private PlaybackController controller;
    private TimeDialogBinding viewBinding;
    private Disposable disposable;
    private volatile Integer currentQueueSize = null;

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

        disposable = Single.fromCallable(() -> {
            FeedMedia media = DBReader.getFeedMedia(PlaybackPreferences.getCurrentlyPlayingFeedMediaId());
            return DBReader.getRemainingQueueSize(media.getItemId());
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> currentQueueSize = result);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (controller != null) {
            controller.release();
        }
        if (disposable != null) {
            disposable.dispose();
            disposable = null;
        }
        EventBus.getDefault().unregister(this);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
            setupFullHeight(bottomSheetDialog);
        });
        return dialog;
    }

    private void setupFullHeight(BottomSheetDialog bottomSheetDialog) {
        FrameLayout bottomSheet = bottomSheetDialog.findViewById(R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
            ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
            bottomSheet.setLayoutParams(layoutParams);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        viewBinding = TimeDialogBinding.inflate(inflater);
        List<String> spinnerContent = new ArrayList<>();
        // add "title" for all options
        spinnerContent.add(getString(R.string.time_minutes));
        spinnerContent.add(getString(R.string.sleep_timer_episodes_label));
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_item, spinnerContent);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        viewBinding.sleepTimerType.setAdapter(spinnerAdapter);
        viewBinding.sleepTimerType.setSelection(SleepTimerPreferences.getSleepTimerType().index);
        viewBinding.sleepTimerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private boolean sleepTimerTypeInitialized = false;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {
                    SleepTimerPreferences.setLastTimer(String.valueOf(getSelectedSleepTime()));
                } catch (NumberFormatException ignore) {
                    // Ignore silently and just not save it
                }
                final SleepTimerType sleepType = SleepTimerType.fromIndex(position);
                SleepTimerPreferences.setSleepTimerType(sleepType);
                // this callback is called even when the spinner is first initialized
                // we need to differentiate these calls
                if (sleepTimerTypeInitialized) {
                    // disable auto sleep timer if they've configured it for most of the day
                    if (isSleepTimerConfiguredForMostOfTheDay()) {
                        viewBinding.autoEnableCheckbox.setChecked(false);
                    }
                    viewBinding.timeEditText.setText(SleepTimerPreferences.lastTimerValue());
                }
                sleepTimerTypeInitialized = true;
                refreshUiState();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        viewBinding.timeDisplayContainer.setVisibility(View.GONE);
        viewBinding.timeEditText.setText(SleepTimerPreferences.lastTimerValue());
        viewBinding.timeEditText.addTextChangedListener(new TextWatcher() {
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
        viewBinding.playbackPreferencesButton.setOnClickListener(view -> {
            final Intent playbackIntent = new Intent(getActivity(), PreferenceActivity.class);
            playbackIntent.putExtra(PreferenceActivity.OPEN_PLAYBACK_SETTINGS, true);
            startActivity(playbackIntent);
            dismiss();
        });
        refreshUiState();
        viewBinding.autoEnableCheckbox.setChecked(SleepTimerPreferences.autoEnable());
        viewBinding.shakeToResetCheckbox.setChecked(SleepTimerPreferences.shakeToReset());
        viewBinding.vibrateCheckbox.setChecked(SleepTimerPreferences.vibrate());
        refreshAutoEnableControls(SleepTimerPreferences.autoEnable());

        viewBinding.shakeToResetCheckbox.setOnCheckedChangeListener((buttonView, isChecked)
                -> SleepTimerPreferences.setShakeToReset(isChecked));
        viewBinding.vibrateCheckbox.setOnCheckedChangeListener((buttonView, isChecked)
                -> SleepTimerPreferences.setVibrate(isChecked));
        viewBinding.autoEnableCheckbox.setOnCheckedChangeListener((compoundButton, isChecked)
                -> {
            boolean mostOfDay = isSleepTimerConfiguredForMostOfTheDay();
            if (isChecked && mostOfDay && SleepTimerPreferences.getSleepTimerType() == SleepTimerType.EPISODES) {
                confirmAlwaysSleepTimerDialog();
            }
            refreshAutoEnableControls(isChecked);
        });
        updateAutoEnableText();

        viewBinding.changeTimesButton.setOnClickListener(changeTimesBtn -> {
            int from = SleepTimerPreferences.autoEnableFrom();
            int to = SleepTimerPreferences.autoEnableTo();
            showTimeRangeDialog(getContext(), from, to);
        });
        viewBinding.disableSleeptimerButton.setOnClickListener(v -> {
            if (controller != null) {
                controller.disableSleepTimer();
            }
        });
        viewBinding.setSleeptimerButton.setOnClickListener(v -> {
            if (!PlaybackService.isRunning
                    || (controller != null && controller.getStatus() != PlayerStatus.PLAYING)) {
                Snackbar.make(viewBinding.getRoot(), R.string.no_media_playing_label, Snackbar.LENGTH_LONG).show();
                return;
            }
            try {
                SleepTimerPreferences.setLastTimer("" + getSelectedSleepTime());
                if (controller != null) {
                    controller.setSleepTimer(SleepTimerPreferences.timerMillisOrEpisodes());
                }
                Keyboard.hide(getActivity());
            } catch (NumberFormatException e) {
                e.printStackTrace();
                Snackbar.make(viewBinding.getRoot(), R.string.time_dialog_invalid_input, Snackbar.LENGTH_LONG).show();
            }
        });
        return viewBinding.getRoot();
    }

    private boolean isSleepTimerConfiguredForMostOfTheDay() {
        return SleepTimerPreferences.autoEnableDuration() > SLEEP_DURATION_DAILY_HOURS_CUTOFF;
    }

    private void confirmAlwaysSleepTimerDialog() {
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.sleep_timer_without_continuous_playback)
                .setMessage(R.string.sleep_timer_without_continuous_playback_message)
                .setNegativeButton(R.string.sleep_timer_without_continuous_playback,
                        (dialogInterface, i) -> {
                            // disable continuous playback and also disable the auto sleep timer
                            UserPreferences.setFollowQueue(false);
                            viewBinding.autoEnableCheckbox.setChecked(false);
                            refreshUiState();
                        })
                .setPositiveButton(R.string.sleep_timer_without_continuous_playback_change_hours,
                        (dialogInterface, i) -> {
                            int from = SleepTimerPreferences.autoEnableFrom();
                            int to = SleepTimerPreferences.autoEnableTo();
                            showTimeRangeDialog(getContext(), from, to);
                        })
                .create();
        dialog.setOnCancelListener(dialogInterface -> viewBinding.autoEnableCheckbox.setChecked(false));
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(ThemeUtils.getColorFromAttr(requireContext(), R.attr.colorError));
    }

    private long getSelectedSleepTime() throws NumberFormatException {
        long time = Long.parseLong(viewBinding.timeEditText.getText().toString());
        if (time == 0) {
            throw new NumberFormatException("Timer must not be zero");
        }
        return time;
    }

    private void refreshAutoEnableControls(boolean enabled) {
        SleepTimerPreferences.setAutoEnable(enabled);
        viewBinding.changeTimesButton.setAlpha(enabled ? 1.0f : 0.5f);
    }

    private void refreshUiState() {
        // if we're using episode timer and continuous playback is disabled, don't
        // let the user use anything other than 1 episode
        boolean isEpisodeType = SleepTimerPreferences.getSleepTimerType() == SleepTimerType.EPISODES;
        boolean noEpisodeSelection = isEpisodeType && !UserPreferences.isFollowQueue();

        if (noEpisodeSelection) {
            viewBinding.timeEditText.setEnabled(false);
            viewBinding.playbackPreferencesButton.setVisibility(View.VISIBLE);
            viewBinding.sleepTimerHintText.setText(R.string.multiple_sleep_episodes_while_continuous_playback_disabled);
            viewBinding.sleepTimerHintText.setVisibility(View.VISIBLE);
            viewBinding.autoEnableCheckbox.setVisibility(View.GONE);
            viewBinding.changeTimesButton.setVisibility(View.GONE);
            viewBinding.shakeToResetCheckbox.setVisibility(View.GONE);
            viewBinding.vibrateCheckbox.setVisibility(View.GONE);
            viewBinding.setSleeptimerButton.setEnabled(false);
        } else {
            viewBinding.playbackPreferencesButton.setVisibility(View.GONE);
            viewBinding.autoEnableCheckbox.setVisibility(View.VISIBLE);
            viewBinding.changeTimesButton.setVisibility(View.VISIBLE);
            viewBinding.shakeToResetCheckbox.setVisibility(isEpisodeType ? View.GONE : View.VISIBLE);
            viewBinding.vibrateCheckbox.setVisibility(View.VISIBLE);
            viewBinding.setSleeptimerButton.setEnabled(true);
            viewBinding.timeEditText.setEnabled(true);
            long selectedSleepTime;
            try {
                selectedSleepTime = getSelectedSleepTime();
            } catch (NumberFormatException nex) {
                selectedSleepTime = 0;
            }

            if (isEpisodeType) {
                // for episode timers check if the queue length exceeds the number of sleep episodes we have
                if (currentQueueSize != null && selectedSleepTime > currentQueueSize) {
                    viewBinding.sleepTimerHintText.setText(getResources().getQuantityString(
                            R.plurals.episodes_sleep_timer_exceeds_queue,
                            currentQueueSize,
                            currentQueueSize));
                    viewBinding.sleepTimerHintText.setVisibility(View.VISIBLE);
                } else {
                    viewBinding.sleepTimerHintText.setVisibility(View.GONE);
                }
            } else {
                // for time sleep timers check if the selected value exceeds the remaining play time in the episode
                final int remaining = controller != null ? controller.getDuration() - controller.getPosition() :
                        Integer.MAX_VALUE;
                final long timer = TimeUnit.MINUTES.toMillis(selectedSleepTime);
                if ((timer > remaining) && !UserPreferences.isFollowQueue()) {
                    final int remainingMinutes = Math.toIntExact(TimeUnit.MILLISECONDS.toMinutes(remaining));
                    viewBinding.sleepTimerHintText
                            .setText(getResources().getQuantityString(
                                    R.plurals.timer_exceeds_remaining_time_while_continuous_playback_disabled,
                                    remainingMinutes,
                                    remainingMinutes
                            ));
                    viewBinding.sleepTimerHintText.setVisibility(View.VISIBLE);
                } else {
                    // don't show it at all
                    viewBinding.sleepTimerHintText.setVisibility(View.GONE); // could maybe show duration in minutes
                }
            }
        }

        // disable extension for episodes if we're not moving to next one
        viewBinding.extendSleepFiveMinutesButton.setEnabled(!noEpisodeSelection);
        viewBinding.extendSleepTenMinutesButton.setEnabled(!noEpisodeSelection);
        viewBinding.extendSleepTwentyMinutesButton.setEnabled(!noEpisodeSelection);

        if (SleepTimerPreferences.getSleepTimerType() == SleepTimerType.CLOCK) {
            setupExtendButton(viewBinding.extendSleepFiveMinutesButton,
                    getString(R.string.extend_sleep_timer_label, EXTEND_FEW_MINUTES_DISPLAY_VALUE),
                    EXTEND_FEW_MINUTES);
            setupExtendButton(viewBinding.extendSleepTenMinutesButton,
                    getString(R.string.extend_sleep_timer_label, EXTEND_MID_MINUTES_DISPLAY_VALUE),
                    EXTEND_MID_MINUTES);
            setupExtendButton(viewBinding.extendSleepTwentyMinutesButton,
                    getString(R.string.extend_sleep_timer_label, EXTEND_LOTS_MINUTES_DISPLAY_VALUE),
                    EXTEND_LOTS_MINUTES);
        } else {
            setupExtendButton(viewBinding.extendSleepFiveMinutesButton,
                    "+" + getResources().getQuantityString(R.plurals.num_episodes,
                    EXTEND_FEW_EPISODES, EXTEND_FEW_EPISODES), EXTEND_FEW_EPISODES);
            setupExtendButton(viewBinding.extendSleepTenMinutesButton,
                    "+" + getResources().getQuantityString(R.plurals.num_episodes,
                    EXTEND_MID_EPISODES, EXTEND_MID_EPISODES), EXTEND_MID_EPISODES);
            setupExtendButton(viewBinding.extendSleepTwentyMinutesButton,
                    "+" + getResources().getQuantityString(R.plurals.num_episodes,
                    EXTEND_LOTS_EPISODES, EXTEND_LOTS_EPISODES), EXTEND_LOTS_EPISODES);
        }
    }

    void setupExtendButton(TextView button, String text, int extendValue) {
        button.setText(text);
        button.setOnClickListener(v -> {
            if (controller != null) {
                controller.extendSleepTimer(extendValue);
            }
        });
    }

    private void showTimeRangeDialog(Context context, int from, int to) {
        TimeRangeDialog dialog = new TimeRangeDialog(context, from, to);
        dialog.setOnDismissListener(v -> {
            SleepTimerPreferences.setAutoEnableFrom(dialog.getFrom());
            SleepTimerPreferences.setAutoEnableTo(dialog.getTo());
            boolean mostOfDay = isSleepTimerConfiguredForMostOfTheDay();
            // disable the checkbox if they've selected always
            // only change the state if true, don't change it regardless of flag (although we could)
            if (mostOfDay && SleepTimerPreferences.getSleepTimerType() == SleepTimerType.EPISODES) {
                confirmAlwaysSleepTimerDialog();
            } else if (!viewBinding.autoEnableCheckbox.isChecked()) {
                // if it's not checked, then make sure it's checked in UI too
                viewBinding.autoEnableCheckbox.setChecked(true);
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
        viewBinding.autoEnableCheckbox.setText(text);
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    @SuppressWarnings("unused")
    public void timerUpdated(SleepTimerUpdatedEvent event) {
        viewBinding.timeDisplayContainer.setVisibility(
                event.isOver() || event.isCancelled() ? View.GONE : View.VISIBLE);
        viewBinding.timeSetupContainer.setVisibility(event.isOver() || event.isCancelled() ? View.VISIBLE : View.GONE);
        viewBinding.sleepTimerType.setEnabled(event.isOver() || event.isCancelled());

        if (SleepTimerPreferences.getSleepTimerType() == SleepTimerType.EPISODES) {
            viewBinding.time.setText(getResources().getQuantityString(R.plurals.num_episodes,
                    (int) event.getDisplayTimeLeft(), (int) event.getDisplayTimeLeft()));
        } else {
            viewBinding.time.setText(Converter.getDurationStringLong((int) event.getDisplayTimeLeft()));
        }
    }
}
