package de.danoeh.antennapod.view;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Samsung's Android 6.0.1 has a bug that crashes the app when inflating a time picker.
 * This class serves as a workaround for affected devices.
 */
public class TimePicker extends android.widget.TimePicker {
    public TimePicker(Context context) {
        super(context);
    }

    public TimePicker(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TimePicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        try {
            super.onRtlPropertiesChanged(layoutDirection);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
