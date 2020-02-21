package de.danoeh.antennapod.fragment.preferences;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import android.text.Html;
import android.text.format.DateUtils;
import android.widget.Toast;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.PreferenceActivity;
import de.danoeh.antennapod.core.preferences.GpodnetPreferences;
import de.danoeh.antennapod.core.service.GpodnetSyncService;
import de.danoeh.antennapod.dialog.AuthenticationDialog;
import de.danoeh.antennapod.dialog.GpodnetSetHostnameDialog;

public class GpodderPreferencesFragment extends PreferenceFragmentCompat {
    private static final String PREF_GPODNET_LOGIN = "pref_gpodnet_authenticate";
    private static final String PREF_GPODNET_SETLOGIN_INFORMATION = "pref_gpodnet_setlogin_information";
    private static final String PREF_GPODNET_SYNC = "pref_gpodnet_sync";
    private static final String PREF_GPODNET_FORCE_FULL_SYNC = "pref_gpodnet_force_full_sync";
    private static final String PREF_GPODNET_LOGOUT = "pref_gpodnet_logout";
    private static final String PREF_GPODNET_HOSTNAME = "pref_gpodnet_hostname";
    private static final String PREF_GPODNET_NOTIFICATIONS = "pref_gpodnet_notifications";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_gpodder);
        setupGpodderScreen();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((PreferenceActivity) getActivity()).getSupportActionBar().setTitle(R.string.gpodnet_main_label);
    }

    @Override
    public void onResume() {
        super.onResume();
        GpodnetPreferences.registerOnSharedPreferenceChangeListener(gpoddernetListener);
        updateGpodnetPreferenceScreen();
    }

    @Override
    public void onPause() {
        super.onPause();
        GpodnetPreferences.unregisterOnSharedPreferenceChangeListener(gpoddernetListener);
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener gpoddernetListener =
            (sharedPreferences, key) -> {
                if (GpodnetPreferences.PREF_LAST_SYNC_ATTEMPT_TIMESTAMP.equals(key)) {
                    updateLastGpodnetSyncReport(GpodnetPreferences.getLastSyncAttemptResult(),
                            GpodnetPreferences.getLastSyncAttemptTimestamp());
                }
            };

    private void setupGpodderScreen() {
        final Activity activity = getActivity();

        findPreference(PREF_GPODNET_SETLOGIN_INFORMATION)
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
        findPreference(PREF_GPODNET_SYNC).setOnPreferenceClickListener(preference -> {
                    GpodnetSyncService.sendSyncIntent(getActivity().getApplicationContext());
                    Toast toast = Toast.makeText(getActivity(), R.string.pref_gpodnet_sync_started,
                            Toast.LENGTH_SHORT);
                    toast.show();
                    return true;
                });
        findPreference(PREF_GPODNET_FORCE_FULL_SYNC).setOnPreferenceClickListener(preference -> {
                    GpodnetPreferences.setLastSubscriptionSyncTimestamp(0L);
                    GpodnetPreferences.setLastEpisodeActionsSyncTimestamp(0L);
                    GpodnetPreferences.setLastSyncAttempt(false, 0);
                    updateLastGpodnetSyncReport(false, 0);
                    GpodnetSyncService.sendSyncIntent(getActivity().getApplicationContext());
                    Toast toast = Toast.makeText(getActivity(), R.string.pref_gpodnet_sync_started,
                            Toast.LENGTH_SHORT);
                    toast.show();
                    return true;
                });
        findPreference(PREF_GPODNET_LOGOUT).setOnPreferenceClickListener(preference -> {
                    GpodnetPreferences.logout();
                    Toast toast = Toast.makeText(activity, R.string.pref_gpodnet_logout_toast, Toast.LENGTH_SHORT);
                    toast.show();
                    updateGpodnetPreferenceScreen();
                    return true;
                });
        findPreference(PREF_GPODNET_HOSTNAME).setOnPreferenceClickListener(preference -> {
                    GpodnetSetHostnameDialog.createDialog(activity).setOnDismissListener(
                            dialog -> updateGpodnetPreferenceScreen());
                    return true;
                });
    }

    private void updateGpodnetPreferenceScreen() {
        final boolean loggedIn = GpodnetPreferences.loggedIn();
        findPreference(PREF_GPODNET_LOGIN).setEnabled(!loggedIn);
        findPreference(PREF_GPODNET_SETLOGIN_INFORMATION).setEnabled(loggedIn);
        findPreference(PREF_GPODNET_SYNC).setEnabled(loggedIn);
        findPreference(PREF_GPODNET_FORCE_FULL_SYNC).setEnabled(loggedIn);
        findPreference(PREF_GPODNET_LOGOUT).setEnabled(loggedIn);
        findPreference(PREF_GPODNET_NOTIFICATIONS).setEnabled(loggedIn);
        if(loggedIn) {
            String format = getActivity().getString(R.string.pref_gpodnet_login_status);
            String summary = String.format(format, GpodnetPreferences.getUsername(),
                    GpodnetPreferences.getDeviceID());
            findPreference(PREF_GPODNET_LOGOUT).setSummary(Html.fromHtml(summary));
            updateLastGpodnetSyncReport(GpodnetPreferences.getLastSyncAttemptResult(),
                    GpodnetPreferences.getLastSyncAttemptTimestamp());
        } else {
            findPreference(PREF_GPODNET_LOGOUT).setSummary(null);
            updateLastGpodnetSyncReport(false, 0);
        }
        findPreference(PREF_GPODNET_HOSTNAME).setSummary(GpodnetPreferences.getHostname());
    }

    private void updateLastGpodnetSyncReport(boolean successful, long lastTime) {
        Preference sync = findPreference(PREF_GPODNET_SYNC);
        if (lastTime != 0) {
            sync.setSummary(getActivity().getString(R.string.pref_gpodnet_sync_changes_sum) + "\n" +
                    getActivity().getString(R.string.pref_gpodnet_sync_sum_last_sync_line,
                            getActivity().getString(successful ?
                                    R.string.gpodnetsync_pref_report_successful :
                                    R.string.gpodnetsync_pref_report_failed),
                            DateUtils.getRelativeDateTimeString(getActivity(),
                                    lastTime,
                                    DateUtils.MINUTE_IN_MILLIS,
                                    DateUtils.WEEK_IN_MILLIS,
                                    DateUtils.FORMAT_SHOW_TIME)));
        } else {
            sync.setSummary(getActivity().getString(R.string.pref_gpodnet_sync_changes_sum));
        }
    }
}
