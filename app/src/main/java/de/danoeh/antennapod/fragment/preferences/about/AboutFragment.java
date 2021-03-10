package de.danoeh.antennapod.fragment.preferences.about;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import com.google.android.material.snackbar.Snackbar;
import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.PreferenceActivity;
import de.danoeh.antennapod.core.util.ClipboardUtil;
import de.danoeh.antennapod.core.util.IntentUtils;

public class AboutFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_about);

        final Preference aboutVersion = findPreference("about_version");

        aboutVersion.setSummary(String.format(
                "%s (%s)", BuildConfig.VERSION_NAME, BuildConfig.COMMIT_HASH));
        aboutVersion.setOnPreferenceClickListener(preference -> {
            ClipboardUtil.copyToClipboard(
                    getContext(), getString(R.string.bug_report_title), aboutVersion.getSummary());
            Snackbar.make(getView(), R.string.copied_to_clipboard, Snackbar.LENGTH_SHORT).show();
            return true;
        });
        findPreference("about_contributors").setOnPreferenceClickListener((preference) -> {
            getParentFragmentManager().beginTransaction().replace(R.id.content, new ContributorsPagerFragment())
                    .addToBackStack(getString(R.string.contributors)).commit();
            return true;
        });
        findPreference("about_privacy_policy").setOnPreferenceClickListener((preference) -> {
            IntentUtils.openInBrowser(getContext(), "https://antennapod.org/privacy/");
            return true;
        });
        findPreference("about_licenses").setOnPreferenceClickListener((preference) -> {
            getParentFragmentManager().beginTransaction().replace(R.id.content, new LicensesFragment())
                    .addToBackStack(getString(R.string.translators)).commit();
            return true;
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        ((PreferenceActivity) getActivity()).getSupportActionBar().setTitle(R.string.about_pref);
    }
}
