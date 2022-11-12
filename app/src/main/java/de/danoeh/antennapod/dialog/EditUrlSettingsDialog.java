package de.danoeh.antennapod.dialog;

import android.content.Context;
import android.view.View;
import android.widget.EditText;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.Feed;

public abstract class EditUrlSettingsDialog extends MaterialAlertDialogBuilder {
    public static final String TAG = "EditUrlSettingsDialog";

    public EditUrlSettingsDialog(Context context, Feed feed) {
        super(context);
        setTitle(R.string.pref_edit_url_feed);
        View rootView = View.inflate(context, R.layout.edit_text_dialog, null);
        setView(rootView);

        EditText etxtDownloadUrl = rootView.findViewById(R.id.urlEditText);

        etxtDownloadUrl.setText(feed.getDownload_url());

        setNegativeButton(R.string.cancel_label, null);
        setPositiveButton(R.string.confirm_label, (dialog, which)
                -> {

            onConfirmed(feed.getDownload_url(), String.valueOf(etxtDownloadUrl.getText()));
        });
    }

    protected abstract void onConfirmed(String original, String updated);
}
