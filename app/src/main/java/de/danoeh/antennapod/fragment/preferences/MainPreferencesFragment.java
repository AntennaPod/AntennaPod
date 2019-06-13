package de.danoeh.antennapod.fragment.preferences;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;
import android.widget.Toast;
import com.bytehamster.lib.preferencesearch.SearchConfiguration;
import com.bytehamster.lib.preferencesearch.SearchPreference;
import de.danoeh.antennapod.CrashReportWriter;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.AboutActivity;
import de.danoeh.antennapod.activity.PreferenceActivity;
import de.danoeh.antennapod.activity.StatisticsActivity;

import java.util.List;

public class MainPreferencesFragment extends PreferenceFragmentCompat {
    private static final String TAG = "MainPreferencesFragment";

    private static final String PREF_SCREEN_USER_INTERFACE = "prefScreenInterface";
    private static final String PREF_SCREEN_PLAYBACK = "prefScreenPlayback";
    private static final String PREF_SCREEN_NETWORK = "prefScreenNetwork";
    private static final String PREF_SCREEN_INTEGRATIONS = "prefScreenIntegrations";
    private static final String PREF_SCREEN_STORAGE = "prefScreenStorage";
    private static final String PREF_KNOWN_ISSUES = "prefKnownIssues";
    private static final String PREF_FAQ = "prefFaq";
    private static final String PREF_SEND_CRASH_REPORT = "prefSendCrashReport";
    private static final String STATISTICS = "statistics";
    private static final String PREF_ABOUT = "prefAbout";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);
        setupMainScreen();
        setupSearch();
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
                    startActivity(new Intent(getActivity(), AboutActivity.class));
                    return true;
                }
        );
        findPreference(STATISTICS).setOnPreferenceClickListener(
                preference -> {
                    startActivity(new Intent(getActivity(), StatisticsActivity.class));
                    return true;
                }
        );
        findPreference(PREF_KNOWN_ISSUES).setOnPreferenceClickListener(preference -> {
            openInBrowser("https://github.com/AntennaPod/AntennaPod/labels/bug");
            return true;
        });
        findPreference(PREF_FAQ).setOnPreferenceClickListener(preference -> {
            openInBrowser("http://antennapod.org/faq.html");
            return true;
        });
        findPreference(PREF_SEND_CRASH_REPORT).setOnPreferenceClickListener(preference -> {
            Context context = getActivity().getApplicationContext();
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("text/plain");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"Martin.Fietz@gmail.com"});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "AntennaPod Crash Report");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Please describe what you were doing when the app crashed");
            // the attachment
            Uri fileUri = FileProvider.getUriForFile(context, context.getString(R.string.provider_authority),
                    CrashReportWriter.getFile());
            emailIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            emailIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            String intentTitle = getActivity().getString(R.string.send_email);
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(emailIntent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    context.grantUriPermission(packageName, fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            }
            getActivity().startActivity(Intent.createChooser(emailIntent, intentTitle));
            return true;
        });
    }

    private void openInBrowser(String url) {
        try {
            Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(myIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getActivity(), R.string.pref_no_browser_found, Toast.LENGTH_LONG).show();
            Log.e(TAG, Log.getStackTraceString(e));
        }
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
        config.index(R.xml.preferences_autodownload)
                .addBreadcrumb(PreferenceActivity.getTitleOfPage(R.xml.preferences_network))
                .addBreadcrumb(R.string.automation)
                .addBreadcrumb(PreferenceActivity.getTitleOfPage(R.xml.preferences_autodownload));
        config.index(R.xml.preferences_gpodder)
                .addBreadcrumb(PreferenceActivity.getTitleOfPage(R.xml.preferences_integrations))
                .addBreadcrumb(PreferenceActivity.getTitleOfPage(R.xml.preferences_gpodder));
        config.index(R.xml.preferences_flattr)
                .addBreadcrumb(PreferenceActivity.getTitleOfPage(R.xml.preferences_integrations))
                .addBreadcrumb(PreferenceActivity.getTitleOfPage(R.xml.preferences_flattr));
    }
}
