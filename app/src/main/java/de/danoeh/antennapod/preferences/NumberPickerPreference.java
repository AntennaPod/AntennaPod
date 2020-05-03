package de.danoeh.antennapod.preferences;

import android.content.Context;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import android.text.InputFilter;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import de.danoeh.antennapod.R;

public class NumberPickerPreference extends Preference {
    private Context context;
    private int defaultValue = 0;
    private int minValue = 0;
    private int maxValue = Integer.MAX_VALUE;

    public NumberPickerPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    public NumberPickerPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public NumberPickerPreference(Context context) {
        super(context);
        this.context = context;
    }

    private void init(Context context, AttributeSet attrs) {
        this.context = context;

        for (int i = 0; i < attrs.getAttributeCount(); i++) {
            String name = attrs.getAttributeName(i);
            String value = attrs.getAttributeValue(i);
            switch (name) {
                case "defaultValue":
                    defaultValue = Integer.parseInt(value);
                    break;
                case "minValue":
                    minValue = Integer.parseInt(value);
                    break;
                case "maxValue":
                    maxValue = Integer.parseInt(value);
                    break;
            }
        }
    }

    @Override
    protected void onClick() {
        super.onClick();

        View view = View.inflate(context, R.layout.numberpicker, null);
        EditText number = view.findViewById(R.id.number);
        number.setText(getSharedPreferences().getString(getKey(), ""+defaultValue));
        number.setFilters(new InputFilter[]{(source, start, end, dest, dstart, dend) -> {
            try {
                String newVal = dest.toString().substring(0, dstart) + dest.toString().substring(dend);
                newVal = newVal.substring(0, dstart) + source.toString() + newVal.substring(dstart);
                int input = Integer.parseInt(newVal);
                if (input >= minValue && input <= maxValue) {
                    return null;
                }
            } catch (NumberFormatException nfe) {
                nfe.printStackTrace();
            }
            return "";
        }});

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(getTitle())
                .setView(view)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    try {
                        String numberString = number.getText().toString();
                        int value = Integer.parseInt(numberString);

                        if (value < minValue || value > maxValue) {
                            return;
                        }

                        getSharedPreferences().edit().putString(getKey(), "" + value).apply();

                        if (getOnPreferenceChangeListener() != null) {
                            getOnPreferenceChangeListener().onPreferenceChange(this, value);
                        }
                    } catch (NumberFormatException e) {
                        // Do not set value
                    }
                })
                .create();
        dialog.show();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }
}
