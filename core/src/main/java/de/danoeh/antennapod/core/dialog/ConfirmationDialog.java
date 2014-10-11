package de.danoeh.antennapod.core.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import de.danoeh.antennapod.core.BuildConfig;
import de.danoeh.antennapod.core.R;

/**
 * Creates an AlertDialog which asks the user to confirm something. Other
 * classes can handle events like confirmation or cancellation.
 */
public abstract class ConfirmationDialog {
	private static final String TAG = "ConfirmationDialog";

	Context context;
	int titleId;
	int messageId;

	public ConfirmationDialog(Context context, int titleId, int messageId) {
		this.context = context;
		this.titleId = titleId;
		this.messageId = messageId;
	}

	public void onCancelButtonPressed(DialogInterface dialog) {
		if (BuildConfig.DEBUG)
			Log.d(TAG, "Dialog was cancelled");
		dialog.dismiss();
	}

	public abstract void onConfirmButtonPressed(DialogInterface dialog);

	public final AlertDialog createNewDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(titleId);
		builder.setMessage(messageId);
		builder.setPositiveButton(R.string.confirm_label,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						onConfirmButtonPressed(dialog);
					}
				});
		builder.setNegativeButton(R.string.cancel_label,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						onCancelButtonPressed(dialog);
					}
				});
		builder.setOnCancelListener(new DialogInterface.OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				onCancelButtonPressed(dialog);
			}
		});
		return builder.create();
	}
}
