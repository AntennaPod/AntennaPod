package de.danoeh.antennapod.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import de.danoeh.antennapod.R;

public class GetSpeedPlaybackPlugin {
	private GetSpeedPlaybackPlugin() {
	}

	public static void showDialog(final Context context) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.no_playback_plugin_title);
		builder.setMessage(R.string.no_playback_plugin_msg);
		builder.setNegativeButton(R.string.close_label, null);
		builder.setPositiveButton(R.string.download_plugin_label,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent playStoreIntent = new Intent(
								Intent.ACTION_VIEW,
								Uri.parse("market://details?id=com.falconware.prestissimo"));
						context.startActivity(playStoreIntent);
					}
				});
		builder.create().show();
	}
}
