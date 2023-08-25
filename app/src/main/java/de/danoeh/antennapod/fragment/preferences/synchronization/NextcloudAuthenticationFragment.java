package de.danoeh.antennapod.fragment.preferences.synchronization;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.fragment.app.DialogFragment;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.service.download.AntennapodHttpClient;
import de.danoeh.antennapod.core.sync.SyncService;
import de.danoeh.antennapod.core.sync.SynchronizationCredentials;
import de.danoeh.antennapod.core.sync.SynchronizationProviderViewData;
import de.danoeh.antennapod.core.sync.SynchronizationSettings;
import de.danoeh.antennapod.databinding.NextcloudAuthDialogBinding;
import de.danoeh.antennapod.net.sync.nextcloud.NextcloudLoginFlow;

/**
 * Guides the user through the authentication process.
 */
public class NextcloudAuthenticationFragment extends DialogFragment
        implements NextcloudLoginFlow.AuthenticationCallback {
    public static final String TAG = "NextcloudAuthenticationFragment";
    private static final String EXTRA_LOGIN_FLOW = "LoginFlow";
    private NextcloudAuthDialogBinding viewBinding;
    private NextcloudLoginFlow nextcloudLoginFlow;
    private boolean shouldDismiss = false;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(getContext());
        dialog.setTitle(R.string.gpodnetauth_login_butLabel);
        dialog.setNegativeButton(R.string.cancel_label, null);
        dialog.setCancelable(false);
        this.setCancelable(false);

        viewBinding = NextcloudAuthDialogBinding.inflate(getLayoutInflater());
        dialog.setView(viewBinding.getRoot());

        viewBinding.chooseHostButton.setOnClickListener(v -> {
            nextcloudLoginFlow = new NextcloudLoginFlow(AntennapodHttpClient.getHttpClient(),
                    viewBinding.serverUrlText.getText().toString(), getContext(), this);
            startLoginFlow();
        });
        if (savedInstanceState != null && savedInstanceState.getStringArrayList(EXTRA_LOGIN_FLOW) != null) {
            nextcloudLoginFlow = NextcloudLoginFlow.fromInstanceState(AntennapodHttpClient.getHttpClient(),
                    getContext(), this, savedInstanceState.getStringArrayList(EXTRA_LOGIN_FLOW));
            startLoginFlow();
        }
        return dialog.create();
    }

    private void startLoginFlow() {
        viewBinding.errorText.setVisibility(View.GONE);
        viewBinding.chooseHostButton.setVisibility(View.GONE);
        viewBinding.loginProgressContainer.setVisibility(View.VISIBLE);
        viewBinding.serverUrlText.setEnabled(false);
        nextcloudLoginFlow.start();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (nextcloudLoginFlow != null) {
            outState.putStringArrayList(EXTRA_LOGIN_FLOW, nextcloudLoginFlow.saveInstanceState());
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (nextcloudLoginFlow != null) {
            nextcloudLoginFlow.cancel();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (shouldDismiss) {
            dismiss();
        }
    }

    @Override
    public void onNextcloudAuthenticated(String server, String username, String password) {
        SynchronizationSettings.setSelectedSyncProvider(SynchronizationProviderViewData.NEXTCLOUD_GPODDER);
        SynchronizationCredentials.clear(getContext());
        SynchronizationCredentials.setPassword(password);
        SynchronizationCredentials.setHosturl(server);
        SynchronizationCredentials.setUsername(username);
        SyncService.fullSync(getContext());
        if (isResumed()) {
            dismiss();
        } else {
            shouldDismiss = true;
        }
    }

    @Override
    public void onNextcloudAuthError(String errorMessage) {
        viewBinding.loginProgressContainer.setVisibility(View.GONE);
        viewBinding.errorText.setVisibility(View.VISIBLE);
        viewBinding.errorText.setText(errorMessage);
        viewBinding.chooseHostButton.setVisibility(View.VISIBLE);
        viewBinding.serverUrlText.setEnabled(true);
    }
}
