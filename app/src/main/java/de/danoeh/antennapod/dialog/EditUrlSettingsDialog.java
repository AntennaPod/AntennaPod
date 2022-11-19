package de.danoeh.antennapod.dialog;

import android.app.Activity;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;


import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.lang.ref.WeakReference;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.databinding.EditTextDialogBinding;
import de.danoeh.antennapod.model.feed.Feed;

public abstract class EditUrlSettingsDialog {
    public static final String TAG = "EditUrlSettingsDialog";
    private final WeakReference<Activity> activityRef;
    private Feed feed;

    public EditUrlSettingsDialog(Activity activity, Feed feed) {
        this.activityRef = new WeakReference<>(activity);
        this.feed = feed;

    }

    public void show() {
        Activity activity = activityRef.get();
        if (activity == null) {
            return;
        }

        final EditTextDialogBinding binding = EditTextDialogBinding.inflate(LayoutInflater.from(activity));

        binding.urlEditText.setText(feed.getDownload_url());

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setView(binding.getRoot())
                .setTitle(R.string.edit_url_feed)
                .setPositiveButton(android.R.string.ok, (d, input) -> {
                    showConfirmAlertDialog(String.valueOf(binding.urlEditText.getText()));
                })
                .setNeutralButton(de.danoeh.antennapod.core.R.string.reset, null)
                .setNegativeButton(R.string.cancel_label, null)
                .show();

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(
                (view) -> binding.urlEditText.setText(feed.getDownload_url()));
    }

    private void onConfirmed(String original, String updated) {
        DBWriter.updateFeedDownloadURL(original, updated);
        feed.setDownload_url(updated);
    }

    private void showConfirmAlertDialog(String url) {
        Activity activity = activityRef.get();

        AlertDialog alertDialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.edit_url_menu)
                .setMessage(R.string.edit_url_confirmation_msg)
                .setPositiveButton(R.string.cancel_label, (d, input) -> {
                    onConfirmed(feed.getDownload_url(), url);
                    setUrl(url);
                })

                // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton(de.danoeh.antennapod.core.R.string.cancel_label, null)
                .show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.INVISIBLE);

        new CountDownTimer(10000, 1000) {

            public void onTick(long millisUntilFinished) {
                alertDialog.setMessage(activity.getString(R.string.edit_url_confirmation_msg,
                        Long.toString(millisUntilFinished / 1000)));
            }

            public void onFinish() {
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.VISIBLE);
            }
        }.start();
    }

    protected abstract void setUrl(String url);
}