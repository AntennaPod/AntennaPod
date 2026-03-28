package de.danoeh.antennapod.ui.screen.preferences;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.Handler;
import android.text.InputType;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.preference.Preference;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.EditTextDialogBinding;
import de.danoeh.antennapod.storage.preferences.ParentalControlPassword;
import de.danoeh.antennapod.event.PlayerStatusEvent;
import de.danoeh.antennapod.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.storage.preferences.UsageStatistics;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.preferences.screen.AnimatedPreferenceFragment;
import de.danoeh.antennapod.ui.screen.drawer.DrawerPreferencesDialog;
import de.danoeh.antennapod.ui.screen.subscriptions.EpisodeListGlobalDefaultSortDialog;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.List;

public class UserInterfacePreferencesFragment extends AnimatedPreferenceFragment {
    private static final String PREF_SWIPE = "prefSwipe";
    private static final String PREF_PARENTAL_CONTROL_PASSWORD = "prefParentalControlPassword";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_user_interface);
        setupInterfaceScreen();
        backOpensDrawerToggle(UserPreferences.isBottomNavigationEnabled());
    }

    @Override
    public void onStart() {
        super.onStart();
        ((PreferenceActivity) getActivity()).getSupportActionBar().setTitle(R.string.user_interface_label);
    }

    private void setupInterfaceScreen() {
        Preference.OnPreferenceChangeListener restartApp = (preference, newValue) -> {
            ActivityCompat.recreate(getActivity());
            return true;
        };
        findPreference(UserPreferences.PREF_THEME).setOnPreferenceChangeListener(restartApp);
        findPreference(UserPreferences.PREF_THEME_BLACK).setOnPreferenceChangeListener(restartApp);
        findPreference(UserPreferences.PREF_TINTED_COLORS).setOnPreferenceChangeListener(restartApp);
        if (Build.VERSION.SDK_INT < 31) {
            findPreference(UserPreferences.PREF_TINTED_COLORS).setVisible(false);
        }

        findPreference(UserPreferences.PREF_SHOW_TIME_LEFT)
                .setOnPreferenceChangeListener(
                        (preference, newValue) -> {
                            UserPreferences.setShowRemainTimeSetting((Boolean) newValue);
                            EventBus.getDefault().post(new UnreadItemsUpdateEvent());
                            EventBus.getDefault().post(new PlayerStatusEvent());
                            return true;
                        });

        findPreference(UserPreferences.PREF_HIDDEN_DRAWER_ITEMS)
                .setOnPreferenceClickListener(preference -> {
                    new DrawerPreferencesDialog(getContext(), null).show();
                    return true;
                });

        findPreference(UserPreferences.PREF_FULL_NOTIFICATION_BUTTONS)
                .setOnPreferenceClickListener(preference -> {
                    showFullNotificationButtonsDialog();
                    return true;
                });
        findPreference(UserPreferences.PREF_GLOBAL_DEFAULT_SORTED_ORDER)
                .setOnPreferenceClickListener((preference -> {
                    EpisodeListGlobalDefaultSortDialog dialog = EpisodeListGlobalDefaultSortDialog.newInstance();
                    dialog.show(getChildFragmentManager(), "SortDialog");
                    return true;
                }));
        findPreference(PREF_SWIPE)
                .setOnPreferenceClickListener(preference -> {
                    ((PreferenceActivity) getActivity()).openScreen(R.xml.preferences_swipe);
                    return true;
                });
        findPreference(UserPreferences.PREF_STREAM_OVER_DOWNLOAD)
                .setOnPreferenceChangeListener((preference, newValue) -> {
                    // Update all visible lists to reflect new streaming action button
                    EventBus.getDefault().post(new UnreadItemsUpdateEvent());
                    // User consciously decided whether to prefer the streaming button, disable suggestion
                    UsageStatistics.doNotAskAgain(UsageStatistics.ACTION_STREAM);
                    return true;
                });

        if (Build.VERSION.SDK_INT >= 26) {
            findPreference(UserPreferences.PREF_EXPANDED_NOTIFICATION).setVisible(false);
        }

        findPreference(UserPreferences.PREF_BOTTOM_NAVIGATION).setOnPreferenceChangeListener((preference, newValue) -> {
            if (newValue instanceof Boolean && !(Boolean) newValue) {
                new MaterialAlertDialogBuilder(getContext())
                        .setMessage(R.string.bottom_navigation_deprecation_warning)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
            if (newValue instanceof Boolean) {
                backOpensDrawerToggle((Boolean) newValue);
            }
            return true;
        });

        findPreference(PREF_PARENTAL_CONTROL_PASSWORD).setOnPreferenceClickListener(preference -> {
            showChangePasswordDialog();
            return true;
        });

        // only show the 'parental password' dialog if we're on a 'child' device (family link)
        findPreference(PREF_PARENTAL_CONTROL_PASSWORD).setVisible(false);
        AccountManager am = AccountManager.get(requireContext());
        Account[] accounts = am.getAccountsByType("com.google");
        for (Account account : accounts) {
            am.hasFeatures(account, new String[]{"child"}, new AccountManagerCallback<Boolean>() {
                @Override
                public void run(AccountManagerFuture<Boolean> future) {
                    try {
                        boolean isChild = future.getResult();
                        findPreference(PREF_PARENTAL_CONTROL_PASSWORD).setVisible(isChild);
                    } catch (AuthenticatorException | OperationCanceledException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }, new Handler(Looper.getMainLooper()));
        }
    }

    private void backOpensDrawerToggle(boolean bottomNavigationEnabled) {
        findPreference(UserPreferences.PREF_BACK_OPENS_DRAWER).setEnabled(!bottomNavigationEnabled);
    }

    private void showFullNotificationButtonsDialog() {
        final Context context = getActivity();

        final List<Integer> preferredButtons = UserPreferences.getFullNotificationButtons();
        final String[] allButtonNames = context.getResources().getStringArray(
                R.array.full_notification_buttons_options);
        final int[] buttonIds = {
                UserPreferences.NOTIFICATION_BUTTON_SKIP,
                UserPreferences.NOTIFICATION_BUTTON_NEXT_CHAPTER,
                UserPreferences.NOTIFICATION_BUTTON_PLAYBACK_SPEED,
                UserPreferences.NOTIFICATION_BUTTON_SLEEP_TIMER,
        };
        final DialogInterface.OnClickListener completeListener = (dialog, which) ->
                UserPreferences.setFullNotificationButtons(preferredButtons);
        final String title = context.getResources().getString(R.string.pref_full_notification_buttons_title);

        boolean[] checked = new boolean[allButtonNames.length]; // booleans default to false in java

        // Clear buttons that are not part of the setting anymore
        for (int i = preferredButtons.size() - 1; i >= 0; i--) {
            boolean isValid = false;
            for (int j = 0; j < checked.length; j++) {
                if (buttonIds[j] == preferredButtons.get(i)) {
                    isValid = true;
                    break;
                }
            }

            if (!isValid) {
                preferredButtons.remove(i);
            }
        }

        for (int i = 0; i < checked.length; i++) {
            if (preferredButtons.contains(buttonIds[i])) {
                checked[i] = true;
            }
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(title);
        builder.setMultiChoiceItems(allButtonNames, checked, (dialog, which, isChecked) -> {
            checked[which] = isChecked;
            if (isChecked) {
                preferredButtons.add(buttonIds[which]);
            } else {
                preferredButtons.remove((Integer) buttonIds[which]);
            }
        });
        builder.setPositiveButton(R.string.confirm_label, null);
        builder.setNegativeButton(R.string.cancel_label, null);
        final AlertDialog dialog = builder.create();

        dialog.show();

        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

        positiveButton.setOnClickListener(v -> {
            if (preferredButtons.size() != 2) {
                ListView selectionView = dialog.getListView();
                Snackbar.make(
                    selectionView,
                    context.getResources().getString(R.string.pref_compact_notification_buttons_dialog_error_exact),
                    Snackbar.LENGTH_SHORT).show();

            } else {
                completeListener.onClick(dialog, AlertDialog.BUTTON_POSITIVE);
                dialog.cancel();
            }
        });
    }

    private void showChangePasswordDialog() {
        if (ParentalControlPassword.isPasswordSet(requireContext())) {
            // Password is set, need to verify old password first
            showVerifyOldPasswordDialog();
        } else {
            // No password set, go directly to setting new password
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
            if (ParentalControlPassword.verifyPassword(requireContext(), oldPassword)) {
                alertDialog.dismiss();
                showSetNewPasswordDialog(true);
            } else {
                dialogBinding.textInputLayout.setError(getString(R.string.wrong_password));
                Toast.makeText(requireContext(), R.string.wrong_password, Toast.LENGTH_SHORT).show();
            }
        });

        alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            String oldPassword = dialogBinding.textInput.getText().toString();
            if (ParentalControlPassword.verifyPassword(requireContext(), oldPassword)) {
                ParentalControlPassword.clearPassword(requireContext());
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
            // Show confirmation dialog
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
                ParentalControlPassword.setPassword(requireContext(), newPassword);
                Toast.makeText(requireContext(),
                        isChanging ? R.string.pref_parental_control_password_changed
                                : R.string.pref_parental_control_password_set,
                        Toast.LENGTH_SHORT).show();
                alertDialog.dismiss();
            }
        });
    }
}
