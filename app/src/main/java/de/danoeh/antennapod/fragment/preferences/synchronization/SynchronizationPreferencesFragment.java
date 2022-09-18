package de.danoeh.antennapod.fragment.preferences.synchronization;

import android.app.Activity;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.core.text.HtmlCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.snackbar.Snackbar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.PreferenceActivity;
import de.danoeh.antennapod.event.SyncServiceEvent;
import de.danoeh.antennapod.core.sync.SynchronizationCredentials;
import de.danoeh.antennapod.core.sync.SyncService;
import de.danoeh.antennapod.core.sync.SynchronizationProviderViewData;
import de.danoeh.antennapod.core.sync.SynchronizationSettings;
import de.danoeh.antennapod.dialog.AuthenticationDialog;

public class SynchronizationPreferencesFragment extends PreferenceFragmentCompat {
    private static final String PREFERENCE_SYNCHRONIZATION_DESCRIPTION = "preference_synchronization_description";
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
        findPreference(PREFERENCE_GPODNET_SETLOGIN_INFORMATION)
                .setOnPreferenceClickListener(preference -> {
                    AuthenticationDialog dialog = new AuthenticationDialog(activity,
                            R.string.pref_gpodnet_setlogin_information_title,
                            false, SynchronizationCredentials.getUsername(), null) {
                        @Override
                        protected void onConfirmed(String username, String password) {
                            SynchronizationCredentials.setPassword(password);
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
            SynchronizationCredentials.clear(getContext());
            Snackbar.make(getView(), R.string.pref_synchronization_logout_toast, Snackbar.LENGTH_LONG).show();
            SynchronizationSettings.setSelectedSyncProvider(null);
            updateScreen();
            return true;
        });
    }

    private void updateScreen() {
        final boolean loggedIn = SynchronizationSettings.isProviderConnected();
        Preference preferenceHeader = findPreference(PREFERENCE_SYNCHRONIZATION_DESCRIPTION);
        if (loggedIn) {
            SynchronizationProviderViewData selectedProvider =
                    SynchronizationProviderViewData.fromIdentifier(getSelectedSyncProviderKey());
            preferenceHeader.setTitle("");
            preferenceHeader.setSummary(selectedProvider.getSummaryResource());
            preferenceHeader.setIcon(selectedProvider.getIconResource());
            preferenceHeader.setOnPreferenceClickListener(null);
        } else {
            preferenceHeader.setTitle(R.string.synchronization_choose_title);
            preferenceHeader.setSummary(R.string.synchronization_summary_unchoosen);
            preferenceHeader.setIcon(R.drawable.ic_cloud);
            preferenceHeader.setOnPreferenceClickListener((preference) -> {
                chooseProviderAndLogin();
                return true;
            });
        }

        Preference gpodnetSetLoginPreference = findPreference(PREFERENCE_GPODNET_SETLOGIN_INFORMATION);
        gpodnetSetLoginPreference.setVisible(isProviderSelected(SynchronizationProviderViewData.GPODDER_NET));
        gpodnetSetLoginPreference.setEnabled(loggedIn);
        findPreference(PREFERENCE_SYNC).setEnabled(loggedIn);
        findPreference(PREFERENCE_FORCE_FULL_SYNC).setEnabled(loggedIn);
        findPreference(PREFERENCE_LOGOUT).setEnabled(loggedIn);
        if (loggedIn) {
            String summary = getString(R.string.synchronization_login_status,
                    SynchronizationCredentials.getUsername(), SynchronizationCredentials.getHosturl());
            Spanned formattedSummary = HtmlCompat.fromHtml(summary, HtmlCompat.FROM_HTML_MODE_LEGACY);
            findPreference(PREFERENCE_LOGOUT).setSummary(formattedSummary);
            updateLastSyncReport(SynchronizationSettings.isLastSyncSuccessful(),
                    SynchronizationSettings.getLastSyncAttempt());
        } else {
            findPreference(PREFERENCE_LOGOUT).setSummary(null);
            ((PreferenceActivity) getActivity()).getSupportActionBar().setSubtitle(null);
        }
    }

    private void chooseProviderAndLogin() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
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
            switch (providers[which]) {
                case GPODDER_NET:
                    new GpodderAuthenticationFragment()
                            .show(getChildFragmentManager(), GpodderAuthenticationFragment.TAG);
                    break;
                case NEXTCLOUD_GPODDER:
                    new NextcloudAuthenticationFragment()
                            .show(getChildFragmentManager(), NextcloudAuthenticationFragment.TAG);
                    break;
                default:
                    break;
            }
            updateScreen();
        });

        builder.show();
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
}
