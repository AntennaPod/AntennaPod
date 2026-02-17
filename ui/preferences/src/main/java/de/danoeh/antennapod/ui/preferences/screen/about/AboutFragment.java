package de.danoeh.antennapod.ui.preferences.screen.about;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

import de.danoeh.antennapod.ui.common.AnimatedFragment;
import de.danoeh.antennapod.ui.common.IntentUtils;
import de.danoeh.antennapod.ui.preferences.BuildConfig;
import de.danoeh.antennapod.ui.preferences.R;

import java.util.Locale;

public class AboutFragment extends AnimatedFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_about_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Context context = requireContext();
        String versionName = "?";
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            versionName = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if ("free".equals(BuildConfig.FLAVOR)) {
            versionName += "f";
        }

        String commitHash = BuildConfig.COMMIT_HASH;
        String buildType = BuildConfig.BUILD_TYPE;
        String flavor = BuildConfig.FLAVOR;
        String packageId = context.getPackageName();
        String channel = BuildConfig.DEBUG ? "debug" : "release";
        if (versionName.toLowerCase(Locale.US).contains("beta")) {
            channel = "beta";
        }
        String versionSummary = getString(R.string.about_version_format, versionName, commitHash);

        TextView versionValue = view.findViewById(R.id.about_version_value);
        versionValue.setText(versionSummary);

        TextView buildTypeValue = view.findViewById(R.id.about_build_type_value);
        buildTypeValue.setText(buildType);

        TextView channelValue = view.findViewById(R.id.about_channel_value);
        channelValue.setText(channel);

        TextView flavorValue = view.findViewById(R.id.about_flavor_value);
        flavorValue.setText(flavor);

        TextView commitValue = view.findViewById(R.id.about_commit_value);
        commitValue.setText(commitHash);

        TextView packageValue = view.findViewById(R.id.about_package_value);
        packageValue.setText(packageId);

        Button copyVersion = view.findViewById(R.id.about_copy_version);
        copyVersion.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(getString(R.string.about_pref), versionSummary);
            clipboard.setPrimaryClip(clip);
            if (Build.VERSION.SDK_INT <= 32) {
                Snackbar.make(view, R.string.copied_to_clipboard, Snackbar.LENGTH_SHORT).show();
            }
        });

        Button contributorsButton = view.findViewById(R.id.button_contributors);
        contributorsButton.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .replace(R.id.settingsContainer, new ContributorsPagerFragment())
                .addToBackStack(getString(R.string.contributors)).commit());

        Button licensesButton = view.findViewById(R.id.button_licenses);
        licensesButton.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .replace(R.id.settingsContainer, new LicensesFragment())
                .addToBackStack(getString(R.string.translators)).commit());

        Button privacyButton = view.findViewById(R.id.button_privacy);
        privacyButton.setOnClickListener(v -> IntentUtils.openInBrowser(context, "https://antennapod.org/privacy/"));

        Button upstreamButton = view.findViewById(R.id.button_upstream);
        upstreamButton.setOnClickListener(v -> IntentUtils.openInBrowser(context,
                getString(R.string.fork_notice_upstream)));
    }

    @Override
    public void onStart() {
        super.onStart();
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.about_pref);
    }
}
