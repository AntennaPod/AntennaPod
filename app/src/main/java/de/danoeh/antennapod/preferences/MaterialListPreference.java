package de.danoeh.antennapod.preferences;

import android.content.Context;
import android.util.AttributeSet;
import androidx.preference.ListPreference;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MaterialListPreference extends ListPreference {

    public MaterialListPreference(Context context) {
        super(context);
    }

    public MaterialListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onClick() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
        builder.setTitle(getTitle());
        builder.setIcon(getDialogIcon());
        builder.setNegativeButton(getNegativeButtonText(), null);

        CharSequence[] values = getEntryValues();
        int selected = -1;
        for (int i = 0; i < values.length; i++) {
            if (values[i].toString().equals(getValue())) {
                selected = i;
            }
        }
        builder.setSingleChoiceItems(getEntries(), selected, (dialog, which) -> {
            dialog.dismiss();
            if (which >= 0 && getEntryValues() != null) {
                String value = getEntryValues()[which].toString();
                if (callChangeListener(value)) {
                    setValue(value);
                }
            }
        });
        builder.show();
    }
}
