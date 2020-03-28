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
import de.danoeh.antennapod.core.event.SyncServiceEvent;
import de.danoeh.antennapod.core.preferences.GpodnetPreferences;
import de.danoeh.antennapod.core.sync.SyncService;
import de.danoeh.antennapod.dialog.AuthenticationDialog;
import de.danoeh.antennapod.dialog.GpodnetSetHostnameDialog;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


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
        updateGpodnetPreferenceScreen();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        ((PreferenceActivity) getActivity()).getSupportActionBar().setSubtitle("");
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void syncStatusChanged(SyncServiceEvent event) {
        if (!GpodnetPreferences.loggedIn()) {
            return;
        }
        if (event.getMessageResId() == R.string.sync_status_error
                || event.getMessageResId() == R.string.sync_status_success) {
            updateLastGpodnetSyncReport(SyncService.isLastSyncSuccessful(getContext()),
                    SyncService.getLastSyncAttempt(getContext()));
        } else {
            ((PreferenceActivity) getActivity()).getSupportActionBar().setSubtitle(event.getMessageResId());
        }
    }

    private void setupGpodderScreen() {
        final Activity activity = getActivity();

        findPreference(PREF_GPODNET_SETLOGIN_INFORMATION)
                .setOnPreferenceClickListener(preference -> {
                    AuthenticationDialog dialog = new AuthenticationDialog(activity,
                            R.string.pref_gpodnet_setlogin_information_title, false, GpodnetPreferences.getUsername(),
                            null) {

                        @Override
                        protected void onConfirmed(String username, String password) {
                            GpodnetPreferences.setPassword(password);
                        }
                    };
                    dialog.show();
                    return true;
                });
        findPreference(PREF_GPODNET_SYNC).setOnPreferenceClickListener(preference -> {
                    SyncService.syncImmediately(getActivity().getApplicationContext());
                    return true;
                });
        findPreference(PREF_GPODNET_FORCE_FULL_SYNC).setOnPreferenceClickListener(preference -> {
                    SyncService.fullSync(getContext());
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
        if (loggedIn) {
            String format = getActivity().getString(R.string.pref_gpodnet_login_status);
            String summary = String.format(format, GpodnetPreferences.getUsername(),
                    GpodnetPreferences.getDeviceID());
            findPreference(PREF_GPODNET_LOGOUT).setSummary(Html.fromHtml(summary));
            updateLastGpodnetSyncReport(SyncService.isLastSyncSuccessful(getContext()),
                    SyncService.getLastSyncAttempt(getContext()));
        } else {
            findPreference(PREF_GPODNET_LOGOUT).setSummary(null);
        }
        findPreference(PREF_GPODNET_HOSTNAME).setSummary(GpodnetPreferences.getHostname());
    }

    private void updateLastGpodnetSyncReport(boolean successful, long lastTime) {
        String status = String.format("%1$s (%2$s)", getString(successful
                        ? R.string.gpodnetsync_pref_report_successful : R.string.gpodnetsync_pref_report_failed),
                        DateUtils.getRelativeDateTimeString(getContext(),
                        lastTime, DateUtils.MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_SHOW_TIME));
        ((PreferenceActivity) getActivity()).getSupportActionBar().setSubtitle(status);
    }
}
