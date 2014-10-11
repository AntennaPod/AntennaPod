package de.danoeh.antennapod.core.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import de.danoeh.antennapod.core.R;

/** Creates Alert Dialogs if a DownloadRequestException has happened. */
public class DownloadRequestErrorDialogCreator {
	private DownloadRequestErrorDialogCreator() {
	}

	public static void newRequestErrorDialog(Context context,
			String errorMessage) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setNeutralButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				})
				.setTitle(R.string.download_error_request_error)
				.setMessage(
						context.getString(R.string.download_request_error_dialog_message_prefix)
								+ errorMessage);
		builder.create().show();
	}
}
