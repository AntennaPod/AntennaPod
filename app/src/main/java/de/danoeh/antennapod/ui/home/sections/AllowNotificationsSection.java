package de.danoeh.antennapod.ui.home.sections;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.databinding.HomeSectionNotificationBinding;
import de.danoeh.antennapod.ui.home.HomeFragment;

public class AllowNotificationsSection extends Fragment {
    HomeSectionNotificationBinding viewBinding;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    ((MainActivity) getActivity()).loadFragment(HomeFragment.TAG, null);
                } else {
                    viewBinding.openSettingsButton.setVisibility(View.VISIBLE);
                    viewBinding.allowButton.setVisibility(View.GONE);
                    Toast.makeText(getContext(), R.string.notification_permission_denied, Toast.LENGTH_LONG).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        viewBinding = HomeSectionNotificationBinding.inflate(inflater);
        viewBinding.allowButton.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        });
        viewBinding.openSettingsButton.setOnClickListener(view -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getContext().getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        });
        viewBinding.denyButton.setOnClickListener(v -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
            builder.setMessage(R.string.notification_permission_deny_warning);
            builder.setPositiveButton(R.string.deny_label, (dialog, which) -> {
                getContext().getSharedPreferences(HomeFragment.PREF_NAME, Context.MODE_PRIVATE)
                        .edit().putBoolean(HomeFragment.PREF_DISABLE_NOTIFICATION_PERMISSION_NAG, true).apply();
                ((MainActivity) getActivity()).loadFragment(HomeFragment.TAG, null);
            });
            builder.setNegativeButton(R.string.cancel_label, null);
            builder.show();
        });
        return viewBinding.getRoot();
    }
}
