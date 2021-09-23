package de.danoeh.antennapod.fragment.preferences.dialog;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import de.danoeh.antennapod.R;

public class PreferenceListDialog {
    protected Context context;
    private String title;
    private OnPreferenceChangedListener onPreferenceChangedListener;
    private int selectedPos = 0;

    public PreferenceListDialog(Context context, String title) {
        this.context = context;
        this.title = title;
    }

    public interface OnPreferenceChangedListener {
        /**
         * Notified when user confirms preference
         *
         * @param pos The index of the item that was selected
         */

        void preferenceChanged(int pos);
    }

    public void openDialog(String[] items) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setSingleChoiceItems(items, selectedPos, (dialog, which) -> {
            selectedPos = which;
        });
        builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> {
            if (onPreferenceChangedListener != null && selectedPos >= 0) {
                onPreferenceChangedListener.preferenceChanged(selectedPos);
            }
        });
        builder.setNegativeButton(R.string.cancel_label, null);
        builder.create().show();
    }

    public void setOnPreferenceChangedListener(OnPreferenceChangedListener onPreferenceChangedListener) {
        this.onPreferenceChangedListener = onPreferenceChangedListener;
    }
}
