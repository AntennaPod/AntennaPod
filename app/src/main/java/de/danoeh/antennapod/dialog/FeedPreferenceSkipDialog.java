package de.danoeh.antennapod.dialog;

import android.content.Context;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import de.danoeh.antennapod.R;

/**
 * Displays a dialog with a username and password text field and an optional checkbox to save username and preferences.
 */
public abstract class FeedPreferenceSkipDialog extends AlertDialog.Builder {

    public FeedPreferenceSkipDialog(Context context, int skipIntroInitialValue,
                                int skipEndInitialValue) {
        super(context);
        setTitle(R.string.pref_feed_skip);
        View rootView = View.inflate(context, R.layout.feed_pref_skip_dialog, null);
        setView(rootView);

        final EditText etxtSkipIntro = rootView.findViewById(R.id.etxtSkipIntro);
        final EditText etxtSkipEnd = rootView.findViewById(R.id.etxtSkipEnd);

        etxtSkipIntro.setText(String.valueOf(skipIntroInitialValue));
        etxtSkipEnd.setText(String.valueOf(skipEndInitialValue));

        setNegativeButton(R.string.cancel_label, null);
        setPositiveButton(R.string.confirm_label, (dialog, which)
                -> onConfirmed(etxtSkipIntro.getText().toString(),
                               etxtSkipEnd.getText().toString()));
    }

    protected abstract void onConfirmed(String skipIntro, String skipEnd);
}
