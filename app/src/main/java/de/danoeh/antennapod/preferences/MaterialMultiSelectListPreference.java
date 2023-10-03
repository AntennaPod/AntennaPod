package de.danoeh.antennapod.preferences;

import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import androidx.preference.MultiSelectListPreference;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.HashSet;
import java.util.Set;

public class MaterialMultiSelectListPreference extends MultiSelectListPreference {

    public MaterialMultiSelectListPreference(Context context) {
        super(context);
    }

    public MaterialMultiSelectListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onClick() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
        builder.setTitle(getTitle());
        builder.setIcon(getDialogIcon());
        builder.setNegativeButton(getNegativeButtonText(), null);

        boolean[] selected = new boolean[getEntries().length];
        CharSequence[] values = getEntryValues();
        for (int i = 0; i < values.length; i++) {
            selected[i] = getValues().contains(values[i].toString());
        }
        builder.setMultiChoiceItems(getEntries(), selected,
                (DialogInterface dialog, int which, boolean isChecked) -> selected[which] = isChecked);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            Set<String> selectedValues = new HashSet<>();
            for (int i = 0; i < values.length; i++) {
                if (selected[i]) {
                    selectedValues.add(getEntryValues()[i].toString());
                }
            }
            setValues(selectedValues);
        });
        builder.show();
    }
}
