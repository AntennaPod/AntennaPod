package de.danoeh.antennapod.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.SleepTimerPreferences;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.playback.PlaybackController;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

import java.util.concurrent.TimeUnit;

public class SleepTimerDialog extends DialogFragment {
    private PlaybackController controller;
    private Disposable timeUpdater;

    private EditText etxtTime;
    private Spinner spTimeUnit;
    private CheckBox cbShakeToReset;
    private CheckBox cbVibrate;
    private CheckBox chAutoEnable;
    private LinearLayout timeSetup;
    private LinearLayout timeDisplay;
    private TextView time;

    public SleepTimerDialog() {

    }

    @Override
    public void onStart() {
        super.onStart();
        controller = new PlaybackController(getActivity(), false) {
            @Override
            public void setupGUI() {
                updateTime();
            }

            @Override
            public void onSleepTimerUpdate() {
                updateTime();
            }
        };
        controller.init();
        timeUpdater = Observable.interval(1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tick -> updateTime());
    }

    @Override
    public void onStop() {
        super.onStop();
        if (controller != null) {
            controller.release();
        }
        if (timeUpdater != null) {
            timeUpdater.dispose();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View content = View.inflate(getContext(), R.layout.time_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.sleep_timer_label);
        builder.setView(content);
        builder.setPositiveButton(android.R.string.ok, null);

        etxtTime = content.findViewById(R.id.etxtTime);
        spTimeUnit = content.findViewById(R.id.spTimeUnit);
        cbShakeToReset = content.findViewById(R.id.cbShakeToReset);
        cbVibrate = content.findViewById(R.id.cbVibrate);
        chAutoEnable = content.findViewById(R.id.chAutoEnable);
        timeSetup = content.findViewById(R.id.timeSetup);
        timeDisplay = content.findViewById(R.id.timeDisplay);
        time = content.findViewById(R.id.time);

        AlertDialog dialog = builder.create();
        etxtTime.setText(SleepTimerPreferences.lastTimerValue());
        etxtTime.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(s.length() > 0);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        etxtTime.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(etxtTime, InputMethodManager.SHOW_IMPLICIT);
        }, 100);

        String[] spinnerContent = new String[] {
                getString(R.string.time_seconds),
                getString(R.string.time_minutes),
                getString(R.string.time_hours) };
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, spinnerContent);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTimeUnit.setAdapter(spinnerAdapter);
        spTimeUnit.setSelection(SleepTimerPreferences.lastTimerTimeUnit());

        cbShakeToReset.setChecked(SleepTimerPreferences.shakeToReset());
        cbVibrate.setChecked(SleepTimerPreferences.vibrate());
        chAutoEnable.setChecked(SleepTimerPreferences.autoEnable());

        chAutoEnable.setOnCheckedChangeListener((compoundButton, isChecked)
                ->  SleepTimerPreferences.setAutoEnable(isChecked));
        Button disableButton = content.findViewById(R.id.disableSleeptimerButton);
        disableButton.setOnClickListener(v -> {
            if (controller != null) {
                controller.disableSleepTimer();
            }
        });
        Button setButton = content.findViewById(R.id.setSleeptimerButton);
        setButton.setOnClickListener(v -> {
            if (!PlaybackService.isRunning) {
                Toast.makeText(getContext(), R.string.no_media_playing_label, Toast.LENGTH_LONG).show();
            }
            try {
                savePreferences();
                long time = SleepTimerPreferences.timerMillis();
                if (controller != null) {
                    controller.setSleepTimer(time, cbShakeToReset.isChecked(), cbVibrate.isChecked());
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
                Toast.makeText(getContext(), R.string.time_dialog_invalid_input, Toast.LENGTH_LONG).show();
            }
        });
        return dialog;
    }

    private void savePreferences() {
        SleepTimerPreferences.setLastTimer(etxtTime.getText().toString(),
                spTimeUnit.getSelectedItemPosition());
        SleepTimerPreferences.setShakeToReset(cbShakeToReset.isChecked());
        SleepTimerPreferences.setVibrate(cbVibrate.isChecked());
        SleepTimerPreferences.setAutoEnable(chAutoEnable.isChecked());
    }

    private void updateTime() {
        if (controller == null) {
            return;
        }
        timeSetup.setVisibility(controller.sleepTimerActive() ? View.GONE : View.VISIBLE);
        timeDisplay.setVisibility(controller.sleepTimerActive() ? View.VISIBLE : View.GONE);
        time.setText(Converter.getDurationStringLong((int) controller.getSleepTimerTimeLeft()));
    }
}
