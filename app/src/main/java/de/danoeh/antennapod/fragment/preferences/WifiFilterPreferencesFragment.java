package de.danoeh.antennapod.fragment.preferences;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.PreferenceActivity;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.NetworkUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WifiFilterPreferencesFragment extends PreferenceFragmentCompat {
    private static final String TAG = "WifiFilterPrefFragment";
    private static final String PREFERENCE_ADD_NETWORK = "prefWifiFilterAddNetwork";

    private final ArrayList<Preference> filteredNetworkPreferenceItems = new ArrayList<>();

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_wifi_filter);

        setupClickActions();
        populateFilteredNetworksList();
        setWifiListItemsEnabled(UserPreferences.isEnableAutodownloadWifiFilter());
    }

    private void setupClickActions() {
        findPreference(UserPreferences.PREF_ENABLE_AUTODL_WIFI_FILTER).setOnPreferenceChangeListener(
                (preference, newValue) -> {
                    if (newValue instanceof Boolean) {
                        setWifiListItemsEnabled((Boolean) newValue);
                        return true;
                    } else {
                        return false;
                    }
                });
        findPreference(PREFERENCE_ADD_NETWORK).setOnPreferenceClickListener(preference -> {
            displayAddNetworkDialog();
            return true;
        });
    }

    private void displayAddNetworkDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        dialog.setTitle(R.string.pref_wifi_filter_add_network);

        if (Build.VERSION.SDK_INT < 29) {
            String[] networks = getKnownNetworks();
            dialog.setItems(networks, (dialog1, which) -> addNetwork(networks[which]));
        } else {
            View view = View.inflate(getContext(), R.layout.edit_test_dialog_content, null);
            EditText text = view.findViewById(R.id.edit_text);
            text.setText(NetworkUtils.getWifiSsid());
            text.setInputType(InputType.TYPE_CLASS_TEXT);
            dialog.setView(view);
            dialog.setMessage(R.string.pref_wifi_filter_add_network_description);
            dialog.setPositiveButton(android.R.string.ok, (dialog12, which)
                    -> addNetwork(text.getText().toString().trim()));
        }
        dialog.setNegativeButton(R.string.cancel_label, null);
        dialog.show();
    }

    private void addNetwork(String ssid) {
        List<String> networks = UserPreferences.getAutodownloadSelectedNetworks();
        if (!networks.contains(ssid)) {
            networks.add(ssid);
            UserPreferences.setAutodownloadSelectedNetworks(networks);
            populateFilteredNetworksList();
        }
    }

    private void removeNetwork(String ssid) {
        List<String> networks = UserPreferences.getAutodownloadSelectedNetworks();
        if (networks.contains(ssid)) {
            networks.remove(ssid);
            UserPreferences.setAutodownloadSelectedNetworks(networks);
            populateFilteredNetworksList();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        ((PreferenceActivity) getActivity()).getSupportActionBar().setTitle(R.string.pref_autodl_wifi_filter_title);
    }

    /**
     * This method only works before Android 10.
     * @return The list of known wifi networks
     */
    private String[] getKnownNetworks() {
        WifiManager wifiService = (WifiManager) getActivity().getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        List<WifiConfiguration> wifiConfigurations = wifiService.getConfiguredNetworks();

        if (wifiConfigurations == null) {
            Log.e(TAG, "Couldn't get list of configure Wi-Fi networks");
            return new String[] { };
        }
        Collections.sort(wifiConfigurations, (x, y) -> blankIfNull(x.SSID).compareToIgnoreCase(blankIfNull(y.SSID)));
        String[] networks = new String[wifiConfigurations.size()];
        for (int i = 0; i < wifiConfigurations.size(); i++) {
            String ssid = NetworkUtils.stripQuotes(wifiConfigurations.get(i).SSID);
            networks[i] = ssid;
        }
        return networks;
    }

    private static String blankIfNull(String val) {
        return val == null ? "" : val;
    }

    private void populateFilteredNetworksList() {
        for (Preference network : filteredNetworkPreferenceItems) {
            getPreferenceScreen().removePreference(network);
        }
        filteredNetworkPreferenceItems.clear();
        List<String> allowedNetworks = UserPreferences.getAutodownloadSelectedNetworks();
        Collections.sort(allowedNetworks, String::compareToIgnoreCase);
        for (String network : allowedNetworks) {
            Preference pref = new Preference(getContext());
            pref.setTitle(network);
            pref.setKey(network);
            pref.setOnPreferenceClickListener(preference -> {
                removeNetwork(network);
                return true;
            });
            pref.setWidgetLayoutResource(R.layout.preference_wifi_network_widget);
            getPreferenceScreen().addPreference(pref);
            filteredNetworkPreferenceItems.add(pref);
        }
    }

    private void setWifiListItemsEnabled(boolean enabled) {
        findPreference(PREFERENCE_ADD_NETWORK).setEnabled(enabled);
        for (Preference p : filteredNetworkPreferenceItems) {
            p.setEnabled(enabled);
        }
    }
}
