package de.danoeh.antennapod.ui.screen.feed.preferences;

import android.app.Activity;
import android.os.StrictMode;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.github.kilianB.exception.SonosControllerException;
import com.github.kilianB.sonos.SonosDevice;
import com.github.kilianB.sonos.SonosDiscovery;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.event.PlayerErrorEvent;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.system.utils.SonosSystem;

/**
 * Shows the dialog that allows setting the Sonos device.
 */
public class SonosDevicePreferenceDialog {
    private static final String TAG = "SonosDevicePreferenceDialog";
    public static void showSonosPreference(Activity activity, TextView textView) {
        int checked = 0;
        Vector<String> choices = new Vector<String>();
        Vector<SonosDevice> finalDevices = new Vector<SonosDevice>();

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run () {
                StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.LAX);

                try {
                    List<SonosDevice> discovered_devices = SonosDiscovery.discover();
                    for (int i = 0; i < discovered_devices.size(); i++) {
                        SonosDevice dev = discovered_devices.get(i);
                        choices.add(dev.getDeviceName());
                        finalDevices.add(dev);
                    }
                } catch (IOException | SonosControllerException io) {
                    Log.d(TAG, "Sonos Playback Enabled and Sonos Device not Present or Assigned");
                    EventBus.getDefault().postSticky(new PlayerErrorEvent("Sonos Playback Enabled and Sonos Device not Present or Assigned"));
                    io.printStackTrace();
                }
            }
        });

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
        builder.setTitle("Select Sonos Device");
        String[] choices_arr = new String[choices.size()];
        choices.toArray(choices_arr);

        builder.setSingleChoiceItems(choices_arr, checked, (dialog, which) -> {
            int choice = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
            if (choice < 0 || choice >= choices.size()) {
                System.err.printf("Choice in showSonosPreference is out of bounds %d", choice);
            }
            else {
                final String sonosDeviceName = choices.get(choice);
                if(finalDevices != null) {
                    SonosDevice finalDevice = finalDevices.get(choice);
                    SonosSystem.selectedDevice = Optional.of(finalDevice);
                }
                UserPreferences.setSonosDevice(sonosDeviceName);
                if (textView != null) {
                    textView.setText(sonosDeviceName);
                }
                dialog.dismiss();
            }
        });

        builder.setNegativeButton(R.string.cancel_label, null);
        builder.show();
    }
}