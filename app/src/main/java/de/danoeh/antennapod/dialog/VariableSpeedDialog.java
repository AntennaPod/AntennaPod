package de.danoeh.antennapod.dialog;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;

import java.util.Arrays;
import java.util.List;

public class VariableSpeedDialog {
	private VariableSpeedDialog() {
	}

	public static void showDialog(final Context context) {
		if (com.aocate.media.MediaPlayer.isPrestoLibraryInstalled(context)) {
			showSpeedSelectorDialog(context);
		} else {
			showGetPluginDialog(context);
		}
	}

	private static void showGetPluginDialog(final Context context) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.no_playback_plugin_title);
		builder.setMessage(R.string.no_playback_plugin_msg);
		builder.setNegativeButton(R.string.close_label, null);
		builder.setPositiveButton(R.string.download_plugin_label,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
                        try {
						    Intent playStoreIntent = new Intent(
								    Intent.ACTION_VIEW,
							    	Uri.parse("market://details?id=com.falconware.prestissimo"));
						    context.startActivity(playStoreIntent);
                        } catch (ActivityNotFoundException e) {
                            // this is usually thrown on an emulator if the Android market is not installed
                            e.printStackTrace();
                        }
					}
				});
		builder.create().show();
	}

	private static void showSpeedSelectorDialog(final Context context) {
		final String[] speedValues = context.getResources().getStringArray(
				R.array.playback_speed_values);
		// According to Java spec these get initialized to false on creation
		final boolean[] speedChecked = new boolean[speedValues.length];

		// Build the "isChecked" array so that multiChoice dialog is
		// populated correctly
		List<String> selectedSpeedList = Arrays.asList(UserPreferences
				.getPlaybackSpeedArray());
		for (int i = 0; i < speedValues.length; i++) {
			speedChecked[i] = selectedSpeedList.contains(speedValues[i]);
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.set_playback_speed_label);
		builder.setMultiChoiceItems(R.array.playback_speed_values,
				speedChecked, new DialogInterface.OnMultiChoiceClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which,
							boolean isChecked) {
						speedChecked[which] = isChecked;
					}

				});
		builder.setNegativeButton(android.R.string.cancel, null);
		builder.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						int choiceCount = 0;
						for (int i = 0; i < speedChecked.length; i++) {
							if (speedChecked[i]) {
								choiceCount++;
							}
						}
						String[] newSpeedValues = new String[choiceCount];
						int newSpeedIndex = 0;
						for (int i = 0; i < speedChecked.length; i++) {
							if (speedChecked[i]) {
								newSpeedValues[newSpeedIndex++] = speedValues[i];
							}
						}

						UserPreferences.setPlaybackSpeedArray(newSpeedValues);

					}
				});
		builder.create().show();
	}
}
