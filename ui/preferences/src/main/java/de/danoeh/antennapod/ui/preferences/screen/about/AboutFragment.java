package de.danoeh.antennapod.ui.preferences.screen.about;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar;
import de.danoeh.antennapod.ui.common.IntentUtils;
import de.danoeh.antennapod.ui.preferences.BuildConfig;
import de.danoeh.antennapod.ui.preferences.R;
import de.danoeh.antennapod.ui.preferences.screen.AnimatedPreferenceFragment;

public class AboutFragment extends AnimatedPreferenceFragment {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_about);

        String versionName = "?";
        try {
            PackageInfo packageInfo = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
            versionName = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        //noinspection ConstantValue
        if ("free".equals(BuildConfig.FLAVOR)) {
            versionName += "f";
        }

        findPreference("about_version").setSummary(String.format(
                "%s (%s)", versionName, BuildConfig.COMMIT_HASH));
        findPreference("about_version").setOnPreferenceClickListener((preference) -> {
            ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(getString(R.string.bug_report_title),
                    findPreference("about_version").getSummary());
            clipboard.setPrimaryClip(clip);
            if (Build.VERSION.SDK_INT <= 32) {
                Snackbar.make(getView(), R.string.copied_to_clipboard, Snackbar.LENGTH_SHORT).show();
            }
            return true;
        });
        findPreference("about_contributors").setOnPreferenceClickListener((preference) -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.settingsContainer, new ContributorsPagerFragment())
                    .addToBackStack(getString(R.string.contributors)).commit();
            return true;
        });
        findPreference("about_privacy_policy").setOnPreferenceClickListener((preference) -> {
            IntentUtils.openInBrowser(getContext(), "https://antennapod.org/privacy/");
            return true;
        });
        findPreference("about_licenses").setOnPreferenceClickListener((preference) -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.settingsContainer, new LicensesFragment())
                    .addToBackStack(getString(R.string.translators)).commit();
            return true;
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.about_pref);
    }
}
