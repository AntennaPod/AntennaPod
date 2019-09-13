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
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;

public class AutoDownloadPreferencesFragment extends PreferenceFragmentCompat {
    private static final String TAG = "AutoDnldPrefFragment";
    private CheckBoxPreference[] selectedNetworks;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_autodownload);

        setupAutoDownloadScreen();
        buildAutodownloadSelectedNetworksPreference();
        setSelectedNetworksEnabled(UserPreferences.isEnableAutodownloadWifiFilter());
        buildEpisodeCleanupPreference();
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

                UserPreferences.setAutodownloadSelectedNetworks(
                        prefValuesList.toArray(new String[prefValuesList.size()])
                );
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
        if (permissionHelper.showPermissionRequestPromptOnAndroid10IfNeeded(b)) {
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
        permissionHelper.doOnRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private class PermissionHelper {
        private static final String requestedPermission = Manifest.permission.ACCESS_COARSE_LOCATION;
        private static final int permissionRequestCode = 1;

        private static final String PREF_KEY_PERMISSION_REQUEST_PROMPT = "prefAutoDownloadWifiFilterAndroid10PermissionPrompt";

        private Preference prefPermissionRequestPromptOnAndroid10 = null;

        void doOnRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            if (requestCode != permissionRequestCode) {
                return;
            }
            if (permissions.length > 0 && permissions[0].equals(requestedPermission) &&
                    grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                buildAutodownloadSelectedNetworksPreference();
            }
        }

        boolean showPermissionRequestPromptOnAndroid10IfNeeded(boolean wifiFilterEnabled) {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                return false;
            }

            // Cases Android 10(Q) or later

            final PreferenceScreen prefScreen = getPreferenceScreen();

            if (prefPermissionRequestPromptOnAndroid10 != null) {
                prefScreen.removePreference(prefPermissionRequestPromptOnAndroid10);
                prefPermissionRequestPromptOnAndroid10 = null;
            }


            if (hasLocationPermission()) {
                return false;
            }

            // Case location permission not yet granted, permission-specific UI is needed

            if (!wifiFilterEnabled) { // don't show the UI when WiFi filter disabled
                return true;
            }

            Preference pref = new Preference(requireActivity());
            pref.setKey(PREF_KEY_PERMISSION_REQUEST_PROMPT);
            pref.setTitle(R.string.autodl_wifi_filter_permission_title);
            pref.setSummary(R.string.autodl_wifi_filter_permission_message);
            pref.setOnPreferenceClickListener(preference -> {
                requestLocationPermission();
                return true;
            });
            pref.setPersistent(false);
            getPreferenceScreen().addPreference(pref);
            prefPermissionRequestPromptOnAndroid10 = pref;

            return true;
        }

        private boolean hasLocationPermission() {
            return ContextCompat.checkSelfPermission(requireContext(), requestedPermission) == PackageManager.PERMISSION_GRANTED;
        }

        private void requestLocationPermission() {
            requestPermissions(new String[]{requestedPermission}, permissionRequestCode);
        }
    }
    private final PermissionHelper permissionHelper = new PermissionHelper();
}
