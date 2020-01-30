package de.danoeh.antennapod.fragment.preferences;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import com.bytehamster.lib.preferencesearch.SearchConfiguration;
import com.bytehamster.lib.preferencesearch.SearchPreference;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.BugReportActivity;
import de.danoeh.antennapod.activity.PreferenceActivity;
import de.danoeh.antennapod.core.util.IntentUtils;

public class MainPreferencesFragment extends PreferenceFragmentCompat {
    private static final String TAG = "MainPreferencesFragment";

    private static final String PREF_SCREEN_USER_INTERFACE = "prefScreenInterface";
    private static final String PREF_SCREEN_PLAYBACK = "prefScreenPlayback";
    private static final String PREF_SCREEN_NETWORK = "prefScreenNetwork";
    private static final String PREF_SCREEN_INTEGRATIONS = "prefScreenIntegrations";
    private static final String PREF_SCREEN_STORAGE = "prefScreenStorage";
    private static final String PREF_FAQ = "prefFaq";
    private static final String PREF_VIEW_MAILING_LIST = "prefViewMailingList";
    private static final String PREF_SEND_BUG_REPORT = "prefSendBugReport";
    private static final String STATISTICS = "statistics";
    private static final String PREF_ABOUT = "prefAbout";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);
        setupMainScreen();
        setupSearch();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((PreferenceActivity) getActivity()).getSupportActionBar().setTitle(R.string.settings_label);
    }

    private void setupMainScreen() {
        findPreference(PREF_SCREEN_USER_INTERFACE).setOnPreferenceClickListener(preference -> {
            ((PreferenceActivity) getActivity()).openScreen(R.xml.preferences_user_interface);
            return true;
        });
        findPreference(PREF_SCREEN_PLAYBACK).setOnPreferenceClickListener(preference -> {
            ((PreferenceActivity) getActivity()).openScreen(R.xml.preferences_playback);
            return true;
        });
        findPreference(PREF_SCREEN_NETWORK).setOnPreferenceClickListener(preference -> {
            ((PreferenceActivity) getActivity()).openScreen(R.xml.preferences_network);
            return true;
        });
        findPreference(PREF_SCREEN_INTEGRATIONS).setOnPreferenceClickListener(preference -> {
            ((PreferenceActivity) getActivity()).openScreen(R.xml.preferences_integrations);
            return true;
        });
        findPreference(PREF_SCREEN_STORAGE).setOnPreferenceClickListener(preference -> {
            ((PreferenceActivity) getActivity()).openScreen(R.xml.preferences_storage);
            return true;
        });

        findPreference(PREF_ABOUT).setOnPreferenceClickListener(
                preference -> {
                    getFragmentManager().beginTransaction().replace(R.id.content, new AboutFragment())
                            .addToBackStack(getString(R.string.about_pref)).commit();
                    return true;
                }
        );
        findPreference(STATISTICS).setOnPreferenceClickListener(
                preference -> {
                    getFragmentManager().beginTransaction().replace(R.id.content, new StatisticsFragment())
                            .addToBackStack(getString(R.string.statistics_label)).commit();
                    return true;
                }
        );
        findPreference(PREF_FAQ).setOnPreferenceClickListener(preference -> {
            IntentUtils.openInBrowser(getContext(), "https://antennapod.org/faq.html");
            return true;
        });
        findPreference(PREF_VIEW_MAILING_LIST).setOnPreferenceClickListener(preference -> {
            IntentUtils.openInBrowser(getContext(), "https://groups.google.com/forum/#!forum/antennapod");
            return true;
        });
        findPreference(PREF_SEND_BUG_REPORT).setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(getActivity(), BugReportActivity.class));
            return true;
        });
    }

    private void setupSearch() {
        SearchPreference searchPreference = (SearchPreference) findPreference("searchPreference");
        SearchConfiguration config = searchPreference.getSearchConfiguration();
        config.setActivity((AppCompatActivity) getActivity());
        config.setFragmentContainerViewId(R.id.content);
        config.setBreadcrumbsEnabled(true);

        config.index(R.xml.preferences_user_interface)
                .addBreadcrumb(PreferenceActivity.getTitleOfPage(R.xml.preferences_user_interface));
        config.index(R.xml.preferences_playback)
                .addBreadcrumb(PreferenceActivity.getTitleOfPage(R.xml.preferences_playback));
        config.index(R.xml.preferences_network)
                .addBreadcrumb(PreferenceActivity.getTitleOfPage(R.xml.preferences_network));
        config.index(R.xml.preferences_storage)
                .addBreadcrumb(PreferenceActivity.getTitleOfPage(R.xml.preferences_storage));
        config.index(R.xml.preferences_import_export)
                .addBreadcrumb(PreferenceActivity.getTitleOfPage(R.xml.preferences_storage))
                .addBreadcrumb(PreferenceActivity.getTitleOfPage(R.xml.preferences_import_export));
        config.index(R.xml.preferences_autodownload)
                .addBreadcrumb(PreferenceActivity.getTitleOfPage(R.xml.preferences_network))
                .addBreadcrumb(R.string.automation)
                .addBreadcrumb(PreferenceActivity.getTitleOfPage(R.xml.preferences_autodownload));
        config.index(R.xml.preferences_gpodder)
                .addBreadcrumb(PreferenceActivity.getTitleOfPage(R.xml.preferences_integrations))
                .addBreadcrumb(PreferenceActivity.getTitleOfPage(R.xml.preferences_gpodder));
    }
}
