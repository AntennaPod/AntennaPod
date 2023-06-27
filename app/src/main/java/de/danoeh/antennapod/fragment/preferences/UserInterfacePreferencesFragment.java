package de.danoeh.antennapod.fragment.preferences;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
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

        findPreference(UserPreferences.PREF_COMPACT_NOTIFICATION_BUTTONS)
                .setOnPreferenceClickListener(preference -> {
                    showNotificationButtonsDialog();
                    return true;
                });
        findPreference(UserPreferences.PREF_FILTER_FEED)
                .setOnPreferenceClickListener((preference -> {
                    SubscriptionsFilterDialog.showDialog(requireContext());
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

    private void showNotificationButtonsDialog() {
        final Context context = getActivity();
        final List<Integer> preferredButtons = UserPreferences.getCompactNotificationButtons();
        final String[] allButtonNames = context.getResources().getStringArray(
                R.array.compact_notification_buttons_options);
        boolean[] checked = new boolean[allButtonNames.length]; // booleans default to false in java

        for(int i=0; i < checked.length; i++) {
            if(preferredButtons.contains(i)) {
                checked[i] = true;
            }
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(String.format(context.getResources().getString(
                R.string.pref_compact_notification_buttons_dialog_title), 2));
        builder.setMultiChoiceItems(allButtonNames, checked, (dialog, which, isChecked) -> {
            checked[which] = isChecked;

            if (isChecked) {
                if (preferredButtons.size() < 2) {
                    preferredButtons.add(which);
                } else {
                    // Only allow a maximum of two selections. This is because the notification
                    // on the lock screen can only display 3 buttons, and the play/pause button
                    // is always included.
                    checked[which] = false;
                    ListView selectionView = ((AlertDialog) dialog).getListView();
                    selectionView.setItemChecked(which, false);
                    Snackbar.make(
                            selectionView,
                            String.format(context.getResources().getString(
                                    R.string.pref_compact_notification_buttons_dialog_error), 2),
                            Snackbar.LENGTH_SHORT).show();
                }
            } else {
                preferredButtons.remove((Integer) which);
            }
        });
        builder.setPositiveButton(R.string.confirm_label, (dialog, which) ->
                UserPreferences.setCompactNotificationButtons(preferredButtons));
        builder.setNegativeButton(R.string.cancel_label, null);
        builder.create().show();
    }
}
