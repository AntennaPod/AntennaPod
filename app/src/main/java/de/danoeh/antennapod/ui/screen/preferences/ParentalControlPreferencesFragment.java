package de.danoeh.antennapod.ui.screen.preferences;

import android.os.Bundle;
import android.text.InputType;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.EditTextDialogBinding;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.preferences.screen.AnimatedPreferenceFragment;


public class ParentalControlPreferencesFragment extends AnimatedPreferenceFragment {

    private static final String PREF_ENABLED = "prefParentalControlEnabled";
    private static final String PREF_REQUIRE_SUBSCRIBE = "prefParentalControlRequireSubscribe";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_parental_control);
        setupPreferences();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((PreferenceActivity) getActivity()).getSupportActionBar().setTitle(R.string.pref_parental_control_title);
    }

    private void setupPreferences() {
        SwitchPreferenceCompat enabledPref = findPreference(PREF_ENABLED);
        enabledPref.setChecked(UserPreferences.isParentalControlPasswordSet());
        enabledPref.setOnPreferenceChangeListener((pref, newValue) -> {
            if ((Boolean) newValue) {
                showSetNewPasswordDialog(false);
            } else {
                showVerifyPasswordDialog(() -> {
                    UserPreferences.clearParentalControlPassword();
                    enabledPref.setChecked(false);
                    updateRequireSubscribeEnabled();
                    Toast.makeText(requireContext(), R.string.pref_parental_control_password_cleared,
                            Toast.LENGTH_SHORT).show();
                });
            }
            return false;
        });
        updateRequireSubscribeEnabled();
    }

    private void updateRequireSubscribeEnabled() {
        SwitchPreferenceCompat requireSubscribePref = findPreference(PREF_REQUIRE_SUBSCRIBE);
        requireSubscribePref.setEnabled(UserPreferences.isParentalControlPasswordSet());
    }

    private void showVerifyPasswordDialog(Runnable onSuccess) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(R.string.pref_parental_control_title);
        builder.setMessage(R.string.pref_parental_control_enter_old_password);
        final EditTextDialogBinding dialogBinding = EditTextDialogBinding.inflate(getLayoutInflater());
        dialogBinding.textInput.setHint(R.string.pref_parental_control_old_password);
        dialogBinding.textInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(dialogBinding.getRoot());
        builder.setPositiveButton(R.string.confirm_label, null);
        builder.setNegativeButton(R.string.cancel_label, null);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String entered = dialogBinding.textInput.getText().toString();
            if (UserPreferences.verifyParentalControlPassword(entered)) {
                alertDialog.dismiss();
                onSuccess.run();
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
            alertDialog.dismiss();
            showConfirmPasswordDialog(newPassword, isChanging);
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
                dialogBinding.textInputLayout.setError(
                        getString(R.string.pref_parental_control_passwords_dont_match));
                Toast.makeText(requireContext(), R.string.pref_parental_control_passwords_dont_match,
                        Toast.LENGTH_SHORT).show();
            } else {
                UserPreferences.setParentalControlPassword(newPassword);
                Toast.makeText(requireContext(),
                        isChanging ? R.string.pref_parental_control_password_changed
                                : R.string.pref_parental_control_password_set,
                        Toast.LENGTH_SHORT).show();
                alertDialog.dismiss();
                SwitchPreferenceCompat enabledPref = findPreference(PREF_ENABLED);
                enabledPref.setChecked(true);
                updateRequireSubscribeEnabled();
            }
        });
    }
}
