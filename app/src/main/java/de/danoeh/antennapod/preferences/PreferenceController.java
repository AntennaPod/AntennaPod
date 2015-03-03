package de.danoeh.antennapod.preferences;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.AboutActivity;
import de.danoeh.antennapod.activity.DirectoryChooserActivity;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.activity.PreferenceActivity;
import de.danoeh.antennapod.activity.PreferenceActivityGingerbread;
import de.danoeh.antennapod.asynctask.OpmlExportWorker;
import de.danoeh.antennapod.core.asynctask.FlattrClickWorker;
import de.danoeh.antennapod.core.preferences.GpodnetPreferences;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.flattr.FlattrStatus;
import de.danoeh.antennapod.core.util.flattr.FlattrUtils;
import de.danoeh.antennapod.core.util.flattr.SimpleFlattrThing;
import de.danoeh.antennapod.dialog.AuthenticationDialog;
import de.danoeh.antennapod.dialog.AutoFlattrPreferenceDialog;
import de.danoeh.antennapod.dialog.GpodnetSetHostnameDialog;
import de.danoeh.antennapod.dialog.VariableSpeedDialog;

/**
 * Sets up a preference UI that lets the user change user preferences.
 */
public class PreferenceController {
    private static final String TAG = "PreferenceController";
    public static final String PREF_FLATTR_THIS_APP = "prefFlattrThisApp";
    public static final String PREF_FLATTR_SETTINGS = "prefFlattrSettings";
    public static final String PREF_FLATTR_AUTH = "pref_flattr_authenticate";
    public static final String PREF_FLATTR_REVOKE = "prefRevokeAccess";
    public static final String PREF_AUTO_FLATTR_PREFS = "prefAutoFlattrPrefs";
    public static final String PREF_OPML_EXPORT = "prefOpmlExport";
    public static final String PREF_ABOUT = "prefAbout";
    public static final String PREF_CHOOSE_DATA_DIR = "prefChooseDataDir";
    public static final String AUTO_DL_PREF_SCREEN = "prefAutoDownloadSettings";
    public static final String PREF_PLAYBACK_SPEED_LAUNCHER = "prefPlaybackSpeedLauncher";
    public static final String PREF_GPODNET_LOGIN = "pref_gpodnet_authenticate";
    public static final String PREF_GPODNET_SETLOGIN_INFORMATION = "pref_gpodnet_setlogin_information";
    public static final String PREF_GPODNET_LOGOUT = "pref_gpodnet_logout";
    public static final String PREF_GPODNET_HOSTNAME = "pref_gpodnet_hostname";
    public static final String PREF_EXPANDED_NOTIFICATION = "prefExpandNotify";
    private static final String PREF_PERSISTENT_NOTIFICATION = "prefPersistNotify";


    private final PreferenceUI ui;

    private CheckBoxPreference[] selectedNetworks;

    public PreferenceController(PreferenceUI ui) {
        this.ui = ui;
    }

    /**
     * Returns the preference activity that should be used on this device.
     *
     * @return PreferenceActivity if the API level is greater than 10, PreferenceActivityGingerbread otherwise.
     */
    public static Class getPreferenceActivity() {
        if (Build.VERSION.SDK_INT > 10) {
            return PreferenceActivity.class;
        } else {
            return PreferenceActivityGingerbread.class;
        }
    }

    public void onCreate() {
        final Activity activity = ui.getActivity();

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            // disable expanded notification option on unsupported android versions
            ui.findPreference(PreferenceController.PREF_EXPANDED_NOTIFICATION).setEnabled(false);
            ui.findPreference(PreferenceController.PREF_EXPANDED_NOTIFICATION).setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener() {

                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            Toast toast = Toast.makeText(activity, R.string.pref_expand_notify_unsupport_toast, Toast.LENGTH_SHORT);
                            toast.show();
                            return true;
                        }
                    }
            );
        }

        ui.findPreference(PreferenceController.PREF_FLATTR_THIS_APP).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {

                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        new FlattrClickWorker(activity,
                                new SimpleFlattrThing(activity.getString(R.string.app_name),
                                        FlattrUtils.APP_URL,
                                        new FlattrStatus(FlattrStatus.STATUS_QUEUE)
                                )
                        ).executeAsync();

                        return true;
                    }
                }
        );

        ui.findPreference(PreferenceController.PREF_FLATTR_REVOKE).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {

                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        FlattrUtils.revokeAccessToken(activity);
                        checkItemVisibility();
                        return true;
                    }

                }
        );

        ui.findPreference(PreferenceController.PREF_ABOUT).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {

                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        activity.startActivity(new Intent(
                                activity, AboutActivity.class));
                        return true;
                    }

                }
        );

        ui.findPreference(PreferenceController.PREF_OPML_EXPORT).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {

                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        new OpmlExportWorker(activity)
                                .executeAsync();

                        return true;
                    }
                }
        );

        ui.findPreference(PreferenceController.PREF_CHOOSE_DATA_DIR).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {

                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        activity.startActivityForResult(
                                new Intent(activity,
                                        DirectoryChooserActivity.class),
                                DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED
                        );
                        return true;
                    }
                }
        );
        ui.findPreference(UserPreferences.PREF_THEME)
                .setOnPreferenceChangeListener(
                        new Preference.OnPreferenceChangeListener() {

                            @Override
                            public boolean onPreferenceChange(
                                    Preference preference, Object newValue) {
                                Intent i = new Intent(activity, MainActivity.class);
                                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        | Intent.FLAG_ACTIVITY_NEW_TASK);
                                activity.finish();
                                activity.startActivity(i);
                                return true;
                            }
                        }
                );
        ui.findPreference(UserPreferences.PREF_ENABLE_AUTODL)
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        if (newValue instanceof Boolean) {
                            ui.findPreference(UserPreferences.PREF_ENABLE_AUTODL_WIFI_FILTER).setEnabled((Boolean) newValue);
                            setSelectedNetworksEnabled((Boolean) newValue && UserPreferences.isEnableAutodownloadWifiFilter());
                            ui.findPreference(UserPreferences.PREF_ENABLE_AUTODL_ON_BATTERY).setEnabled((Boolean) newValue);
                        }
                        return true;
                    }
                });
        ui.findPreference(UserPreferences.PREF_ENABLE_AUTODL_WIFI_FILTER)
                .setOnPreferenceChangeListener(
                        new Preference.OnPreferenceChangeListener() {

                            @Override
                            public boolean onPreferenceChange(
                                    Preference preference, Object newValue) {
                                if (newValue instanceof Boolean) {
                                    setSelectedNetworksEnabled((Boolean) newValue);
                                    return true;
                                } else {
                                    return false;
                                }
                            }
                        }
                );
        ui.findPreference(UserPreferences.PREF_EPISODE_CACHE_SIZE)
                .setOnPreferenceChangeListener(
                        new Preference.OnPreferenceChangeListener() {
                            @Override
                            public boolean onPreferenceChange(Preference preference, Object o) {
                                if (o instanceof String) {
                                    setEpisodeCacheSizeText(UserPreferences.readEpisodeCacheSize((String) o));
                                }
                                return true;
                            }
                        }
                );
        ui.findPreference(PreferenceController.PREF_PLAYBACK_SPEED_LAUNCHER)
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        VariableSpeedDialog.showDialog(activity);
                        return true;
                    }
                });
        ui.findPreference(PreferenceController.PREF_GPODNET_SETLOGIN_INFORMATION).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AuthenticationDialog dialog = new AuthenticationDialog(activity,
                        R.string.pref_gpodnet_setlogin_information_title, false, false, GpodnetPreferences.getUsername(),
                        null) {

                    @Override
                    protected void onConfirmed(String username, String password, boolean saveUsernamePassword) {
                        GpodnetPreferences.setPassword(password);
                    }
                };
                dialog.show();
                return true;
            }
        });
        ui.findPreference(PreferenceController.PREF_GPODNET_LOGOUT).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                GpodnetPreferences.logout();
                Toast toast = Toast.makeText(activity, R.string.pref_gpodnet_logout_toast, Toast.LENGTH_SHORT);
                toast.show();
                updateGpodnetPreferenceScreen();
                return true;
            }
        });
        ui.findPreference(PreferenceController.PREF_GPODNET_HOSTNAME).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                GpodnetSetHostnameDialog.createDialog(activity).setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        updateGpodnetPreferenceScreen();
                    }
                });
                return true;
            }
        });

        ui.findPreference(PreferenceController.PREF_AUTO_FLATTR_PREFS).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AutoFlattrPreferenceDialog.newAutoFlattrPreferenceDialog(activity,
                        new AutoFlattrPreferenceDialog.AutoFlattrPreferenceDialogInterface() {
                            @Override
                            public void onCancelled() {

                            }

                            @Override
                            public void onConfirmed(boolean autoFlattrEnabled, float autoFlattrValue) {
                                UserPreferences.setAutoFlattrSettings(activity, autoFlattrEnabled, autoFlattrValue);
                                checkItemVisibility();
                            }
                        });
                return true;
            }
        });
        buildUpdateIntervalPreference();
        buildAutodownloadSelectedNetworsPreference();
        setSelectedNetworksEnabled(UserPreferences
                .isEnableAutodownloadWifiFilter());
    }

    public void onResume() {
        checkItemVisibility();
        setEpisodeCacheSizeText(UserPreferences.getEpisodeCacheSize());
        setDataFolderText();
        updateGpodnetPreferenceScreen();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED) {
            String dir = data
                    .getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR);
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Setting data folder");
            UserPreferences.setDataFolder(dir);
        }
    }

    private void updateGpodnetPreferenceScreen() {
        final boolean loggedIn = GpodnetPreferences.loggedIn();
        ui.findPreference(PreferenceController.PREF_GPODNET_LOGIN).setEnabled(!loggedIn);
        ui.findPreference(PreferenceController.PREF_GPODNET_SETLOGIN_INFORMATION).setEnabled(loggedIn);
        ui.findPreference(PreferenceController.PREF_GPODNET_LOGOUT).setEnabled(loggedIn);
        ui.findPreference(PreferenceController.PREF_GPODNET_HOSTNAME).setSummary(GpodnetPreferences.getHostname());
    }

    private void buildUpdateIntervalPreference() {
        final Resources res = ui.getActivity().getResources();

        ListPreference pref = (ListPreference) ui.findPreference(UserPreferences.PREF_UPDATE_INTERVAL);
        String[] values = res.getStringArray(
                R.array.update_intervall_values);
        String[] entries = new String[values.length];
        for (int x = 0; x < values.length; x++) {
            Integer v = Integer.parseInt(values[x]);
            switch (v) {
                case 0:
                    entries[x] = res.getString(R.string.pref_update_interval_hours_manual);
                    break;
                case 1:
                    entries[x] = v
                            + " "
                            + res.getString(R.string.pref_update_interval_hours_singular);
                    break;
                default:
                    entries[x] = v + " "
                            + res.getString(R.string.pref_update_interval_hours_plural);
                    break;

            }
        }
        pref.setEntries(entries);

    }

    private void setSelectedNetworksEnabled(boolean b) {
        if (selectedNetworks != null) {
            for (Preference p : selectedNetworks) {
                p.setEnabled(b);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void checkItemVisibility() {

        boolean hasFlattrToken = FlattrUtils.hasToken();

        ui.findPreference(PreferenceController.PREF_FLATTR_SETTINGS).setEnabled(FlattrUtils.hasAPICredentials());
        ui.findPreference(PreferenceController.PREF_FLATTR_AUTH).setEnabled(!hasFlattrToken);
        ui.findPreference(PreferenceController.PREF_FLATTR_REVOKE).setEnabled(hasFlattrToken);
        ui.findPreference(PreferenceController.PREF_AUTO_FLATTR_PREFS).setEnabled(hasFlattrToken);

        ui.findPreference(UserPreferences.PREF_ENABLE_AUTODL_WIFI_FILTER)
                .setEnabled(UserPreferences.isEnableAutodownload());
        setSelectedNetworksEnabled(UserPreferences.isEnableAutodownload()
                && UserPreferences.isEnableAutodownloadWifiFilter());

        ui.findPreference(UserPreferences.PREF_ENABLE_AUTODL_ON_BATTERY)
                .setEnabled(UserPreferences.isEnableAutodownload());
    }

    private void setEpisodeCacheSizeText(int cacheSize) {
        final Resources res = ui.getActivity().getResources();

        String s;
        if (cacheSize == res.getInteger(
                R.integer.episode_cache_size_unlimited)) {
            s = res.getString(R.string.pref_episode_cache_unlimited);
        } else {
            s = Integer.toString(cacheSize)
                    + res.getString(R.string.episodes_suffix);
        }
        ui.findPreference(UserPreferences.PREF_EPISODE_CACHE_SIZE).setSummary(s);
    }

    private void setDataFolderText() {
        File f = UserPreferences.getDataFolder(ui.getActivity(), null);
        if (f != null) {
            ui.findPreference(PreferenceController.PREF_CHOOSE_DATA_DIR)
                    .setSummary(f.getAbsolutePath());
        }
    }

    private void buildAutodownloadSelectedNetworsPreference() {
        final Activity activity = ui.getActivity();

        if (selectedNetworks != null) {
            clearAutodownloadSelectedNetworsPreference();
        }
        // get configured networks
        WifiManager wifiservice = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
        List<WifiConfiguration> networks = wifiservice.getConfiguredNetworks();

        if (networks != null) {
            selectedNetworks = new CheckBoxPreference[networks.size()];
            List<String> prefValues = Arrays.asList(UserPreferences
                    .getAutodownloadSelectedNetworks());
            PreferenceScreen prefScreen = (PreferenceScreen) ui.findPreference(PreferenceController.AUTO_DL_PREF_SCREEN);
            Preference.OnPreferenceClickListener clickListener = new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (preference instanceof CheckBoxPreference) {
                        String key = preference.getKey();
                        ArrayList<String> prefValuesList = new ArrayList<String>(
                                Arrays.asList(UserPreferences
                                        .getAutodownloadSelectedNetworks())
                        );
                        boolean newValue = ((CheckBoxPreference) preference)
                                .isChecked();
                        if (BuildConfig.DEBUG)
                            Log.d(TAG, "Selected network " + key
                                    + ". New state: " + newValue);

                        int index = prefValuesList.indexOf(key);
                        if (index >= 0 && newValue == false) {
                            // remove network
                            prefValuesList.remove(index);
                        } else if (index < 0 && newValue == true) {
                            prefValuesList.add(key);
                        }

                        UserPreferences.setAutodownloadSelectedNetworks(
                                activity, prefValuesList
                                        .toArray(new String[prefValuesList
                                                .size()])
                        );
                        return true;
                    } else {
                        return false;
                    }
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
        } else {
            Log.e(TAG, "Couldn't get list of configure Wi-Fi networks");
        }
    }

    private void clearAutodownloadSelectedNetworsPreference() {
        if (selectedNetworks != null) {
            PreferenceScreen prefScreen = (PreferenceScreen) ui.findPreference(PreferenceController.AUTO_DL_PREF_SCREEN);

            for (int i = 0; i < selectedNetworks.length; i++) {
                if (selectedNetworks[i] != null) {
                    prefScreen.removePreference(selectedNetworks[i]);
                }
            }
        }
    }


    public static interface PreferenceUI {

        /**
         * Finds a preference based on its key.
         */
        public Preference findPreference(CharSequence key);

        public Activity getActivity();
    }
}
