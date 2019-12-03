package de.danoeh.antennapodSA.dialog;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import org.greenrobot.eventbus.EventBus;

import de.danoeh.antennapodSA.R;
import de.danoeh.antennapodSA.core.event.MessageEvent;
import de.danoeh.antennapodSA.core.preferences.SleepTimerPreferences;

public abstract class SleepTimerDialog {
    
    private static final String TAG = SleepTimerDialog.class.getSimpleName();

    private final Context context;

    private AlertDialog dialog;
    private EditText etxtTime;
    private Spinner spTimeUnit;
    private CheckBox cbShakeToReset;
    private CheckBox cbVibrate;
    private CheckBox chAutoEnable;


    protected SleepTimerDialog(Context context) {
        this.context = context;
    }

    public AlertDialog createNewDialog() {
        View content = View.inflate(context, R.layout.time_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.set_sleeptimer_label);
        builder.setView(content);
        builder.setNegativeButton(R.string.cancel_label, (dialog, which) -> dialog.dismiss());
        builder.setPositiveButton(R.string.set_sleeptimer_label, (dialog, which) -> {
            try {
                savePreferences();
                long input = SleepTimerPreferences.timerMillis();
                onTimerSet(input, cbShakeToReset.isChecked(), cbVibrate.isChecked());
                dialog.dismiss();
            } catch (NumberFormatException e) {
                e.printStackTrace();
                Toast toast = Toast.makeText(context, R.string.time_dialog_invalid_input,
                        Toast.LENGTH_LONG);
                toast.show();
            }
        });
        dialog = builder.create();

        etxtTime = content.findViewById(R.id.etxtTime);
        spTimeUnit = content.findViewById(R.id.spTimeUnit);
        cbShakeToReset = content.findViewById(R.id.cbShakeToReset);
        cbVibrate = content.findViewById(R.id.cbVibrate);
        chAutoEnable = content.findViewById(R.id.chAutoEnable);

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
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(etxtTime, InputMethodManager.SHOW_IMPLICIT);
        }, 100);

        String[] spinnerContent = new String[] {
                context.getString(R.string.time_seconds),
                context.getString(R.string.time_minutes),
                context.getString(R.string.time_hours) };
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, spinnerContent);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTimeUnit.setAdapter(spinnerAdapter);
        spTimeUnit.setSelection(SleepTimerPreferences.lastTimerTimeUnit());

        cbShakeToReset.setChecked(SleepTimerPreferences.shakeToReset());
        cbVibrate.setChecked(SleepTimerPreferences.vibrate());
        chAutoEnable.setChecked(SleepTimerPreferences.autoEnable());

        chAutoEnable.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            SleepTimerPreferences.setAutoEnable(isChecked);
            int messageString = isChecked ? R.string.sleep_timer_enabled_label : R.string.sleep_timer_disabled_label;
            EventBus.getDefault().post(new MessageEvent(context.getString(messageString)));
        });
        return dialog;
    }

    public abstract void onTimerSet(long millis, boolean shakeToReset, boolean vibrate);

    private void savePreferences() {
        SleepTimerPreferences.setLastTimer(etxtTime.getText().toString(),
                spTimeUnit.getSelectedItemPosition());
        SleepTimerPreferences.setShakeToReset(cbShakeToReset.isChecked());
        SleepTimerPreferences.setVibrate(cbVibrate.isChecked());
        SleepTimerPreferences.setAutoEnable(chAutoEnable.isChecked());
    }

}
