package de.danoeh.antennapod.fragment.preferences.about;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;
import com.google.android.material.snackbar.Snackbar;
import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.PreferenceActivity;
import de.danoeh.antennapod.core.util.IntentUtils;

public class AboutFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_about);

        findPreference("about_version").setSummary(String.format(
                "%s (%s)", BuildConfig.VERSION_NAME, BuildConfig.COMMIT_HASH));
        findPreference("about_version").setOnPreferenceClickListener((preference) -> {
            ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(getString(R.string.bug_report_title),
                    findPreference("about_version").getSummary());
            clipboard.setPrimaryClip(clip);
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
