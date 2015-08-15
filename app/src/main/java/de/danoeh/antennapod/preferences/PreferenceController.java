package de.danoeh.antennapod.preferences;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.EditText;
import android.widget.TimePicker;
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
import de.danoeh.antennapod.core.preferences.GpodnetPreferences;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.flattr.FlattrUtils;
import de.danoeh.antennapod.dialog.AuthenticationDialog;
import de.danoeh.antennapod.dialog.AutoFlattrPreferenceDialog;
import de.danoeh.antennapod.dialog.GpodnetSetHostnameDialog;
import de.danoeh.antennapod.dialog.VariableSpeedDialog;

/**
 * Sets up a preference UI that lets the user change user preferences.
 */
public class PreferenceController {
    private static final String TAG = "PreferenceController";
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
        ui.findPreference(UserPreferences.PREF_HIDDEN_DRAWER_ITEMS)
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        showDrawerPreferencesDialog();
                        return true;
                    }
                });

        ui.findPreference(UserPreferences.PREF_UPDATE_INTERVAL)
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        showUpdateIntervalTimePreferencesDialog();
                        return true;
                    }
                });

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
        ui.findPreference(UserPreferences.PREF_PARALLEL_DOWNLOADS)
                .setOnPreferenceChangeListener(
                        new Preference.OnPreferenceChangeListener() {
                            @Override
                            public boolean onPreferenceChange(Preference preference, Object o) {
                                if (o instanceof String) {
                                    try {
                                        int value = Integer.valueOf((String) o);
                                        if (1 <= value && value <= 50) {
                                            setParallelDownloadsText(value);
                                            return true;
                                        }
                                    } catch(NumberFormatException e) {
                                        return false;
                                    }
                                }
                                return false;
                            }
                        }
                );
        // validate and set correct value: number of downloads between 1 and 50 (inclusive)
        final EditText ev = ((EditTextPreference)ui.findPreference(UserPreferences.PREF_PARALLEL_DOWNLOADS)).getEditText();
        ev.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if(s.length() > 0) {
                    try {
                        int value = Integer.valueOf(s.toString());
                        if (value <= 0) {
                            ev.setText("1");
                        } else if (value > 50) {
                            ev.setText("50");
                        }
                    } catch(NumberFormatException e) {
                        ev.setText("6");
                    }
                    ev.setSelection(ev.getText().length());
                }
            }
        });
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
                                UserPreferences.setAutoFlattrSettings(autoFlattrEnabled, autoFlattrValue);
                                checkItemVisibility();
                            }
                        });
                return true;
            }
        });
        ui.findPreference(UserPreferences.PREF_IMAGE_CACHE_SIZE)
                .setOnPreferenceChangeListener(
                        new Preference.OnPreferenceChangeListener() {
                            @Override
                            public boolean onPreferenceChange(Preference preference, Object o) {
                                if (o instanceof String) {
                                    int newValue = Integer.valueOf((String) o) * 1024 * 1024;
                                    if(newValue != UserPreferences.getImageCacheSize()) {
                                        AlertDialog.Builder dialog = new AlertDialog.Builder(ui.getActivity());
                                        dialog.setTitle(android.R.string.dialog_alert_title);
                                        dialog.setMessage(R.string.pref_restart_required);
                                        dialog.setPositiveButton(android.R.string.ok, null);
                                        dialog.show();
                                    }
                                    return true;
                                }
                                return false;
                            }
                        }
                );
        buildSmartMarkAsPlayedPreference();
        buildAutodownloadSelectedNetworsPreference();
        setSelectedNetworksEnabled(UserPreferences
                .isEnableAutodownloadWifiFilter());
    }

    public void onResume() {
        checkItemVisibility();
        setParallelDownloadsText(UserPreferences.getParallelDownloads());
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

    private String[] getUpdateIntervalEntries(final String[] values) {
        final Resources res = ui.getActivity().getResources();
        String[] entries = new String[values.length];
        for (int x = 0; x < values.length; x++) {
            Integer v = Integer.parseInt(values[x]);
            switch (v) {
                case 0:
                    entries[x] = res.getString(R.string.pref_update_interval_hours_manual);
                    break;
                case 1:
                    entries[x] = v + " " + res.getString(R.string.pref_update_interval_hours_singular);
                    break;
                default:
                    entries[x] = v + " " + res.getString(R.string.pref_update_interval_hours_plural);
                    break;

            }
        }
        return entries;
    }

    private void buildSmartMarkAsPlayedPreference() {
        final Resources res = ui.getActivity().getResources();

        ListPreference pref = (ListPreference) ui.findPreference(UserPreferences.PREF_SMART_MARK_AS_PLAYED_SECS);
        String[] values = res.getStringArray(
                R.array.smart_mark_as_played_values);
        String[] entries = new String[values.length];
        for (int x = 0; x < values.length; x++) {
            if(x == 0) {
                entries[x] = res.getString(R.string.pref_smart_mark_as_played_disabled);
            } else {
                Integer v = Integer.parseInt(values[x]);
                entries[x] = res.getQuantityString(R.plurals.time_seconds_quantified, v, v);
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

    private void setParallelDownloadsText(int downloads) {
        final Resources res = ui.getActivity().getResources();

        String s = Integer.toString(downloads)
                    + res.getString(R.string.parallel_downloads_suffix);
        ui.findPreference(UserPreferences.PREF_PARALLEL_DOWNLOADS).setSummary(s);
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
                                prefValuesList.toArray(new String[prefValuesList.size()])
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

    private void showDrawerPreferencesDialog() {
        final Context context = ui.getActivity();
        final List<String> hiddenDrawerItems = UserPreferences.getHiddenDrawerItems();
        final String[] navTitles = context.getResources().getStringArray(R.array.nav_drawer_titles);
        final String[] NAV_DRAWER_TAGS = MainActivity.NAV_DRAWER_TAGS;
        boolean[] checked = new boolean[MainActivity.NAV_DRAWER_TAGS.length];
        for(int i=0; i < NAV_DRAWER_TAGS.length; i++) {
            String tag = NAV_DRAWER_TAGS[i];
            if(!hiddenDrawerItems.contains(tag)) {
                checked[i] = true;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.drawer_preferences);
        builder.setMultiChoiceItems(navTitles, checked, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                if (isChecked) {
                    hiddenDrawerItems.remove(NAV_DRAWER_TAGS[which]);
                } else {
                    hiddenDrawerItems.add(NAV_DRAWER_TAGS[which]);
                }
            }
        });
        builder.setPositiveButton(R.string.confirm_label, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                UserPreferences.setHiddenDrawerItems(context, hiddenDrawerItems);
            }
        });
        builder.setNegativeButton(R.string.cancel_label, null);
        builder.create().show();
    }

    private void showUpdateIntervalTimePreferencesDialog() {
        final Context context = ui.getActivity();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.pref_autoUpdateIntervallOrTime_title);
        builder.setMessage(R.string.pref_autoUpdateIntervallOrTime_message);
        builder.setNegativeButton(R.string.pref_autoUpdateIntervallOrTime_Disable, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                UserPreferences.setUpdateInterval(0);
            }
        });
        builder.setNeutralButton(R.string.pref_autoUpdateIntervallOrTime_Interval, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(context.getString(R.string.pref_autoUpdateIntervallOrTime_Interval));
                final String[] values = context.getResources().getStringArray(R.array.update_intervall_values);
                final String[] entries = getUpdateIntervalEntries(values);
                builder.setSingleChoiceItems(entries, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int hours = Integer.valueOf(values[which]);
                        UserPreferences.setUpdateInterval(hours);
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton(context.getString(R.string.cancel_label), null);
                builder.show();
            }
        });
        builder.setPositiveButton(R.string.pref_autoUpdateIntervallOrTime_TimeOfDay, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int hourOfDay = 7, minute = 0;
                        int[] updateTime = UserPreferences.getUpdateTimeOfDay();
                        if (updateTime.length == 2) {
                            hourOfDay = updateTime[0];
                            minute = updateTime[1];
                        }
                        TimePickerDialog timePickerDialog = new TimePickerDialog(context, new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                if (view.getTag() == null) { // onTimeSet() may get called twice!
                                    view.setTag("TAGGED");
                                    UserPreferences.setUpdateTimeOfDay(hourOfDay, minute);
                                }
                            }
                        }, hourOfDay, minute, DateFormat.is24HourFormat(context));
                        timePickerDialog.setTitle(context.getString(R.string.pref_autoUpdateIntervallOrTime_TimeOfDay));
                        timePickerDialog.show();
                    }
                }

        );
        builder.show();
    }


    public static interface PreferenceUI {

        /**
         * Finds a preference based on its key.
         */
        public Preference findPreference(CharSequence key);

        public Activity getActivity();
    }
}
