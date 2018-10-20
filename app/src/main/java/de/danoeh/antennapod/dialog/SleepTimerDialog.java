package de.danoeh.antennapod.dialog;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.event.MessageEvent;
import de.danoeh.antennapod.core.preferences.SleepTimerPreferences;
import de.greenrobot.event.EventBus;

public abstract class SleepTimerDialog {
    
    private static final String TAG = SleepTimerDialog.class.getSimpleName();

    private final Context context;

    private MaterialDialog dialog;
    private EditText etxtTime;
    private Spinner spTimeUnit;
    private CheckBox cbShakeToReset;
    private CheckBox cbVibrate;
    private CheckBox chAutoEnable;


    protected SleepTimerDialog(Context context) {
        this.context = context;
    }

    public MaterialDialog createNewDialog() {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(context);
        builder.title(R.string.set_sleeptimer_label);
        builder.customView(R.layout.time_dialog, false);
        builder.positiveText(R.string.set_sleeptimer_label);
        builder.negativeText(R.string.cancel_label);
        builder.onNegative((dialog, which) -> dialog.dismiss());
        builder.onPositive((dialog, which) -> {
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
        dialog = builder.build();
        
        View view = dialog.getView();
        etxtTime = (EditText) view.findViewById(R.id.etxtTime);
        spTimeUnit = (Spinner) view.findViewById(R.id.spTimeUnit);
        cbShakeToReset = (CheckBox) view.findViewById(R.id.cbShakeToReset);
        cbVibrate = (CheckBox) view.findViewById(R.id.cbVibrate);
        chAutoEnable = (CheckBox) view.findViewById(R.id.chAutoEnable);

        etxtTime.setText(SleepTimerPreferences.lastTimerValue());
        etxtTime.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                checkInputLength(s.length());
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

    private void checkInputLength(int length) {
        if (length > 0) {
            Log.d(TAG, "Length is larger than 0, enabling confirm button");
            dialog.getActionButton(DialogAction.POSITIVE).setEnabled(true);
        } else {
            Log.d(TAG, "Length is smaller than 0, disabling confirm button");
            dialog.getActionButton(DialogAction.POSITIVE).setEnabled(false);
        }
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
