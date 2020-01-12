package de.danoeh.antennapod.fragment.preferences;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.PreferenceActivity;
import de.danoeh.antennapod.core.preferences.UserPreferences;

public class AutoDownloadPreferencesFragment extends PreferenceFragmentCompat {
    private static final String TAG = "AutoDnldPrefFragment";

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String PREF_KEY_LOCATION_PERMISSION_REQUEST_PROMPT = "prefAutoDownloadWifiFilterAndroid10PermissionPrompt";

    private CheckBoxPreference[] selectedNetworks;

    private Preference prefPermissionRequestPromptOnAndroid10 = null;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_autodownload);

        setupAutoDownloadScreen();
        buildAutodownloadSelectedNetworksPreference();
        setSelectedNetworksEnabled(UserPreferences.isEnableAutodownloadWifiFilter());
        buildEpisodeCleanupPreference();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((PreferenceActivity) getActivity()).getSupportActionBar().setTitle(R.string.pref_automatic_download_title);
    }

    @Override
    public void onResume() {
        super.onResume();
        checkAutodownloadItemVisibility(UserPreferences.isEnableAutodownload());
    }

    private void setupAutoDownloadScreen() {
        findPreference(UserPreferences.PREF_ENABLE_AUTODL).setOnPreferenceChangeListener(
                (preference, newValue) -> {
                    if (newValue instanceof Boolean) {
                        checkAutodownloadItemVisibility((Boolean) newValue);
                    }
                    return true;
                });
        findPreference(UserPreferences.PREF_ENABLE_AUTODL_WIFI_FILTER)
                .setOnPreferenceChangeListener(
                        (preference, newValue) -> {
                            if (newValue instanceof Boolean) {
                                setSelectedNetworksEnabled((Boolean) newValue);
                                return true;
                            } else {
                                return false;
                            }
                        }
                );
    }

    private void checkAutodownloadItemVisibility(boolean autoDownload) {
        findPreference(UserPreferences.PREF_EPISODE_CACHE_SIZE).setEnabled(autoDownload);
        findPreference(UserPreferences.PREF_ENABLE_AUTODL_ON_BATTERY).setEnabled(autoDownload);
        findPreference(UserPreferences.PREF_ENABLE_AUTODL_WIFI_FILTER).setEnabled(autoDownload);
        findPreference(UserPreferences.PREF_EPISODE_CLEANUP).setEnabled(autoDownload);
        setSelectedNetworksEnabled(autoDownload && UserPreferences.isEnableAutodownloadWifiFilter());
    }

    private static String blankIfNull(String val) {
        return val == null ? "" : val;
    }

    private void buildAutodownloadSelectedNetworksPreference() {
        final Activity activity = getActivity();

        if (selectedNetworks != null) {
            clearAutodownloadSelectedNetworsPreference();
        }
        // get configured networks
        WifiManager wifiservice = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        List<WifiConfiguration> networks = wifiservice.getConfiguredNetworks();

        if (networks == null) {
            Log.e(TAG, "Couldn't get list of configure Wi-Fi networks");
            return;
        }
        Collections.sort(networks, (x, y) ->
                blankIfNull(x.SSID).compareToIgnoreCase(blankIfNull(y.SSID)));
        selectedNetworks = new CheckBoxPreference[networks.size()];
        List<String> prefValues = Arrays.asList(UserPreferences
                .getAutodownloadSelectedNetworks());
        PreferenceScreen prefScreen = getPreferenceScreen();
        Preference.OnPreferenceClickListener clickListener = preference -> {
            if (preference instanceof CheckBoxPreference) {
                String key = preference.getKey();
                List<String> prefValuesList = new ArrayList<>(
                        Arrays.asList(UserPreferences
                                .getAutodownloadSelectedNetworks())
                );
                boolean newValue = ((CheckBoxPreference) preference)
                        .isChecked();
                Log.d(TAG, "Selected network " + key + ". New state: " + newValue);

                int index = prefValuesList.indexOf(key);
                if (index >= 0 && !newValue) {
                    // remove network
                    prefValuesList.remove(index);
                } else if (index < 0 && newValue) {
                    prefValuesList.add(key);
                }

                UserPreferences.setAutodownloadSelectedNetworks(prefValuesList.toArray(new String[0]));
                return true;
            } else {
                return false;
            }
        };
        // create preference for each known network. attach listener and set
        // value
        for (int i = 0; i < networks.size(); i++) {
            WifiConfiguration config = networks.get(i);

            CheckBoxPreference pref = new CheckBoxPreference(activity);
            String key = Integer.toString(config.networkId);
            pref.setTitle(config.SSID);
            pref.setKey(key);
            pref.setOnPreferenceClickListener(clickListener);
            pref.setPersistent(false);
            pref.setChecked(prefValues.contains(key));
            selectedNetworks[i] = pref;
            prefScreen.addPreference(pref);
        }
    }

    private void clearAutodownloadSelectedNetworsPreference() {
        if (selectedNetworks != null) {
            PreferenceScreen prefScreen = getPreferenceScreen();

            for (CheckBoxPreference network : selectedNetworks) {
                if (network != null) {
                    prefScreen.removePreference(network);
                }
            }
        }
    }

    private void buildEpisodeCleanupPreference() {
        final Resources res = getActivity().getResources();

        ListPreference pref = (ListPreference) findPreference(UserPreferences.PREF_EPISODE_CLEANUP);
        String[] values = res.getStringArray(
                R.array.episode_cleanup_values);
        String[] entries = new String[values.length];
        for (int x = 0; x < values.length; x++) {
            int v = Integer.parseInt(values[x]);
            if (v == UserPreferences.EPISODE_CLEANUP_QUEUE) {
                entries[x] = res.getString(R.string.episode_cleanup_queue_removal);
            } else if (v == UserPreferences.EPISODE_CLEANUP_NULL){
                entries[x] = res.getString(R.string.episode_cleanup_never);
            } else if (v == 0) {
                entries[x] = res.getString(R.string.episode_cleanup_after_listening);
            } else if (v > 0 && v < 24) {
                entries[x] = res.getQuantityString(R.plurals.episode_cleanup_hours_after_listening, v, v);
            } else {
                int numDays = v / 24; // assume underlying value will be NOT fraction of days, e.g., 36 (hours)
                entries[x] = res.getQuantityString(R.plurals.episode_cleanup_days_after_listening, numDays, numDays);
            }
        }
        pref.setEntries(entries);
    }

    private void setSelectedNetworksEnabled(boolean b) {
        if (showPermissionRequestPromptOnAndroid10IfNeeded(b)) {
            return;
        }

        if (selectedNetworks != null) {
            for (Preference p : selectedNetworks) {
                p.setEnabled(b);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }
        if (permissions.length > 0 && permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION) &&
                grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            buildAutodownloadSelectedNetworksPreference();
        }
    }

    private boolean showPermissionRequestPromptOnAndroid10IfNeeded(boolean wifiFilterEnabled) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            return false;
        }

        // Cases Android 10(Q) or later
        if (prefPermissionRequestPromptOnAndroid10 != null) {
            getPreferenceScreen().removePreference(prefPermissionRequestPromptOnAndroid10);
            prefPermissionRequestPromptOnAndroid10 = null;
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        // Case location permission not yet granted, permission-specific UI is needed
        if (!wifiFilterEnabled) {
            // Don't show the UI when WiFi filter disabled.
            // it still return true, so that the caller knows
            // it does not have required permission, and will not invoke codes that require so.
            return true;
        }

        Preference pref = new Preference(requireActivity());
        pref.setKey(PREF_KEY_LOCATION_PERMISSION_REQUEST_PROMPT);
        pref.setTitle(R.string.autodl_wifi_filter_permission_title);
        pref.setSummary(R.string.autodl_wifi_filter_permission_message);
        pref.setIcon(R.drawable.ic_warning_red);
        pref.setOnPreferenceClickListener(preference -> {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return true;
        });
        pref.setPersistent(false);
        getPreferenceScreen().addPreference(pref);
        prefPermissionRequestPromptOnAndroid10 = pref;
        return true;
    }

}
