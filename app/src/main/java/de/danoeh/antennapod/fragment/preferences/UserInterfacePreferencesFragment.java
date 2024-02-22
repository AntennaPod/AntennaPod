package de.danoeh.antennapod.fragment.preferences;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.PreferenceActivity;
import de.danoeh.antennapod.dialog.DrawerPreferencesDialog;
import de.danoeh.antennapod.dialog.FeedSortDialog;
import de.danoeh.antennapod.dialog.SubscriptionsFilterDialog;
import de.danoeh.antennapod.event.PlayerStatusEvent;
import de.danoeh.antennapod.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import org.greenrobot.eventbus.EventBus;

import java.util.List;

public class UserInterfacePreferencesFragment extends PreferenceFragmentCompat {
    private static final String PREF_SWIPE = "prefSwipe";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_user_interface);
        setupInterfaceScreen();
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
                    DrawerPreferencesDialog.show(getContext(), null);
                    return true;
                });

        findPreference(UserPreferences.PREF_FULL_NOTIFICATION_BUTTONS)
                .setOnPreferenceClickListener(preference -> {
                    showFullNotificationButtonsDialog();
                    return true;
                });
        findPreference(UserPreferences.PREF_FILTER_FEED)
                .setOnPreferenceClickListener((preference -> {
                    new SubscriptionsFilterDialog().show(getChildFragmentManager(), "filter");
                    return true;
                }));

        findPreference(UserPreferences.PREF_DRAWER_FEED_ORDER)
                .setOnPreferenceClickListener((preference -> {
                    FeedSortDialog.showDialog(requireContext());
                    return true;
                }));
        findPreference(PREF_SWIPE)
                .setOnPreferenceClickListener(preference -> {
                    ((PreferenceActivity) getActivity()).openScreen(R.xml.preferences_swipe);
                    return true;
                });

        if (Build.VERSION.SDK_INT >= 26) {
            findPreference(UserPreferences.PREF_EXPANDED_NOTIFICATION).setVisible(false);
        }
    }


    private void showFullNotificationButtonsDialog() {
        final Context context = getActivity();

        final List<Integer> preferredButtons = UserPreferences.getFullNotificationButtons();
        final String[] allButtonNames = context.getResources().getStringArray(
                R.array.full_notification_buttons_options);
        final int[] buttonIDs = {2, 3, 4};
        final int exactItems = 2;
        final DialogInterface.OnClickListener completeListener = (dialog, which) ->
                UserPreferences.setFullNotificationButtons(preferredButtons);
        final String title = context.getResources().getString(
                R.string.pref_full_notification_buttons_title);

        showNotificationButtonsDialog(preferredButtons, allButtonNames, buttonIDs, title,
                exactItems, completeListener
        );
    }

    private void showNotificationButtonsDialog(List<Integer> preferredButtons,
            String[] allButtonNames, int[] buttonIds, String title,
            int exactItems, DialogInterface.OnClickListener completeListener) {
        boolean[] checked = new boolean[allButtonNames.length]; // booleans default to false in java

        final Context context = getActivity();

        // Clear buttons that are not part of the setting anymore
        for (int i = preferredButtons.size() - 1; i >= 0; i--) {
            boolean isValid = false;
            for (int j = 0; j < checked.length; j++) {
                if (buttonIds[j] == preferredButtons.get(i)) {
                    isValid = true;
                }
            }

            if (!isValid) {
                preferredButtons.remove(i);
            }
        }

        for(int i=0; i < checked.length; i++) {
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
            if (preferredButtons.size() != exactItems) {
                ListView selectionView = dialog.getListView();
                Snackbar.make(
                    selectionView,
                    String.format(context.getResources().getString(
                        R.string.pref_compact_notification_buttons_dialog_error_exact), exactItems),
                    Snackbar.LENGTH_SHORT).show();

            } else {
                completeListener.onClick(dialog, AlertDialog.BUTTON_POSITIVE);
                dialog.cancel();
            }
        }
        );
    }
}
