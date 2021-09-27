package de.danoeh.antennapod.fragment.preferences.synchronization;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.text.HtmlCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.snackbar.Snackbar;
import com.nextcloud.android.sso.AccountImporter;
import com.nextcloud.android.sso.exceptions.AccountImportCancelledException;
import com.nextcloud.android.sso.exceptions.AndroidGetAccountsPermissionNotGranted;
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppNotInstalledException;
import com.nextcloud.android.sso.helper.SingleAccountHelper;
import com.nextcloud.android.sso.ui.UiExceptionManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.PreferenceActivity;
import de.danoeh.antennapod.core.event.SyncServiceEvent;
import de.danoeh.antennapod.core.preferences.GpodnetPreferences;
import de.danoeh.antennapod.core.sync.SyncService;
import de.danoeh.antennapod.core.sync.SynchronizationProviderViewData;
import de.danoeh.antennapod.core.sync.SynchronizationSettings;
import de.danoeh.antennapod.dialog.AuthenticationDialog;
import de.danoeh.antennapod.fragment.preferences.GpodderAuthenticationFragment;

public class SynchronizationPreferencesFragment extends PreferenceFragmentCompat {
    private static final String PREFERENCE_SYNCHRONIZATION_DESCRIPTION = "preference_synchronization_description";
    private static final String PREFERENCE_LOGIN = "pref_synchronization_authenticate";
    private static final String PREFERENCE_GPODNET_SETLOGIN_INFORMATION = "pref_gpodnet_setlogin_information";
    private static final String PREFERENCE_SYNC = "pref_synchronization_sync";
    private static final String PREFERENCE_FORCE_FULL_SYNC = "pref_synchronization_force_full_sync";
    private static final String PREFERENCE_LOGOUT = "pref_synchronization_logout";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_synchronization);
        setupScreen();
        updateScreen();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((PreferenceActivity) getActivity()).getSupportActionBar().setTitle(R.string.synchronization_pref);
        updateScreen();
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
        if (!SynchronizationSettings.isProviderConnected()) {
            return;
        }
        updateScreen();
        if (event.getMessageResId() == R.string.sync_status_error
                || event.getMessageResId() == R.string.sync_status_success) {
            updateLastSyncReport(SynchronizationSettings.isLastSyncSuccessful(),
                    SynchronizationSettings.getLastSyncAttempt());
        } else {
            ((PreferenceActivity) getActivity()).getSupportActionBar().setSubtitle(event.getMessageResId());
        }
    }

    private void setupScreen() {
        final Activity activity = getActivity();

        findPreference(PREFERENCE_LOGIN).setOnPreferenceClickListener(preference -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(R.string.dialog_choose_sync_service_title);

            SynchronizationProviderViewData[] providers = SynchronizationProviderViewData.values();
            ListAdapter adapter = new ArrayAdapter<SynchronizationProviderViewData>(
                    getContext(), R.layout.alertdialog_sync_provider_chooser, providers) {

                ViewHolder holder;

                class ViewHolder {
                    ImageView icon;
                    TextView title;
                }

                public View getView(int position, View convertView, ViewGroup parent) {
                    final LayoutInflater inflater = LayoutInflater.from(getContext());
                    if (convertView == null) {
                        convertView = inflater.inflate(
                                R.layout.alertdialog_sync_provider_chooser, null);

                        holder = new ViewHolder();
                        holder.icon = (ImageView) convertView.findViewById(R.id.icon);
                        holder.title = (TextView) convertView.findViewById(R.id.title);
                        convertView.setTag(holder);
                    } else {
                        holder = (ViewHolder) convertView.getTag();
                    }
                    SynchronizationProviderViewData synchronizationProviderViewData = getItem(position);
                    holder.title.setText(synchronizationProviderViewData.getSummaryResource());
                    holder.icon.setImageResource(synchronizationProviderViewData.getIconResource());
                    return convertView;
                }
            };

            builder.setAdapter(adapter, (dialog, which) -> {
                setSelectedSyncProvider(providers[which]);
                switch (providers[which]) {
                    case GPODDER_NET:
                        new GpodderAuthenticationFragment()
                                .show(getChildFragmentManager(), GpodderAuthenticationFragment.TAG);
                        break;
                    case NEXTCLOUD_GPODDER:
                        openNextcloudAccountChooser();
                        break;
                    default:
                        break;
                }
                updateScreen();
            });

            AlertDialog dialog = builder.create();
            dialog.show();
            return true;
        });
        findPreference(PREFERENCE_GPODNET_SETLOGIN_INFORMATION)
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
        findPreference(PREFERENCE_SYNC).setOnPreferenceClickListener(preference -> {
            SyncService.syncImmediately(getActivity().getApplicationContext());
            return true;
        });
        findPreference(PREFERENCE_FORCE_FULL_SYNC).setOnPreferenceClickListener(preference -> {
            SyncService.fullSync(getContext());
            return true;
        });
        findPreference(PREFERENCE_LOGOUT).setOnPreferenceClickListener(preference -> {
            GpodnetPreferences.logout(getContext());
            Snackbar.make(getView(), R.string.pref_synchronization_logout_toast, Snackbar.LENGTH_LONG).show();
            SynchronizationSettings.setIsProviderConnected(false);
            setSelectedSyncProvider(null);
            updateScreen();
            return true;
        });
    }

    private void setSelectedSyncProvider(SynchronizationProviderViewData userSelect) {
        SynchronizationSettings.setSelectedSyncProvider(userSelect);
    }

    private void updateScreen() {
        final boolean loggedIn = SynchronizationSettings.isProviderConnected();
        findPreference(PREFERENCE_LOGIN).setVisible(!loggedIn);

        Preference preferenceHeader = findPreference(PREFERENCE_SYNCHRONIZATION_DESCRIPTION);
        preferenceHeader.setIcon(ViewDataProvider.getSynchronizationProviderIcon(getSelectedSyncProviderKey()));
        preferenceHeader.setSummary(ViewDataProvider.getSynchronizationProviderHeaderSummary(
                getSelectedSyncProviderKey()));

        Preference gpodnetSetLoginPreference = findPreference(PREFERENCE_GPODNET_SETLOGIN_INFORMATION);
        gpodnetSetLoginPreference.setVisible(isProviderSelected(SynchronizationProviderViewData.GPODDER_NET));
        gpodnetSetLoginPreference.setEnabled(loggedIn);
        findPreference(PREFERENCE_SYNC).setEnabled(loggedIn);
        findPreference(PREFERENCE_FORCE_FULL_SYNC).setEnabled(loggedIn);
        findPreference(PREFERENCE_LOGOUT).setEnabled(loggedIn);
        if (loggedIn) {
            String format = getActivity().getString(R.string.synchronization_login_status);
            String summary = String.format(format,
                    ViewDataProvider.getUsernameFromSelectedSyncProvider(getContext(), getSelectedSyncProviderKey()));
            Spanned formattedSummary = HtmlCompat.fromHtml(summary, HtmlCompat.FROM_HTML_MODE_LEGACY);
            findPreference(PREFERENCE_LOGOUT).setSummary(formattedSummary);
            updateLastSyncReport(SynchronizationSettings.isLastSyncSuccessful(),
                    SynchronizationSettings.getLastSyncAttempt());
        } else {
            findPreference(PREFERENCE_LOGOUT).setSummary(null);
            ((PreferenceActivity) getActivity()).getSupportActionBar().setSubtitle(null);
        }
    }

    private boolean isProviderSelected(@NonNull SynchronizationProviderViewData provider) {
        String selectedSyncProviderKey = getSelectedSyncProviderKey();
        return provider.getIdentifier().equals(selectedSyncProviderKey);
    }

    private String getSelectedSyncProviderKey() {
        return SynchronizationSettings.getSelectedSyncProviderKey();
    }

    private void updateLastSyncReport(boolean successful, long lastTime) {
        String status = String.format("%1$s (%2$s)", getString(successful
                        ? R.string.gpodnetsync_pref_report_successful : R.string.gpodnetsync_pref_report_failed),
                DateUtils.getRelativeDateTimeString(getContext(),
                        lastTime, DateUtils.MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_SHOW_TIME));
        ((PreferenceActivity) getActivity()).getSupportActionBar().setSubtitle(status);
    }

    private void openNextcloudAccountChooser() {
        try {
            AccountImporter.pickNewAccount(this);
        } catch (NextcloudFilesAppNotInstalledException | AndroidGetAccountsPermissionNotGranted e) {
            UiExceptionManager.showDialogForException(getContext(), e);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            AccountImporter.onActivityResult(requestCode, resultCode, data,
                    SynchronizationPreferencesFragment.this,
                    singleSignOnAccount
                            -> {
                        SingleAccountHelper.setCurrentAccount(getContext(), singleSignOnAccount.name);
                        SynchronizationSettings.setIsProviderConnected(true);
                        SyncService.fullSync(getContext());
                        updateScreen();
                    });
        } catch (AccountImportCancelledException e) {
            e.printStackTrace();
        }
    }
}
