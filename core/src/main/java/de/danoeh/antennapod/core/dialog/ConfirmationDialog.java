package de.danoeh.antennapod.core.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

import de.danoeh.antennapod.core.R;

/**
 * Creates an AlertDialog which asks the user to confirm something. Other
 * classes can handle events like confirmation or cancellation.
 */
public abstract class ConfirmationDialog {

	private static final String TAG = ConfirmationDialog.class.getSimpleName();

	protected Context context;
	int titleId;
	int messageId;

    int positiveText;
    int negativeText;

	public ConfirmationDialog(Context context, int titleId, int messageId) {
		this.context = context;
		this.titleId = titleId;
		this.messageId = messageId;
	}

	public void onCancelButtonPressed(DialogInterface dialog) {
		Log.d(TAG, "Dialog was cancelled");
		dialog.dismiss();
	}

    public void setPositiveText(int id) {
        this.positiveText = id;
    }

    public void setNegativeText(int id) {
        this.negativeText = id;
    }


	public abstract void onConfirmButtonPressed(DialogInterface dialog);

	public final AlertDialog createNewDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(titleId);
		builder.setMessage(messageId);
		builder.setPositiveButton(positiveText != 0 ? positiveText : R.string.confirm_label,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						onConfirmButtonPressed(dialog);
					}
				});
		builder.setNegativeButton(negativeText != 0 ? negativeText : R.string.cancel_label,
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
