package de.danoeh.antennapod.ui.screen.preferences;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;

import com.bytehamster.lib.preferencesearch.SearchConfiguration;
import com.bytehamster.lib.preferencesearch.SearchPreference;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.EditTextDialogBinding;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.common.IntentUtils;
import de.danoeh.antennapod.ui.preferences.screen.AnimatedPreferenceFragment;
import de.danoeh.antennapod.ui.preferences.screen.about.AboutFragment;
import de.danoeh.antennapod.ui.preferences.screen.bugreport.BugReportFragment;

import java.io.IOException;

public class MainPreferencesFragment extends AnimatedPreferenceFragment {

    private static final String PREF_SCREEN_USER_INTERFACE = "prefScreenInterface";
    private static final String PREF_SCREEN_PLAYBACK = "prefScreenPlayback";
    private static final String PREF_SCREEN_DOWNLOADS = "prefScreenDownloads";
    private static final String PREF_SCREEN_IMPORT_EXPORT = "prefScreenImportExport";
    private static final String PREF_SCREEN_SYNCHRONIZATION = "prefScreenSynchronization";
    private static final String PREF_DOCUMENTATION = "prefDocumentation";
    private static final String PREF_VIEW_FORUM = "prefViewForum";
    private static final String PREF_SEND_BUG_REPORT = "prefSendBugReport";
    private static final String PREF_CATEGORY_PROJECT = "project";
    private static final String PREF_ABOUT = "prefAbout";
    private static final String PREF_NOTIFICATION = "notifications";
    private static final String PREF_CONTRIBUTE = "prefContribute";
    private static final String PREF_PARENTAL_CONTROL_PASSWORD = "prefParentalControlPassword";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);
        setupMainScreen();
        setupSearch();
        setParentalControlsVisibility();

        // If you are writing a spin-off, please update the details on screens like "About" and "Report bug"
        // and afterwards remove the following lines. Please keep in mind that AntennaPod is licensed under the GPL.
        // This means that your application needs to be open-source under the GPL, too.
        // It must also include a prominent copyright notice.
        int packageHash = getContext().getPackageName().hashCode();
        if (packageHash != 1790437538 && packageHash != -1190467065) {
            findPreference(PREF_CATEGORY_PROJECT).setVisible(false);
            Preference copyrightNotice = new Preference(getContext());
            copyrightNotice.setIcon(R.drawable.ic_info_white);
            copyrightNotice.getIcon().mutate()
                    .setColorFilter(new PorterDuffColorFilter(0xffcc0000, PorterDuff.Mode.MULTIPLY));
            copyrightNotice.setSummary("This application is based on AntennaPod."
                    + " The AntennaPod team does NOT provide support for this unofficial version."
                    + " If you can read this message, the developers of this modification"
                    + " violate the GNU General Public License (GPL).");
            findPreference(PREF_CATEGORY_PROJECT).getParent().addPreference(copyrightNotice);
        } else if (packageHash == -1190467065) {
            Preference debugNotice = new Preference(getContext());
            debugNotice.setIcon(R.drawable.ic_info_white);
            debugNotice.getIcon().mutate()
                    .setColorFilter(new PorterDuffColorFilter(0xffcc0000, PorterDuff.Mode.MULTIPLY));
            debugNotice.setOrder(-1);
            debugNotice.setSummary("This is a development version of AntennaPod and not meant for daily use");
            findPreference(PREF_CATEGORY_PROJECT).getParent().addPreference(debugNotice);
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
        findPreference(PREF_SCREEN_DOWNLOADS).setOnPreferenceClickListener(preference -> {
            ((PreferenceActivity) getActivity()).openScreen(R.xml.preferences_downloads);
            return true;
        });
        findPreference(PREF_SCREEN_SYNCHRONIZATION).setOnPreferenceClickListener(preference -> {
            ((PreferenceActivity) getActivity()).openScreen(R.xml.preferences_synchronization);
            return true;
        });
        findPreference(PREF_SCREEN_IMPORT_EXPORT).setOnPreferenceClickListener(preference -> {
            ((PreferenceActivity) getActivity()).openScreen(R.xml.preferences_import_export);
            return true;
        });
        findPreference(PREF_NOTIFICATION).setOnPreferenceClickListener(preference -> {
            ((PreferenceActivity) getActivity()).openScreen(R.xml.preferences_notifications);
            return true;
        });
        findPreference(PREF_ABOUT).setOnPreferenceClickListener(
                preference -> {
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.settingsContainer, new AboutFragment())
                            .addToBackStack(getString(R.string.about_pref)).commit();
                    return true;
                }
        );
        findPreference(PREF_DOCUMENTATION).setOnPreferenceClickListener(preference -> {
            IntentUtils.openInBrowser(getContext(), "https://antennapod.org/documentation/");
            return true;
        });
        findPreference(PREF_VIEW_FORUM).setOnPreferenceClickListener(preference -> {
            IntentUtils.openInBrowser(getContext(), "https://forum.antennapod.org/");
            return true;
        });
        findPreference(PREF_CONTRIBUTE).setOnPreferenceClickListener(preference -> {
            IntentUtils.openInBrowser(getContext(), "https://antennapod.org/contribute/");
            return true;
        });
        findPreference(PREF_SEND_BUG_REPORT).setOnPreferenceClickListener(preference -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.settingsContainer, new BugReportFragment())
                    .addToBackStack(getString(R.string.report_bug_title)).commit();
            return true;
        });
        findPreference(PREF_PARENTAL_CONTROL_PASSWORD).setOnPreferenceClickListener(preference -> {
            showChangePasswordDialog();
            return true;
        });
    }

    // show the 'parental password' preference if we're on a 'child' device (family link) or if it's a debug build
    private void setParentalControlsVisibility() {
        findPreference(PREF_PARENTAL_CONTROL_PASSWORD).setVisible(false);

        // --- Approach 1: AccountManager - all accounts ---
        AccountManager am = AccountManager.get(requireContext());
        Account[] allAccounts = am.getAccounts();
        android.util.Log.d("ParentalControl", "All accounts (" + allAccounts.length + "):");
        for (Account account : allAccounts) {
            android.util.Log.d("ParentalControl", "  name=" + account.name + " type=" + account.type);
        }

        // --- Approach 2: AccountManager feature checks on all Google accounts ---
        String[] featuresToCheck = {"child", "service_uca_familylink", "features_FAMILY_LINK_CHILD"};
        Account[] googleAccounts = am.getAccountsByType("com.google");
        android.util.Log.d("ParentalControl", "Google accounts: " + googleAccounts.length);
        for (Account account : googleAccounts) {
            for (String feature : featuresToCheck) {
                am.hasFeatures(account, new String[]{feature}, future -> {
                    try {
                        boolean has = future.getResult();
                        android.util.Log.d("ParentalControl", "  " + account.name + " feature=" + feature + " -> " + has);
                        if (has) {
                            findPreference(PREF_PARENTAL_CONTROL_PASSWORD).setVisible(true);
                        }
                    } catch (AuthenticatorException | OperationCanceledException | IOException e) {
                        android.util.Log.e("ParentalControl", "  error for feature=" + feature, e);
                    }
                }, new Handler(Looper.getMainLooper()));
            }
        }

        // --- Approach 3: UserManager restrictions set by Family Link ---
        android.os.UserManager um = requireContext().getSystemService(android.os.UserManager.class);
        android.os.Bundle restrictions = um.getUserRestrictions();
        android.util.Log.d("ParentalControl", "UserRestrictions:");
        for (String key : restrictions.keySet()) {
            android.util.Log.d("ParentalControl", "  " + key + "=" + restrictions.get(key));
        }
        boolean disallowInstall = um.hasUserRestriction(android.os.UserManager.DISALLOW_INSTALL_APPS);
        boolean disallowUninstall = um.hasUserRestriction(android.os.UserManager.DISALLOW_UNINSTALL_APPS);
        android.util.Log.d("ParentalControl", "DISALLOW_INSTALL_APPS=" + disallowInstall
                + " DISALLOW_UNINSTALL_APPS=" + disallowUninstall);

        // --- Approach 4: Check if Family Link helper app is installed ---
        String[] familyLinkPackages = {
                "com.google.android.apps.kids.familylinkhelper",
                "com.google.android.apps.kids.familylink",
                "com.google.android.apps.kids.home"
        };
        android.content.pm.PackageManager pm = requireContext().getPackageManager();
        for (String pkg : familyLinkPackages) {
            boolean installed = false;
            try {
                pm.getPackageInfo(pkg, 0);
                installed = true;
            } catch (android.content.pm.PackageManager.NameNotFoundException ignored) { }
            android.util.Log.d("ParentalControl", "package " + pkg + " installed=" + installed);
        }
    }

    private void showChangePasswordDialog() {
        if (UserPreferences.isParentalControlPasswordSet()) {
            showVerifyOldPasswordDialog();
        } else {
            showSetNewPasswordDialog(false);
        }
    }

    private void showVerifyOldPasswordDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(R.string.pref_parental_control_password_title);
        builder.setMessage(R.string.pref_parental_control_enter_old_password);
        final EditTextDialogBinding dialogBinding = EditTextDialogBinding.inflate(getLayoutInflater());
        dialogBinding.textInput.setHint(R.string.pref_parental_control_old_password);
        dialogBinding.textInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(dialogBinding.getRoot());
        builder.setPositiveButton(R.string.confirm_label, null);
        builder.setNegativeButton(R.string.cancel_label, null);
        builder.setNeutralButton(R.string.pref_parental_control_clear_password, null);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String oldPassword = dialogBinding.textInput.getText().toString();
            if (UserPreferences.verifyParentalControlPassword(oldPassword)) {
                alertDialog.dismiss();
                showSetNewPasswordDialog(true);
            } else {
                dialogBinding.textInputLayout.setError(getString(R.string.wrong_password));
                Toast.makeText(requireContext(), R.string.wrong_password, Toast.LENGTH_SHORT).show();
            }
        });

        alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            String oldPassword = dialogBinding.textInput.getText().toString();
            if (UserPreferences.verifyParentalControlPassword(oldPassword)) {
                UserPreferences.clearParentalControlPassword();
                Toast.makeText(requireContext(), R.string.pref_parental_control_password_cleared, Toast.LENGTH_SHORT)
                        .show();
                alertDialog.dismiss();
            } else {
                dialogBinding.textInputLayout.setError(getString(R.string.wrong_password));
                Toast.makeText(requireContext(), R.string.wrong_password, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSetNewPasswordDialog(boolean isChanging) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(isChanging ? R.string.pref_parental_control_change_password
                : R.string.pref_parental_control_set_password);
        final EditTextDialogBinding dialogBinding = EditTextDialogBinding.inflate(getLayoutInflater());
        dialogBinding.textInput.setHint(R.string.pref_parental_control_new_password);
        dialogBinding.textInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(dialogBinding.getRoot());
        builder.setPositiveButton(R.string.confirm_label, null);
        builder.setNegativeButton(R.string.cancel_label, null);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newPassword = dialogBinding.textInput.getText().toString();
            if (newPassword.isEmpty()) {
                dialogBinding.textInputLayout.setError(getString(R.string.pref_parental_control_password_empty));
                return;
            }
            showConfirmPasswordDialog(newPassword, isChanging);
            alertDialog.dismiss();
        });
    }

    private void showConfirmPasswordDialog(String newPassword, boolean isChanging) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(R.string.pref_parental_control_confirm_password_title);
        final EditTextDialogBinding dialogBinding = EditTextDialogBinding.inflate(getLayoutInflater());
        dialogBinding.textInput.setHint(R.string.pref_parental_control_confirm_password);
        dialogBinding.textInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(dialogBinding.getRoot());
        builder.setPositiveButton(R.string.confirm_label, null);
        builder.setNegativeButton(R.string.cancel_label, null);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String confirmPassword = dialogBinding.textInput.getText().toString();
            if (!newPassword.equals(confirmPassword)) {
                dialogBinding.textInputLayout.setError(getString(R.string.pref_parental_control_passwords_dont_match));
                Toast.makeText(requireContext(), R.string.pref_parental_control_passwords_dont_match,
                        Toast.LENGTH_SHORT).show();
            } else {
                UserPreferences.setParentalControlPassword(newPassword);
                Toast.makeText(requireContext(),
                        isChanging ? R.string.pref_parental_control_password_changed
                                : R.string.pref_parental_control_password_set,
                        Toast.LENGTH_SHORT).show();
                alertDialog.dismiss();
            }
        });
    }

    private void setupSearch() {
        SearchPreference searchPreference = findPreference("searchPreference");
        SearchConfiguration config = searchPreference.getSearchConfiguration();
        config.setActivity((AppCompatActivity) getActivity());
        config.setFragmentContainerViewId(R.id.settingsContainer);
        config.setBreadcrumbsEnabled(true);

        config.index(R.xml.preferences_user_interface)
                .addBreadcrumb(PreferenceActivity.getTitleOfPage(R.xml.preferences_user_interface));
        config.index(R.xml.preferences_playback)
                .addBreadcrumb(PreferenceActivity.getTitleOfPage(R.xml.preferences_playback));
        config.index(R.xml.preferences_downloads)
                .addBreadcrumb(PreferenceActivity.getTitleOfPage(R.xml.preferences_downloads));
        config.index(R.xml.preferences_import_export)
                .addBreadcrumb(PreferenceActivity.getTitleOfPage(R.xml.preferences_import_export));
        config.index(R.xml.preferences_autodownload)
                .addBreadcrumb(PreferenceActivity.getTitleOfPage(R.xml.preferences_downloads))
                .addBreadcrumb(R.string.automation)
                .addBreadcrumb(PreferenceActivity.getTitleOfPage(R.xml.preferences_autodownload));
        config.index(R.xml.preferences_synchronization)
                .addBreadcrumb(PreferenceActivity.getTitleOfPage(R.xml.preferences_synchronization));
        config.index(R.xml.preferences_notifications)
                .addBreadcrumb(PreferenceActivity.getTitleOfPage(R.xml.preferences_notifications));
        config.index(R.xml.feed_settings)
                .addBreadcrumb(PreferenceActivity.getTitleOfPage(R.xml.feed_settings));
        config.index(R.xml.preferences_swipe)
                .addBreadcrumb(PreferenceActivity.getTitleOfPage(R.xml.preferences_user_interface))
                .addBreadcrumb(PreferenceActivity.getTitleOfPage(R.xml.preferences_swipe));
    }
}
