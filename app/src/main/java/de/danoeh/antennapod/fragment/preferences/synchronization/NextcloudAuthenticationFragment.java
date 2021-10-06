package de.danoeh.antennapod.fragment.preferences.synchronization;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
    private NextcloudAuthDialogBinding viewBinding;
    private NextcloudLoginFlow nextcloudLoginFlow;
    private boolean shouldDismiss = false;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        dialog.setTitle(R.string.gpodnetauth_login_butLabel);
        dialog.setNegativeButton(R.string.cancel_label, null);
        dialog.setCancelable(false);
        this.setCancelable(false);

        viewBinding = NextcloudAuthDialogBinding.inflate(getLayoutInflater());
        dialog.setView(viewBinding.getRoot());

        viewBinding.loginButton.setOnClickListener(v -> {
            viewBinding.errorText.setVisibility(View.GONE);
            viewBinding.loginButton.setVisibility(View.GONE);
            viewBinding.loginProgressContainer.setVisibility(View.VISIBLE);
            nextcloudLoginFlow = new NextcloudLoginFlow(AntennapodHttpClient.getHttpClient(),
                    viewBinding.serverUrlText.getText().toString(), getContext(), this);
            nextcloudLoginFlow.start();
        });

        return dialog.create();
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
        if (isVisible()) {
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
        viewBinding.loginButton.setVisibility(View.VISIBLE);
    }
}
