package de.danoeh.antennapod.ui.screen.preferences;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.preference.Preference;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import de.danoeh.antennapod.storage.preferences.UsageStatistics;
import de.danoeh.antennapod.ui.preferences.screen.AnimatedPreferenceFragment;
import de.danoeh.antennapod.ui.screen.subscriptions.FeedSortDialog;
import org.greenrobot.eventbus.EventBus;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.ui.screen.drawer.DrawerPreferencesDialog;
import de.danoeh.antennapod.ui.screen.subscriptions.SubscriptionsFilterDialog;
import de.danoeh.antennapod.event.PlayerStatusEvent;
import de.danoeh.antennapod.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

public class UserInterfacePreferencesFragment extends AnimatedPreferenceFragment {
    private static final String PREF_SWIPE = "prefSwipe";

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
            if (newValue instanceof Boolean) {
                backOpensDrawerToggle((Boolean) newValue);
            }
            return true;
        });
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
}
