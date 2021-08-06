package de.danoeh.antennapod.fragment.preferences.dialog;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.FeedItemFilterGroup;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.ui.common.RecursiveRadioGroup;

public class PreferenceSwitchDialog  {
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
         * @param enabled The preference
         */

        void preferenceChanged(boolean enabled);
    }
    public void openDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);

        LayoutInflater inflater = LayoutInflater.from(this.context);
        View layout = inflater.inflate(R.layout.dialog_switch_preference, null, false);
        Switch switchButton = layout.findViewById(R.id.switch1);
        switchButton.setText(text);
        builder.setView(layout);

        builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> {
            if(onPreferenceChangedListener != null)
                onPreferenceChangedListener.preferenceChanged(switchButton.isChecked());
        });
        builder.setNegativeButton(R.string.cancel_label, null);
        builder.create().show();
    }

    public void setOnPreferenceChangedListener(OnPreferenceChangedListener onPreferenceChangedListener) {
        this.onPreferenceChangedListener = onPreferenceChangedListener;
    }
}
