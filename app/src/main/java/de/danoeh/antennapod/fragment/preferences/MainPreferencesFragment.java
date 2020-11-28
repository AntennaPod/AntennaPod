package de.danoeh.antennapod.fragment.preferences;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import com.bytehamster.lib.preferencesearch.SearchConfiguration;
import com.bytehamster.lib.preferencesearch.SearchPreference;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.BugReportActivity;
import de.danoeh.antennapod.activity.PreferenceActivity;
import de.danoeh.antennapod.core.util.IntentUtils;
import de.danoeh.antennapod.core.util.gui.NotificationUtils;
import de.danoeh.antennapod.fragment.preferences.about.AboutFragment;

public class MainPreferencesFragment extends PreferenceFragmentCompat {
    private static final String TAG = "MainPreferencesFragment";

    private static final String PREF_SCREEN_USER_INTERFACE = "prefScreenInterface";
    private static final String PREF_SCREEN_PLAYBACK = "prefScreenPlayback";
    private static final String PREF_SCREEN_NETWORK = "prefScreenNetwork";
    private static final String PREF_SCREEN_GPODDER = "prefScreenGpodder";
    private static final String PREF_SCREEN_STORAGE = "prefScreenStorage";
    private static final String PREF_FAQ = "prefFaq";
    private static final String PREF_VIEW_FORUM = "prefViewForum";
    private static final String PREF_SEND_BUG_REPORT = "prefSendBugReport";
    private static final String PREF_CATEGORY_PROJECT = "project";
    private static final String STATISTICS = "statistics";
    private static final String PREF_ABOUT = "prefAbout";
    private static final String PREF_NOTIFICATION = "notifications";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);
        setupMainScreen();
        setupSearch();

        // If you are writing a spin-off, please update the details on screens like "About" and "Report bug"
        // and afterwards remove the following lines.
        String packageName = getContext().getPackageName();
        if (!"de.danoeh.antennapod".equals(packageName) && !"de.danoeh.antennapod.debug".equals(packageName)) {
            findPreference(PREF_CATEGORY_PROJECT).setVisible(false);
        }
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
        findPreference(PREF_SCREEN_GPODDER).setOnPreferenceClickListener(preference -> {
            ((PreferenceActivity) getActivity()).openScreen(R.xml.preferences_gpodder);
            return true;
        });
        findPreference(PREF_SCREEN_STORAGE).setOnPreferenceClickListener(preference -> {
            ((PreferenceActivity) getActivity()).openScreen(R.xml.preferences_storage);
            return true;
        });
        findPreference(PREF_NOTIFICATION).setOnPreferenceClickListener(preference -> {
            if (Build.VERSION.SDK_INT >= 26) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getActivity().getPackageName());
                startActivity(intent);
            } else {
                ((PreferenceActivity) getActivity()).openScreen(R.xml.preferences_notifications);
            }
            return true;
        });
        findPreference(PREF_ABOUT).setOnPreferenceClickListener(
                preference -> {
                    getParentFragmentManager().beginTransaction().replace(R.id.content, new AboutFragment())
                            .addToBackStack(getString(R.string.about_pref)).commit();
                    return true;
                }
        );
        findPreference(STATISTICS).setOnPreferenceClickListener(
                preference -> {
                    getParentFragmentManager().beginTransaction().replace(R.id.content, new StatisticsFragment())
                            .addToBackStack(getString(R.string.statistics_label)).commit();
                    return true;
                }
        );
        findPreference(PREF_FAQ).setOnPreferenceClickListener(preference -> {
            IntentUtils.openInBrowser(getContext(), "https://antennapod.org/faq.html");
            return true;
        });
        findPreference(PREF_VIEW_FORUM).setOnPreferenceClickListener(preference -> {
            IntentUtils.openInBrowser(getContext(), "https://forum.antennapod.org/");
            return true;
        });
        findPreference(PREF_SEND_BUG_REPORT).setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(getActivity(), BugReportActivity.class));
            return true;
        });
    }

    private void setupSearch() {
        SearchPreference searchPreference = findPreference("searchPreference");
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
                .addBreadcrumb(PreferenceActivity.getTitleOfPage(R.xml.preferences_gpodder));
        config.index(R.xml.preferences_notifications)
                .addBreadcrumb(PreferenceActivity.getTitleOfPage(R.xml.preferences_notifications));
    }
}
