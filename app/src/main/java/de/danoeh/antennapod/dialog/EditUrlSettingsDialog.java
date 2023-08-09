package de.danoeh.antennapod.dialog;

import android.app.Activity;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.download.FeedUpdateManager;
import de.danoeh.antennapod.databinding.EditTextDialogBinding;
import de.danoeh.antennapod.model.feed.Feed;

import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public abstract class EditUrlSettingsDialog {
    public static final String TAG = "EditUrlSettingsDialog";
    private final WeakReference<Activity> activityRef;
    private final Feed feed;

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

        new MaterialAlertDialogBuilder(activity)
                .setView(binding.getRoot())
                .setTitle(R.string.edit_url_menu)
                .setPositiveButton(android.R.string.ok, (d, input) ->
                        showConfirmAlertDialog(String.valueOf(binding.urlEditText.getText())))
                .setNegativeButton(R.string.cancel_label, null)
                .show();
    }

    private void onConfirmed(String original, String updated) {
        try {
            DBWriter.updateFeedDownloadURL(original, updated).get();
            feed.setDownload_url(updated);
            FeedUpdateManager.runOnce(activityRef.get(), feed);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void showConfirmAlertDialog(String url) {
        Activity activity = activityRef.get();

        AlertDialog alertDialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.edit_url_menu)
                .setMessage(R.string.edit_url_confirmation_msg)
                .setPositiveButton(android.R.string.ok, (d, input) -> {
                    onConfirmed(feed.getDownload_url(), url);
                    setUrl(url);
                })
                .setNegativeButton(R.string.cancel_label, null)
                .show();
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

        new CountDownTimer(15000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(
                        String.format(Locale.getDefault(), "%s (%d)",
                                activity.getString(android.R.string.ok), millisUntilFinished / 1000 + 1));
            }

            @Override
            public void onFinish() {
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(android.R.string.ok);
            }
        }.start();
    }

    protected abstract void setUrl(String url);
}
