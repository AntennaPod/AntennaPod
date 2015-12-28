package de.danoeh.antennapod.preferences;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;

import de.danoeh.antennapod.R;

public class SwitchCompatPreference extends CheckBoxPreference {

    public SwitchCompatPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SwitchCompatPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public SwitchCompatPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SwitchCompatPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        setWidgetLayoutResource(R.layout.preference_switch_layout);
    }
}