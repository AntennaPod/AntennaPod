package de.danoeh.antennapod.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

import org.apache.commons.lang3.Validate;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;

/**
 * Creates a new AlertDialog that displays preferences for auto-flattring to the user.
 */
public class AutoFlattrPreferenceDialog {

    private AutoFlattrPreferenceDialog() {
    }

    public static void newAutoFlattrPreferenceDialog(final Activity activity, final AutoFlattrPreferenceDialogInterface callback) {
        Validate.notNull(activity);
        Validate.notNull(callback);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        @SuppressLint("InflateParams") View view = activity.getLayoutInflater().inflate(R.layout.autoflattr_preference_dialog, null);
        final CheckBox chkAutoFlattr = (CheckBox) view.findViewById(R.id.chkAutoFlattr);
        final SeekBar skbPercent = (SeekBar) view.findViewById(R.id.skbPercent);
        final TextView txtvStatus = (TextView) view.findViewById(R.id.txtvStatus);

        chkAutoFlattr.setChecked(UserPreferences.isAutoFlattr());
        skbPercent.setEnabled(chkAutoFlattr.isChecked());
        txtvStatus.setEnabled(chkAutoFlattr.isChecked());

        final int initialValue = (int) (UserPreferences.getAutoFlattrPlayedDurationThreshold() * 100.0f);
        setStatusMsgText(activity, txtvStatus, initialValue);
        skbPercent.setProgress(initialValue);

        chkAutoFlattr.setOnClickListener(v -> {
            skbPercent.setEnabled(chkAutoFlattr.isChecked());
            txtvStatus.setEnabled(chkAutoFlattr.isChecked());
        });

        skbPercent.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setStatusMsgText(activity, txtvStatus, progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        builder.setTitle(R.string.pref_auto_flattr_title)
                .setView(view)
                .setPositiveButton(R.string.confirm_label, (dialog, which) -> {
                    float progDouble = ((float) skbPercent.getProgress()) / 100.0f;
                    callback.onConfirmed(chkAutoFlattr.isChecked(), progDouble);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel_label, (dialog, which) -> {
                    callback.onCancelled();
                    dialog.dismiss();
                })
                .setCancelable(false).show();
    }

    private static void setStatusMsgText(Context context, TextView txtvStatus, int progress) {
        if (progress == 0) {
            txtvStatus.setText(R.string.auto_flattr_ater_beginning);
        } else if (progress == 100) {
            txtvStatus.setText(R.string.auto_flattr_ater_end);
        } else {
            txtvStatus.setText(context.getString(R.string.auto_flattr_after_percent, progress));
        }
    }

    public interface AutoFlattrPreferenceDialogInterface {
        void onCancelled();

        void onConfirmed(boolean autoFlattrEnabled, float autoFlattrValue);
    }


}
