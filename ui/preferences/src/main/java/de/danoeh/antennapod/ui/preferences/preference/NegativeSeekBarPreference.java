package de.danoeh.antennapod.ui.preferences.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SeekBarPreference;

import de.danoeh.antennapod.ui.preferences.R;

// NegativeSeekBarPreference extends SeekBarPreference and enables negative integer values.
// The attributes are max/min/defaultValue, not max/min/defaultVal, to make them different from SeekBarPreference.
// Internally, an offset is used so SeekBarPreference only sees non-negative values. The range is this class' max-min,
// and SeekBarPreference will have its min set to 0 and its max set to this class' range.
// Writing to and reading from persistent storage is overridden, too: This class' value (possibly negative) is used.
public class NegativeSeekBarPreference extends SeekBarPreference {

    private static final String TAG = "NegativeSeekBarPref";
    private static final int MAX_VALUE_INIT = 50;
    private static final int MIN_VALUE_INIT = -50;
    private static final int DEF_VALUE_INIT = 0;

    private int maxValue = MAX_VALUE_INIT;
    private int minValue = MIN_VALUE_INIT;
    private int defValue = DEF_VALUE_INIT;

    @SuppressWarnings("unused")
    public NegativeSeekBarPreference(@NonNull Context context) {
        this(context, null);
    }

    public NegativeSeekBarPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.seekBarPreferenceStyle, 0);
    }

    @SuppressWarnings("unused")
    public NegativeSeekBarPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    @SuppressWarnings("this-escape")
    public NegativeSeekBarPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr,
                                     int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        setShowSeekBarValue(true);

        if (attrs != null) {
            try (TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.NegativeSeekBarPreference)) {
                maxValue = a.getInt(R.styleable.NegativeSeekBarPreference_maxValue, maxValue);
                minValue = a.getInt(R.styleable.NegativeSeekBarPreference_minValue, minValue);
                defValue = a.getInt(R.styleable.NegativeSeekBarPreference_defValue, defValue);
            } catch (Exception e) {
                Log.e(TAG, "Error while reading attributes for NegativeSeekBarPreference: " + e.getMessage());
            }
        }

        super.setMin(0);
        super.setMax(getRange());
        super.setDefaultValue(convertToSuperValue(defValue));
    }

    private int getRange() {
        return getRange(maxValue, minValue);
    }

    private static int getRange(int maxVal, int minVal) {
        return maxVal - minVal;
    }

    private int convertToSuperValue(int val) {
        return convertToSuperValue(val, minValue);
    }

    private static int convertToSuperValue(int val, int minVal) {
        return val - minVal;
    }

    private int convertFromSuperValue(int val) {
        return convertFromSuperValue(val, minValue);
    }

    private static int convertFromSuperValue(int val, int minVal) {
        return val + minVal;
    }

    @Override
    public int getMin() {
        return minValue;
    }

    @Override
    public int getMax() {
        return maxValue;
    }

    @Override
    public int getValue() {
        return convertFromSuperValue(super.getValue());
    }

    @Override
    public void setValue(int value) {
        super.setValue(convertToSuperValue(value));
    }

    @Override
    public void setDefaultValue(Object defaultValue) {
        if (defaultValue == null) {
            defaultValue = 0;
        }

        if (defaultValue instanceof Integer) {
            defValue = (Integer) defaultValue;
            int defSuperVal = convertToSuperValue(defValue);
            super.setDefaultValue(defSuperVal);
        } else {
            super.setDefaultValue(defaultValue);
        }
    }

    @Override
    protected boolean persistInt(int superValue) {
        int val = convertFromSuperValue(superValue);
        return super.persistInt(val);
    }

    @Override
    protected int getPersistedInt(int defaultReturnValue) {
        int persistedValue = super.getPersistedInt(defaultReturnValue);
        return convertToSuperValue(persistedValue);
    }

    @Override
    protected void onSetInitialValue(Object superDefaultValue) {
        int defaultVal = 0;
        if (superDefaultValue instanceof Integer) {
            defaultVal = convertFromSuperValue((Integer) superDefaultValue);
        }

        int persistedSuperVal = getPersistedInt(defaultVal);
        int persistedVal = convertFromSuperValue(persistedSuperVal);
        setValue(persistedVal);
    }

    // Adapt the shown value
    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        SeekBar seekBar = (SeekBar) holder.findViewById(androidx.preference.R.id.seekbar);
        TextView valueView = (TextView) holder.findViewById(androidx.preference.R.id.seekbar_value);
        if (seekBar != null && valueView != null) {
            valueView.setText(String.valueOf(getValue()));

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int value = convertFromSuperValue(progress);
                    valueView.setText(String.valueOf(value));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    int value = convertFromSuperValue(seekBar.getProgress());
                    setValue(value);
                }
            });
        }
    }
}