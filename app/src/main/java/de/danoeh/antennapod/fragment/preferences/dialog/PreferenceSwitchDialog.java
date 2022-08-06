package de.danoeh.antennapod.fragment.preferences.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.appcompat.widget.SwitchCompat;

import de.danoeh.antennapod.R;

public class PreferenceSwitchDialog {
    protected Context context;
    private String title;
    private String text;
    private OnPreferenceChangedListener onPreferenceChangedListener;

    public PreferenceSwitchDialog(Context context, String title, String text) {
        this.context = context;
        this.title = title;
        this.text = text;
    }

    public interface OnPreferenceChangedListener {
        /**
         * Notified when user confirms preference
         *
         * @param enabled The preference
         */

        void preferenceChanged(boolean enabled);
    }

    public void openDialog() {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(title);

        LayoutInflater inflater = LayoutInflater.from(this.context);
        View layout = inflater.inflate(R.layout.dialog_switch_preference, null, false);
        SwitchCompat switchButton = layout.findViewById(R.id.dialogSwitch);
        switchButton.setText(text);
        builder.setView(layout);

        builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> {
            if (onPreferenceChangedListener != null) {
                onPreferenceChangedListener.preferenceChanged(switchButton.isChecked());
            }
        });
        builder.setNegativeButton(R.string.cancel_label, null);
        builder.create().show();
    }

    public void setOnPreferenceChangedListener(OnPreferenceChangedListener onPreferenceChangedListener) {
        this.onPreferenceChangedListener = onPreferenceChangedListener;
    }
}
