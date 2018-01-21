package de.danoeh.antennapod.preferences;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import de.danoeh.antennapod.activity.ImportExportActivity;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.CrashReportWriter;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.AboutActivity;
import de.danoeh.antennapod.activity.DirectoryChooserActivity;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.activity.MediaplayerActivity;
import de.danoeh.antennapod.activity.StatisticsActivity;
import de.danoeh.antennapod.asynctask.ExportWorker;
import de.danoeh.antennapod.core.export.ExportWriter;
import de.danoeh.antennapod.core.export.html.HtmlWriter;
import de.danoeh.antennapod.core.export.opml.OpmlWriter;
import de.danoeh.antennapod.core.preferences.GpodnetPreferences;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.GpodnetSyncService;
import de.danoeh.antennapod.core.util.flattr.FlattrUtils;
import de.danoeh.antennapod.dialog.AuthenticationDialog;
import de.danoeh.antennapod.dialog.AutoFlattrPreferenceDialog;
import de.danoeh.antennapod.dialog.ChooseDataFolderDialog;
import de.danoeh.antennapod.dialog.GpodnetSetHostnameDialog;
import de.danoeh.antennapod.dialog.ProxyDialog;
import de.danoeh.antennapod.dialog.VariableSpeedDialog;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Sets up a preference UI that lets the user change user preferences.
 */

public class PreferenceController implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "PreferenceController";

    private static final String PREF_FLATTR_SETTINGS = "prefFlattrSettings";
    private static final String PREF_FLATTR_AUTH = "pref_flattr_authenticate";
    private static final String PREF_FLATTR_REVOKE = "prefRevokeAccess";
    private static final String PREF_AUTO_FLATTR_PREFS = "prefAutoFlattrPrefs";
    private static final String PREF_OPML_EXPORT = "prefOpmlExport";
    private static final String PREF_HTML_EXPORT = "prefHtmlExport";
    private static final String STATISTICS = "statistics";
    private static final String IMPORT_EXPORT = "importExport";
    private static final String PREF_ABOUT = "prefAbout";
    private static final String PREF_CHOOSE_DATA_DIR = "prefChooseDataDir";
    private static final String AUTO_DL_PREF_SCREEN = "prefAutoDownloadSettings";
    private static final String PREF_PLAYBACK_SPEED_LAUNCHER = "prefPlaybackSpeedLauncher";
    private static final String PREF_PLAYBACK_REWIND_DELTA_LAUNCHER = "prefPlaybackRewindDeltaLauncher";
    private static final String PREF_PLAYBACK_FAST_FORWARD_DELTA_LAUNCHER = "prefPlaybackFastForwardDeltaLauncher";
    private static final String PREF_GPODNET_LOGIN = "pref_gpodnet_authenticate";
    private static final String PREF_GPODNET_SETLOGIN_INFORMATION = "pref_gpodnet_setlogin_information";
    private static final String PREF_GPODNET_SYNC = "pref_gpodnet_sync";
    private static final String PREF_GPODNET_FORCE_FULL_SYNC = "pref_gpodnet_force_full_sync";
    private static final String PREF_GPODNET_LOGOUT = "pref_gpodnet_logout";
    private static final String PREF_GPODNET_HOSTNAME = "pref_gpodnet_hostname";
    private static final String PREF_GPODNET_NOTIFICATIONS = "pref_gpodnet_notifications";
    private static final String PREF_EXPANDED_NOTIFICATION = "prefExpandNotify";
    private static final String PREF_PROXY = "prefProxy";
    private static final String PREF_KNOWN_ISSUES = "prefKnownIssues";
    private static final String PREF_FAQ = "prefFaq";
    private static final String PREF_SEND_CRASH_REPORT = "prefSendCrashReport";
    private static final String[] EXTERNAL_STORAGE_PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE };
    private static final int PERMISSION_REQUEST_EXTERNAL_STORAGE = 41;
    private final PreferenceUI ui;
    private final SharedPreferences.OnSharedPreferenceChangeListener gpoddernetListener =
            (sharedPreferences, key) -> {
                if (GpodnetPreferences.PREF_LAST_SYNC_ATTEMPT_TIMESTAMP.equals(key)) {
                    updateLastGpodnetSyncReport(GpodnetPreferences.getLastSyncAttemptResult(),
                            GpodnetPreferences.getLastSyncAttemptTimestamp());
                }
            };
    private CheckBoxPreference[] selectedNetworks;
    private Subscription subscription;

    public PreferenceController(PreferenceUI ui) {
        this.ui = ui;
        PreferenceManager.getDefaultSharedPreferences(ui.getActivity().getApplicationContext())
            .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(UserPreferences.PREF_SONIC)) {
            CheckBoxPreference prefSonic = (CheckBoxPreference) ui.findPreference(UserPreferences.PREF_SONIC);
            if(prefSonic != null) {
                prefSonic.setChecked(sharedPreferences.getBoolean(UserPreferences.PREF_SONIC, false));
            }
        }
    }

    public void onCreate() {
        final Activity activity = ui.getActivity();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            // disable expanded notification option on unsupported android versions
            ui.findPreference(PreferenceController.PREF_EXPANDED_NOTIFICATION).setEnabled(false);
            ui.findPreference(PreferenceController.PREF_EXPANDED_NOTIFICATION).setOnPreferenceClickListener(
                    preference -> {
                        Toast toast = Toast.makeText(activity,
                                R.string.pref_expand_notify_unsupport_toast, Toast.LENGTH_SHORT);
                        toast.show();
                        return true;
                    }
            );
        }
        ui.findPreference(PreferenceController.PREF_FLATTR_REVOKE).setOnPreferenceClickListener(
                preference -> {
                    FlattrUtils.revokeAccessToken(activity);
                    checkItemVisibility();
                    return true;
                }
        );
        ui.findPreference(PreferenceController.PREF_ABOUT).setOnPreferenceClickListener(
                preference -> {
                    activity.startActivity(new Intent(activity, AboutActivity.class));
                    return true;
                }
        );
        ui.findPreference(PreferenceController.STATISTICS).setOnPreferenceClickListener(
                preference -> {
                    activity.startActivity(new Intent(activity, StatisticsActivity.class));
                    return true;
                }
        );
        ui.findPreference(PreferenceController.IMPORT_EXPORT).setOnPreferenceClickListener(
                preference -> {
                    activity.startActivity(new Intent(activity, ImportExportActivity.class));
                    return true;
                }
        );
        ui.findPreference(PreferenceController.PREF_OPML_EXPORT).setOnPreferenceClickListener(
                preference -> export(new OpmlWriter()));
        ui.findPreference(PreferenceController.PREF_HTML_EXPORT).setOnPreferenceClickListener(
                preference -> export(new HtmlWriter()));
        ui.findPreference(PreferenceController.PREF_CHOOSE_DATA_DIR).setOnPreferenceClickListener(
                preference -> {
                    if (Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT &&
                            Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
                        showChooseDataFolderDialog();
                    } else {
                        int readPermission = ActivityCompat.checkSelfPermission(
                                activity, Manifest.permission.READ_EXTERNAL_STORAGE);
                        int writePermission = ActivityCompat.checkSelfPermission(
                                activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        if (readPermission == PackageManager.PERMISSION_GRANTED &&
                                writePermission == PackageManager.PERMISSION_GRANTED) {
                            openDirectoryChooser();
                        } else {
                            requestPermission();
                        }
                    }
                    return true;
                }
        );
        ui.findPreference(PreferenceController.PREF_CHOOSE_DATA_DIR)
                .setOnPreferenceClickListener(
                        preference -> {
                            if (Build.VERSION.SDK_INT >= 19) {
                                showChooseDataFolderDialog();
                            } else {
                                Intent intent = new Intent(activity, DirectoryChooserActivity.class);
                                activity.startActivityForResult(intent,
                                        DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED);
                            }
                            return true;
                        }
                );
        ui.findPreference(UserPreferences.PREF_THEME)
                .setOnPreferenceChangeListener(
                        (preference, newValue) -> {
                            Intent i = new Intent(activity, MainActivity.class);
                            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    | Intent.FLAG_ACTIVITY_NEW_TASK);
                            activity.finish();
                            activity.startActivity(i);
                            return true;
                        }
                );
        ui.findPreference(UserPreferences.PREF_HIDDEN_DRAWER_ITEMS)
                .setOnPreferenceClickListener(preference -> {
                    showDrawerPreferencesDialog();
                    return true;
                });

        ui.findPreference(UserPreferences.PREF_COMPACT_NOTIFICATION_BUTTONS)
                .setOnPreferenceClickListener(preference -> {
                    showNotificationButtonsDialog();
                    return true;
                });

        ui.findPreference(UserPreferences.PREF_UPDATE_INTERVAL)
                .setOnPreferenceClickListener(preference -> {
                    showUpdateIntervalTimePreferencesDialog();
                    return true;
                });

        ui.findPreference(UserPreferences.PREF_ENABLE_AUTODL).setOnPreferenceChangeListener(
                (preference, newValue) -> {
                    if (newValue instanceof Boolean) {
                        boolean enabled = (Boolean) newValue;
                        ui.findPreference(UserPreferences.PREF_EPISODE_CACHE_SIZE).setEnabled(enabled);
                        ui.findPreference(UserPreferences.PREF_ENABLE_AUTODL_ON_BATTERY).setEnabled(enabled);
                        ui.findPreference(UserPreferences.PREF_ENABLE_AUTODL_WIFI_FILTER).setEnabled(enabled);
                        setSelectedNetworksEnabled(enabled && UserPreferences.isEnableAutodownloadWifiFilter());
                    }
                    return true;
                });
        ui.findPreference(UserPreferences.PREF_ENABLE_AUTODL_WIFI_FILTER)
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
        ui.findPreference(UserPreferences.PREF_PARALLEL_DOWNLOADS)
                .setOnPreferenceChangeListener(
                        (preference, o) -> {
                            if (o instanceof String) {
                                try {
                                    int value = Integer.parseInt((String) o);
                                    if (1 <= value && value <= 50) {
                                        setParallelDownloadsText(value);
                                        return true;
                                    }
                                } catch (NumberFormatException e) {
                                    return false;
                                }
                            }
                            return false;
                        }
                );
        // validate and set correct value: number of downloads between 1 and 50 (inclusive)
        final EditText ev = ((EditTextPreference) ui.findPreference(UserPreferences.PREF_PARALLEL_DOWNLOADS)).getEditText();
        ev.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    try {
                        int value = Integer.parseInt(s.toString());
                        if (value <= 0) {
                            ev.setText("1");
                        } else if (value > 50) {
                            ev.setText("50");
                        }
                    } catch (NumberFormatException e) {
                        ev.setText("6");
                    }
                    ev.setSelection(ev.getText().length());
                }
            }
        });
        ui.findPreference(UserPreferences.PREF_EPISODE_CACHE_SIZE)
                .setOnPreferenceChangeListener(
                        (preference, o) -> {
                            if (o instanceof String) {
                                setEpisodeCacheSizeText(UserPreferences.readEpisodeCacheSize((String) o));
                            }
                            return true;
                        }
                );
        ui.findPreference(PreferenceController.PREF_PLAYBACK_SPEED_LAUNCHER)
                .setOnPreferenceClickListener(preference -> {
                    VariableSpeedDialog.showDialog(activity);
                    return true;
                });
        ui.findPreference(PreferenceController.PREF_PLAYBACK_REWIND_DELTA_LAUNCHER)
                .setOnPreferenceClickListener(preference -> {
                    MediaplayerActivity.showSkipPreference(activity, MediaplayerActivity.SkipDirection.SKIP_REWIND);
                    return true;
                });
        ui.findPreference(PreferenceController.PREF_PLAYBACK_FAST_FORWARD_DELTA_LAUNCHER)
                .setOnPreferenceClickListener(preference -> {
                    MediaplayerActivity.showSkipPreference(activity, MediaplayerActivity.SkipDirection.SKIP_FORWARD);
                    return true;
                });
        ui.findPreference(PreferenceController.PREF_GPODNET_SETLOGIN_INFORMATION)
                .setOnPreferenceClickListener(preference -> {
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
                });
        ui.findPreference(PreferenceController.PREF_GPODNET_SYNC).
                setOnPreferenceClickListener(preference -> {
                    GpodnetSyncService.sendSyncIntent(ui.getActivity().getApplicationContext());
                    Toast toast = Toast.makeText(ui.getActivity(), R.string.pref_gpodnet_sync_started,
                            Toast.LENGTH_SHORT);
                    toast.show();
                    return true;
                });
        ui.findPreference(PreferenceController.PREF_GPODNET_FORCE_FULL_SYNC).
                setOnPreferenceClickListener(preference -> {
                    GpodnetPreferences.setLastSubscriptionSyncTimestamp(0L);
                    GpodnetPreferences.setLastEpisodeActionsSyncTimestamp(0L);
                    GpodnetPreferences.setLastSyncAttempt(false, 0);
                    updateLastGpodnetSyncReport(false, 0);
                    GpodnetSyncService.sendSyncIntent(ui.getActivity().getApplicationContext());
                    Toast toast = Toast.makeText(ui.getActivity(), R.string.pref_gpodnet_sync_started,
                            Toast.LENGTH_SHORT);
                    toast.show();
                    return true;
                });
        ui.findPreference(PreferenceController.PREF_GPODNET_LOGOUT).setOnPreferenceClickListener(
                preference -> {
                    GpodnetPreferences.logout();
                    Toast toast = Toast.makeText(activity, R.string.pref_gpodnet_logout_toast, Toast.LENGTH_SHORT);
                    toast.show();
                    updateGpodnetPreferenceScreen();
                    return true;
                });
        ui.findPreference(PreferenceController.PREF_GPODNET_HOSTNAME).setOnPreferenceClickListener(
                preference -> {
                    GpodnetSetHostnameDialog.createDialog(activity).setOnDismissListener(dialog -> updateGpodnetPreferenceScreen());
                    return true;
                });

        ui.findPreference(PreferenceController.PREF_AUTO_FLATTR_PREFS)
                .setOnPreferenceClickListener(preference -> {
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
                });
        ui.findPreference(UserPreferences.PREF_IMAGE_CACHE_SIZE).setOnPreferenceChangeListener(
                (preference, o) -> {
                    if (o instanceof String) {
                        int newValue = Integer.parseInt((String) o) * 1024 * 1024;
                        if (newValue != UserPreferences.getImageCacheSize()) {
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
        );
        ui.findPreference(PREF_PROXY).setOnPreferenceClickListener(preference -> {
            ProxyDialog dialog = new ProxyDialog(ui.getActivity());
            dialog.createDialog().show();
            return true;
        });
        ui.findPreference(PREF_KNOWN_ISSUES).setOnPreferenceClickListener(preference -> {
            openInBrowser("https://github.com/AntennaPod/AntennaPod/labels/bug");
            return true;
        });
        ui.findPreference(PREF_FAQ).setOnPreferenceClickListener(preference -> {
            openInBrowser("http://antennapod.org/faq.html");
            return true;
        });
        ui.findPreference(PREF_SEND_CRASH_REPORT).setOnPreferenceClickListener(preference -> {
            Context context = ui.getActivity().getApplicationContext();
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("text/plain");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"Martin.Fietz@gmail.com"});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "AntennaPod Crash Report");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Please describe what you were doing when the app crashed");
            // the attachment
            Uri fileUri = FileProvider.getUriForFile(context, context.getString(R.string.provider_authority),
                    CrashReportWriter.getFile());
            emailIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            emailIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            String intentTitle = ui.getActivity().getString(R.string.send_email);
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(emailIntent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    context.grantUriPermission(packageName, fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            }
            ui.getActivity().startActivity(Intent.createChooser(emailIntent, intentTitle));
            return true;
        });
        PreferenceControllerFlavorHelper.setupFlavoredUI(ui);
        buildEpisodeCleanupPreference();
        buildSmartMarkAsPlayedPreference();
        buildAutodownloadSelectedNetworsPreference();
        setSelectedNetworksEnabled(UserPreferences.isEnableAutodownloadWifiFilter());
    }

    private boolean export(ExportWriter exportWriter) {
        Context context = ui.getActivity();
        final ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setMessage(context.getString(R.string.exporting_label));
        progressDialog.setIndeterminate(true);
        progressDialog.show();
        final AlertDialog.Builder alert = new AlertDialog.Builder(context)
                .setNeutralButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
        Observable<File> observable = new ExportWorker(exportWriter).exportObservable();
        subscription = observable.subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(output -> {
                    alert.setTitle(R.string.opml_export_success_title);
                    String message = context.getString(R.string.opml_export_success_sum) + output.toString();
                    alert.setMessage(message);
                    alert.setPositiveButton(R.string.send_label, (dialog, which) -> {
                        Uri fileUri = FileProvider.getUriForFile(context.getApplicationContext(),
                                "de.danoeh.antennapod.provider", output);
                        Intent sendIntent = new Intent(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_SUBJECT,
                                context.getResources().getText(R.string.opml_export_label));
                        sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                        sendIntent.setType("text/plain");
                        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                            List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(sendIntent, PackageManager.MATCH_DEFAULT_ONLY);
                            for (ResolveInfo resolveInfo : resInfoList) {
                                String packageName = resolveInfo.activityInfo.packageName;
                                context.grantUriPermission(packageName, fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            }
                        }
                        context.startActivity(Intent.createChooser(sendIntent,
                                context.getResources().getText(R.string.send_label)));
                    });
                    alert.create().show();
                }, error -> {
                    alert.setTitle(R.string.export_error_label);
                    alert.setMessage(error.getMessage());
                    alert.show();
                }, progressDialog::dismiss);
        return true;
    }

    private void openInBrowser(String url) {
        try {
            Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            ui.getActivity().startActivity(myIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(ui.getActivity(), R.string.pref_no_browser_found, Toast.LENGTH_LONG).show();
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public void onResume() {
        checkItemVisibility();
        setUpdateIntervalText();
        setParallelDownloadsText(UserPreferences.getParallelDownloads());
        setEpisodeCacheSizeText(UserPreferences.getEpisodeCacheSize());
        setDataFolderText();
        GpodnetPreferences.registerOnSharedPreferenceChangeListener(gpoddernetListener);
        updateGpodnetPreferenceScreen();
    }

    public void onPause() {
        GpodnetPreferences.unregisterOnSharedPreferenceChangeListener(gpoddernetListener);
    }

    public void onStop() {
        if(subscription != null) {
            subscription.unsubscribe();
        }
    }

    @SuppressLint("NewApi")
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK &&
                requestCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED) {
            String dir = data.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR);

            File path;
            if(dir != null) {
                path = new File(dir);
            } else {
                path = ui.getActivity().getExternalFilesDir(null);
            }
            String message = null;
            final Context context= ui.getActivity().getApplicationContext();
            if(!path.exists()) {
                message = String.format(context.getString(R.string.folder_does_not_exist_error), dir);
            } else if(!path.canRead()) {
                message = String.format(context.getString(R.string.folder_not_readable_error), dir);
            } else if(!path.canWrite()) {
                message = String.format(context.getString(R.string.folder_not_writable_error), dir);
            }

            if(message == null) {
                Log.d(TAG, "Setting data folder: " + dir);
                UserPreferences.setDataFolder(dir);
                setDataFolderText();
            } else {
                AlertDialog.Builder ab = new AlertDialog.Builder(ui.getActivity());
                ab.setMessage(message);
                ab.setPositiveButton(android.R.string.ok, null);
                ab.show();
            }
        }
    }


    private void updateGpodnetPreferenceScreen() {
        final boolean loggedIn = GpodnetPreferences.loggedIn();
        ui.findPreference(PreferenceController.PREF_GPODNET_LOGIN).setEnabled(!loggedIn);
        ui.findPreference(PreferenceController.PREF_GPODNET_SETLOGIN_INFORMATION).setEnabled(loggedIn);
        ui.findPreference(PreferenceController.PREF_GPODNET_SYNC).setEnabled(loggedIn);
        ui.findPreference(PreferenceController.PREF_GPODNET_FORCE_FULL_SYNC).setEnabled(loggedIn);
        ui.findPreference(PreferenceController.PREF_GPODNET_LOGOUT).setEnabled(loggedIn);
        ui.findPreference(PREF_GPODNET_NOTIFICATIONS).setEnabled(loggedIn);
        if(loggedIn) {
            String format = ui.getActivity().getString(R.string.pref_gpodnet_login_status);
            String summary = String.format(format, GpodnetPreferences.getUsername(),
                    GpodnetPreferences.getDeviceID());
            ui.findPreference(PreferenceController.PREF_GPODNET_LOGOUT).setSummary(Html.fromHtml(summary));
            updateLastGpodnetSyncReport(GpodnetPreferences.getLastSyncAttemptResult(),
                    GpodnetPreferences.getLastSyncAttemptTimestamp());
        } else {
            ui.findPreference(PreferenceController.PREF_GPODNET_LOGOUT).setSummary(null);
            updateLastGpodnetSyncReport(false, 0);
        }
        ui.findPreference(PreferenceController.PREF_GPODNET_HOSTNAME).setSummary(GpodnetPreferences.getHostname());
    }

    private void updateLastGpodnetSyncReport(boolean successful, long lastTime) {
        Preference sync = ui.findPreference(PREF_GPODNET_SYNC);
        if (lastTime != 0) {
            sync.setSummary(ui.getActivity().getString(R.string.pref_gpodnet_sync_changes_sum) + "\n" +
                    ui.getActivity().getString(R.string.pref_gpodnet_sync_sum_last_sync_line,
                            ui.getActivity().getString(successful ?
                                    R.string.gpodnetsync_pref_report_successful :
                                    R.string.gpodnetsync_pref_report_failed),
                            DateUtils.getRelativeDateTimeString(ui.getActivity(),
                                    lastTime,
                                    DateUtils.MINUTE_IN_MILLIS,
                                    DateUtils.WEEK_IN_MILLIS,
                                    DateUtils.FORMAT_SHOW_TIME)));
        } else {
            sync.setSummary(ui.getActivity().getString(R.string.pref_gpodnet_sync_changes_sum));
        }
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

    private void buildEpisodeCleanupPreference() {
        final Resources res = ui.getActivity().getResources();

        ListPreference pref = (ListPreference) ui.findPreference(UserPreferences.PREF_EPISODE_CLEANUP);
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
            } else {
                entries[x] = res.getQuantityString(R.plurals.episode_cleanup_days_after_listening, v, v);
            }
        }
        pref.setEntries(entries);
    }

    private void buildSmartMarkAsPlayedPreference() {
        final Resources res = ui.getActivity().getResources();

        ListPreference pref = (ListPreference) ui.findPreference(UserPreferences.PREF_SMART_MARK_AS_PLAYED_SECS);
        String[] values = res.getStringArray(R.array.smart_mark_as_played_values);
        String[] entries = new String[values.length];
        for (int x = 0; x < values.length; x++) {
            if(x == 0) {
                entries[x] = res.getString(R.string.pref_smart_mark_as_played_disabled);
            } else {
                Integer v = Integer.parseInt(values[x]);
                if(v < 60) {
                    entries[x] = res.getQuantityString(R.plurals.time_seconds_quantified, v, v);
                } else {
                    v /= 60;
                    entries[x] = res.getQuantityString(R.plurals.time_minutes_quantified, v, v);
                }
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

        boolean autoDownload = UserPreferences.isEnableAutodownload();
        ui.findPreference(UserPreferences.PREF_EPISODE_CACHE_SIZE).setEnabled(autoDownload);
        ui.findPreference(UserPreferences.PREF_ENABLE_AUTODL_ON_BATTERY).setEnabled(autoDownload);
        ui.findPreference(UserPreferences.PREF_ENABLE_AUTODL_WIFI_FILTER).setEnabled(autoDownload);
        setSelectedNetworksEnabled(autoDownload && UserPreferences.isEnableAutodownloadWifiFilter());

        ui.findPreference(PREF_SEND_CRASH_REPORT).setEnabled(CrashReportWriter.getFile().exists());

        if (Build.VERSION.SDK_INT >= 16) {
            ui.findPreference(UserPreferences.PREF_SONIC).setEnabled(true);
        } else {
            Preference prefSonic = ui.findPreference(UserPreferences.PREF_SONIC);
            prefSonic.setSummary("[Android 4.1+]\n" + prefSonic.getSummary());
        }
    }

    private void setUpdateIntervalText() {
        Context context = ui.getActivity().getApplicationContext();
        String val;
        long interval = UserPreferences.getUpdateInterval();
        if(interval > 0) {
            int hours = (int) TimeUnit.MILLISECONDS.toHours(interval);
            String hoursStr = context.getResources().getQuantityString(R.plurals.time_hours_quantified, hours, hours);
            val = String.format(context.getString(R.string.pref_autoUpdateIntervallOrTime_every), hoursStr);
        } else {
            int[] timeOfDay = UserPreferences.getUpdateTimeOfDay();
            if(timeOfDay.length == 2) {
                Calendar cal = new GregorianCalendar();
                cal.set(Calendar.HOUR_OF_DAY, timeOfDay[0]);
                cal.set(Calendar.MINUTE, timeOfDay[1]);
                String timeOfDayStr = DateFormat.getTimeFormat(context).format(cal.getTime());
                val = String.format(context.getString(R.string.pref_autoUpdateIntervallOrTime_at),
                        timeOfDayStr);
            } else {
                val = context.getString(R.string.pref_smart_mark_as_played_disabled);  // TODO: Is this a bug? Otherwise document why is this related to smart mark???
            }
        }
        String summary = context.getString(R.string.pref_autoUpdateIntervallOrTime_sum) + "\n"
                + String.format(context.getString(R.string.pref_current_value), val);
        ui.findPreference(UserPreferences.PREF_UPDATE_INTERVAL).setSummary(summary);
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
        File f = UserPreferences.getDataFolder(null);
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
        WifiManager wifiservice = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        List<WifiConfiguration> networks = wifiservice.getConfiguredNetworks();

        if (networks == null) {
            Log.e(TAG, "Couldn't get list of configure Wi-Fi networks");
            return;
        }
        Collections.sort(networks, (x, y) -> x.SSID.compareTo(y.SSID));
        selectedNetworks = new CheckBoxPreference[networks.size()];
        List<String> prefValues = Arrays.asList(UserPreferences
                .getAutodownloadSelectedNetworks());
        PreferenceScreen prefScreen = (PreferenceScreen) ui.findPreference(PreferenceController.AUTO_DL_PREF_SCREEN);
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
            PreferenceScreen prefScreen = (PreferenceScreen) ui.findPreference(PreferenceController.AUTO_DL_PREF_SCREEN);

            for (CheckBoxPreference network : selectedNetworks) {
                if (network != null) {
                    prefScreen.removePreference(network);
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
        builder.setMultiChoiceItems(navTitles, checked, (dialog, which, isChecked) -> {
            if (isChecked) {
                hiddenDrawerItems.remove(NAV_DRAWER_TAGS[which]);
            } else {
                hiddenDrawerItems.add(NAV_DRAWER_TAGS[which]);
            }
        });
        builder.setPositiveButton(R.string.confirm_label, (dialog, which) ->
            UserPreferences.setHiddenDrawerItems(hiddenDrawerItems));
        builder.setNegativeButton(R.string.cancel_label, null);
        builder.create().show();
    }

    private void showNotificationButtonsDialog() {
        final Context context = ui.getActivity();
        final List<Integer> preferredButtons = UserPreferences.getCompactNotificationButtons();
        final String[] allButtonNames = context.getResources().getStringArray(
                R.array.compact_notification_buttons_options);
        boolean[] checked = new boolean[allButtonNames.length]; // booleans default to false in java

        for(int i=0; i < checked.length; i++) {
            if(preferredButtons.contains(i)) {
                checked[i] = true;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(String.format(context.getResources().getString(
                R.string.pref_compact_notification_buttons_dialog_title), 2));
        builder.setMultiChoiceItems(allButtonNames, checked, (dialog, which, isChecked) -> {
            checked[which] = isChecked;

            if (isChecked) {
                if (preferredButtons.size() < 2) {
                    preferredButtons.add(which);
                } else {
                    // Only allow a maximum of two selections. This is because the notification
                    // on the lock screen can only display 3 buttons, and the play/pause button
                    // is always included.
                    checked[which] = false;
                    ListView selectionView = ((AlertDialog) dialog).getListView();
                    selectionView.setItemChecked(which, false);
                    Snackbar.make(
                            selectionView,
                            String.format(context.getResources().getString(
                                    R.string.pref_compact_notification_buttons_dialog_error), 2),
                            Snackbar.LENGTH_SHORT).show();
                }
            } else {
                preferredButtons.remove((Integer) which);
            }
        });
        builder.setPositiveButton(R.string.confirm_label, (dialog, which) ->
            UserPreferences.setCompactNotificationButtons(preferredButtons));
        builder.setNegativeButton(R.string.cancel_label, null);
        builder.create().show();
    }

    // CHOOSE DATA FOLDER

    private void requestPermission() {
        ActivityCompat.requestPermissions(ui.getActivity(), EXTERNAL_STORAGE_PERMISSIONS,
                PERMISSION_REQUEST_EXTERNAL_STORAGE);
    }

    private void openDirectoryChooser() {
        Activity activity = ui.getActivity();
        Intent intent = new Intent(activity, DirectoryChooserActivity.class);
        activity.startActivityForResult(intent, DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED);
    }

    private void showChooseDataFolderDialog() {
        ChooseDataFolderDialog.showDialog(
                ui.getActivity(), new ChooseDataFolderDialog.RunnableWithString() {
                    @Override
                    public void run(final String folder) {
                        UserPreferences.setDataFolder(folder);
                        setDataFolderText();
                    }
                });
    }

    // UPDATE TIME/INTERVAL DIALOG

    private void showUpdateIntervalTimePreferencesDialog() {
        final Context context = ui.getActivity();

        MaterialDialog.Builder builder = new MaterialDialog.Builder(context);
        builder.title(R.string.pref_autoUpdateIntervallOrTime_title);
        builder.content(R.string.pref_autoUpdateIntervallOrTime_message);
        builder.positiveText(R.string.pref_autoUpdateIntervallOrTime_Interval);
        builder.negativeText(R.string.pref_autoUpdateIntervallOrTime_TimeOfDay);
        builder.neutralText(R.string.pref_autoUpdateIntervallOrTime_Disable);
        builder.onPositive((dialog, which) -> {
            AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
            builder1.setTitle(context.getString(R.string.pref_autoUpdateIntervallOrTime_Interval));
            final String[] values = context.getResources().getStringArray(R.array.update_intervall_values);
            final String[] entries = getUpdateIntervalEntries(values);
            long currInterval = UserPreferences.getUpdateInterval();
            int checkedItem = -1;
            if(currInterval > 0) {
                String currIntervalStr = String.valueOf(TimeUnit.MILLISECONDS.toHours(currInterval));
                checkedItem = ArrayUtils.indexOf(values, currIntervalStr);
            }
            builder1.setSingleChoiceItems(entries, checkedItem, (dialog1, which1) -> {
                int hours = Integer.parseInt(values[which1]);
                UserPreferences.setUpdateInterval(hours);
                dialog1.dismiss();
                setUpdateIntervalText();
            });
            builder1.setNegativeButton(context.getString(R.string.cancel_label), null);
            builder1.show();
        });
        builder.onNegative((dialog, which) -> {
            int hourOfDay = 7, minute = 0;
            int[] updateTime = UserPreferences.getUpdateTimeOfDay();
            if (updateTime.length == 2) {
                hourOfDay = updateTime[0];
                minute = updateTime[1];
            }
            TimePickerDialog timePickerDialog = new TimePickerDialog(context,
                    (view, selectedHourOfDay, selectedMinute) -> {
                        if (view.getTag() == null) { // onTimeSet() may get called twice!
                            view.setTag("TAGGED");
                            UserPreferences.setUpdateTimeOfDay(selectedHourOfDay, selectedMinute);
                            setUpdateIntervalText();
                        }
                    }, hourOfDay, minute, DateFormat.is24HourFormat(context));
            timePickerDialog.setTitle(context.getString(R.string.pref_autoUpdateIntervallOrTime_TimeOfDay));
            timePickerDialog.show();
        });
        builder.onNeutral((dialog, which) -> {
            UserPreferences.setUpdateInterval(0);
            setUpdateIntervalText();
        });
        builder.show();
    }


    public interface PreferenceUI {

        /**
         * Finds a preference based on its key.
         */
        Preference findPreference(CharSequence key);

        Activity getActivity();
    }
}
