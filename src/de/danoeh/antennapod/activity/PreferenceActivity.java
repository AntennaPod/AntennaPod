package de.danoeh.antennapod.activity;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources.Theme;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.asynctask.FlattrClickWorker;
import de.danoeh.antennapod.asynctask.OpmlExportWorker;
import de.danoeh.antennapod.dialog.AuthenticationDialog;
import de.danoeh.antennapod.dialog.GpodnetSetHostnameDialog;
import de.danoeh.antennapod.dialog.VariableSpeedDialog;
import de.danoeh.antennapod.preferences.GpodnetPreferences;
import de.danoeh.antennapod.preferences.UserPreferences;
import de.danoeh.antennapod.util.flattr.FlattrUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The main preference activity
 */
public class PreferenceActivity extends android.preference.PreferenceActivity {
    private static final String TAG = "PreferenceActivity";

    private static final String PREF_FLATTR_THIS_APP = "prefFlattrThisApp";
    private static final String PREF_FLATTR_AUTH = "pref_flattr_authenticate";
    private static final String PREF_FLATTR_REVOKE = "prefRevokeAccess";
    private static final String PREF_OPML_EXPORT = "prefOpmlExport";
    private static final String PREF_ABOUT = "prefAbout";
    private static final String PREF_CHOOSE_DATA_DIR = "prefChooseDataDir";
    private static final String AUTO_DL_PREF_SCREEN = "prefAutoDownloadSettings";
    private static final String PREF_PLAYBACK_SPEED_LAUNCHER = "prefPlaybackSpeedLauncher";

    private static final String PREF_GPODNET_LOGIN = "pref_gpodnet_authenticate";
    private static final String PREF_GPODNET_SETLOGIN_INFORMATION = "pref_gpodnet_setlogin_information";
    private static final String PREF_GPODNET_LOGOUT = "pref_gpodnet_logout";
    private static final String PREF_GPODNET_HOSTNAME = "pref_gpodnet_hostname";

    private CheckBoxPreference[] selectedNetworks;

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);

        if (android.os.Build.VERSION.SDK_INT >= 11) {
            ActionBar ab = getActionBar();
            if (ab != null) {
                ab.setDisplayHomeAsUpEnabled(true);
            }
        }

        addPreferencesFromResource(R.xml.preferences);
        findPreference(PREF_FLATTR_THIS_APP).setOnPreferenceClickListener(
                new OnPreferenceClickListener() {

                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        new FlattrClickWorker(PreferenceActivity.this,
                                FlattrUtils.APP_URL).executeAsync();

                        return true;
                    }
                });

        findPreference(PREF_FLATTR_REVOKE).setOnPreferenceClickListener(
                new OnPreferenceClickListener() {

                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        FlattrUtils.revokeAccessToken(PreferenceActivity.this);
                        checkItemVisibility();
                        return true;
                    }

                });

        findPreference(PREF_ABOUT).setOnPreferenceClickListener(
                new OnPreferenceClickListener() {

                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        PreferenceActivity.this.startActivity(new Intent(
                                PreferenceActivity.this, AboutActivity.class));
                        return true;
                    }

                });

        findPreference(PREF_OPML_EXPORT).setOnPreferenceClickListener(
                new OnPreferenceClickListener() {

                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        new OpmlExportWorker(PreferenceActivity.this)
                                .executeAsync();

                        return true;
                    }
                });

        findPreference(PREF_CHOOSE_DATA_DIR).setOnPreferenceClickListener(
                new OnPreferenceClickListener() {

                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        startActivityForResult(
                                new Intent(PreferenceActivity.this,
                                        DirectoryChooserActivity.class),
                                DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED);
                        return true;
                    }
                });
        findPreference(UserPreferences.PREF_THEME)
                .setOnPreferenceChangeListener(
                        new OnPreferenceChangeListener() {

                            @Override
                            public boolean onPreferenceChange(
                                    Preference preference, Object newValue) {
                                Intent i = getIntent();
                                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        | Intent.FLAG_ACTIVITY_NEW_TASK);
                                finish();
                                startActivity(i);
                                return true;
                            }
                        });
        findPreference(UserPreferences.PREF_ENABLE_AUTODL_WIFI_FILTER)
                .setOnPreferenceChangeListener(
                        new OnPreferenceChangeListener() {

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
                        });
        findPreference(UserPreferences.PREF_EPISODE_CACHE_SIZE)
                .setOnPreferenceChangeListener(
                        new OnPreferenceChangeListener() {


                            @Override
                            public boolean onPreferenceChange(Preference preference, Object o) {
                                checkItemVisibility();
                                return true;
                            }
                        });
        findPreference(PREF_PLAYBACK_SPEED_LAUNCHER)
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        VariableSpeedDialog.showDialog(PreferenceActivity.this);
                        return true;
                    }
                });
        findPreference(PREF_GPODNET_SETLOGIN_INFORMATION).setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AuthenticationDialog dialog = new AuthenticationDialog(PreferenceActivity.this,
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
        findPreference(PREF_GPODNET_LOGOUT).setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                GpodnetPreferences.logout();
                Toast toast = Toast.makeText(PreferenceActivity.this, R.string.pref_gpodnet_logout_toast, Toast.LENGTH_SHORT);
                toast.show();
                updateGpodnetPreferenceScreen();
                return true;
            }
        });
        findPreference(PREF_GPODNET_HOSTNAME).setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                GpodnetSetHostnameDialog.createDialog(PreferenceActivity.this).setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        updateGpodnetPreferenceScreen();
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

    private void updateGpodnetPreferenceScreen() {
        final boolean loggedIn = GpodnetPreferences.loggedIn();
        findPreference(PREF_GPODNET_LOGIN).setEnabled(!loggedIn);
        findPreference(PREF_GPODNET_SETLOGIN_INFORMATION).setEnabled(loggedIn);
        findPreference(PREF_GPODNET_LOGOUT).setEnabled(loggedIn);
        findPreference(PREF_GPODNET_HOSTNAME).setSummary(GpodnetPreferences.getHostname());
    }

    private void buildUpdateIntervalPreference() {
        ListPreference pref = (ListPreference) findPreference(UserPreferences.PREF_UPDATE_INTERVAL);
        String[] values = getResources().getStringArray(
                R.array.update_intervall_values);
        String[] entries = new String[values.length];
        for (int x = 0; x < values.length; x++) {
            Integer v = Integer.parseInt(values[x]);
            switch (v) {
                case 0:
                    entries[x] = getString(R.string.pref_update_interval_hours_manual);
                    break;
                case 1:
                    entries[x] = v
                            + " "
                            + getString(R.string.pref_update_interval_hours_singular);
                    break;
                default:
                    entries[x] = v + " "
                            + getString(R.string.pref_update_interval_hours_plural);
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

    @Override
    protected void onResume() {
        super.onResume();
        checkItemVisibility();
        setEpisodeCacheSizeText(UserPreferences.getEpisodeCacheSize());
        setDataFolderText();
        updateGpodnetPreferenceScreen();
    }

    @SuppressWarnings("deprecation")
    private void checkItemVisibility() {

        boolean hasFlattrToken = FlattrUtils.hasToken();

        findPreference(PREF_FLATTR_AUTH).setEnabled(!hasFlattrToken);
        findPreference(PREF_FLATTR_REVOKE).setEnabled(hasFlattrToken);

        findPreference(UserPreferences.PREF_ENABLE_AUTODL_WIFI_FILTER)
                .setEnabled(UserPreferences.isEnableAutodownload());
        setSelectedNetworksEnabled(UserPreferences.isEnableAutodownload()
                && UserPreferences.isEnableAutodownloadWifiFilter());

    }

    private void setEpisodeCacheSizeText(int cacheSize) {
        String s;
        if (cacheSize == getResources().getInteger(
                R.integer.episode_cache_size_unlimited)) {
            s = getString(R.string.pref_episode_cache_unlimited);
        } else {
            s = Integer.toString(cacheSize)
                    + getString(R.string.episodes_suffix);
        }
        findPreference(UserPreferences.PREF_EPISODE_CACHE_SIZE).setSummary(s);
    }

    private void setDataFolderText() {
        File f = UserPreferences.getDataFolder(this, null);
        if (f != null) {
            findPreference(PREF_CHOOSE_DATA_DIR)
                    .setSummary(f.getAbsolutePath());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    protected void onApplyThemeResource(Theme theme, int resid, boolean first) {
        theme.applyStyle(UserPreferences.getTheme(), true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED) {
            String dir = data
                    .getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR);
            if (AppConfig.DEBUG)
                Log.d(TAG, "Setting data folder");
            UserPreferences.setDataFolder(dir);
        }
    }

    private void buildAutodownloadSelectedNetworsPreference() {
        if (selectedNetworks != null) {
            clearAutodownloadSelectedNetworsPreference();
        }
        // get configured networks
        WifiManager wifiservice = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        List<WifiConfiguration> networks = wifiservice.getConfiguredNetworks();

        if (networks != null) {
            selectedNetworks = new CheckBoxPreference[networks.size()];
            List<String> prefValues = Arrays.asList(UserPreferences
                    .getAutodownloadSelectedNetworks());
            PreferenceScreen prefScreen = (PreferenceScreen) findPreference(AUTO_DL_PREF_SCREEN);
            OnPreferenceClickListener clickListener = new OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (preference instanceof CheckBoxPreference) {
                        String key = preference.getKey();
                        ArrayList<String> prefValuesList = new ArrayList<String>(
                                Arrays.asList(UserPreferences
                                        .getAutodownloadSelectedNetworks()));
                        boolean newValue = ((CheckBoxPreference) preference)
                                .isChecked();
                        if (AppConfig.DEBUG)
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
                                PreferenceActivity.this, prefValuesList
                                .toArray(new String[prefValuesList
                                        .size()]));
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

                CheckBoxPreference pref = new CheckBoxPreference(this);
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
            PreferenceScreen prefScreen = (PreferenceScreen) findPreference(AUTO_DL_PREF_SCREEN);

            for (int i = 0; i < selectedNetworks.length; i++) {
                if (selectedNetworks[i] != null) {
                    prefScreen.removePreference(selectedNetworks[i]);
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         Preference preference) {
        super.onPreferenceTreeClick(preferenceScreen, preference);
        if (preference != null)
            if (preference instanceof PreferenceScreen)
                if (((PreferenceScreen) preference).getDialog() != null)
                    ((PreferenceScreen) preference)
                            .getDialog()
                            .getWindow()
                            .getDecorView()
                            .setBackgroundDrawable(
                                    this.getWindow().getDecorView()
                                            .getBackground().getConstantState()
                                            .newDrawable());
        return false;
    }
}
