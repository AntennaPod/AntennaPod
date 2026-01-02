package de.danoeh.antennapod.ui.screen.feed.preferences;

import android.app.Activity;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.github.kilianB.exception.SonosControllerException;
import com.github.kilianB.sonos.SonosDevice;
import com.github.kilianB.sonos.SonosDiscovery;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.system.utils.SonosSystem;

/**
 * Shows the dialog that allows setting the Sonos device.
 */
public class SonosDevicePreferenceDialog {
    public static void showSonosPreference(Activity activity, TextView textView) {
        int checked = 0;
        Vector<String> choices = new Vector<String>();
        List<SonosDevice> devices = null;

        try {
            devices = SonosDiscovery.discover();
            for (int i = 0; i < devices.size(); i++) {
                SonosDevice dev = devices.get(i);
                if(dev.isCoordinator()) {
                    choices.add(dev.getDeviceName());
                }
            }
        }
        catch(IOException | SonosControllerException io) {
            devices = null;
            io.printStackTrace();
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
        builder.setTitle("Select Sonos Device");
        String[] choices_arr = new String[choices.size()];
        choices.toArray(choices_arr);

        List<SonosDevice> finalDevices = devices;
        builder.setSingleChoiceItems(choices_arr, checked, (dialog, which) -> {
            int choice = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
            if (choice < 0 || choice >= choices.size()) {
                System.err.printf("Choice in showSonosPreference is out of bounds %d", choice);
            } else {
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