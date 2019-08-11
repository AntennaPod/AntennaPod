package de.danoeh.antennapod.fragment.preferences;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.widget.ListView;
import android.widget.Toast;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;

public class UserInterfacePreferencesFragment extends PreferenceFragmentCompat {
    private static final String PREF_EXPANDED_NOTIFICATION = "prefExpandNotify";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_user_interface);
        setupInterfaceScreen();
    }

    private void setupInterfaceScreen() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            // disable expanded notification option on unsupported android versions
            findPreference(PREF_EXPANDED_NOTIFICATION).setEnabled(false);
            findPreference(PREF_EXPANDED_NOTIFICATION).setOnPreferenceClickListener(
                    preference -> {
                        Toast toast = Toast.makeText(getActivity(),
                                R.string.pref_expand_notify_unsupport_toast, Toast.LENGTH_SHORT);
                        toast.show();
                        return true;
                    }
            );
        }
        findPreference(UserPreferences.PREF_THEME)
                .setOnPreferenceChangeListener(
                        (preference, newValue) -> {
                            Intent i = new Intent(getActivity(), MainActivity.class);
                            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    | Intent.FLAG_ACTIVITY_NEW_TASK);
                            getActivity().finish();
                            startActivity(i);
                            return true;
                        }
                );
        findPreference(UserPreferences.PREF_HIDDEN_DRAWER_ITEMS)
                .setOnPreferenceClickListener(preference -> {
                    showDrawerPreferencesDialog();
                    return true;
                });

        findPreference(UserPreferences.PREF_COMPACT_NOTIFICATION_BUTTONS)
                .setOnPreferenceClickListener(preference -> {
                    showNotificationButtonsDialog();
                    return true;
                });

        findPreference(UserPreferences.PREF_BACK_BUTTON_BEHAVIOR)
                .setOnPreferenceChangeListener((preference, newValue) -> {
                    if (newValue.equals("page")) {
                        final Context context = getActivity();
                        final String[] navTitles = context.getResources().getStringArray(R.array.back_button_go_to_pages);
                        final String[] navTags = context.getResources().getStringArray(R.array.back_button_go_to_pages_tags);
                        final String choice[] = { UserPreferences.getBackButtonGoToPage() };

                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle(R.string.back_button_go_to_page_title);
                        builder.setSingleChoiceItems(navTitles, ArrayUtils.indexOf(navTags, UserPreferences.getBackButtonGoToPage()), (dialogInterface, i) -> {
                            if (i >= 0) {
                                choice[0] = navTags[i];
                            }
                        });
                        builder.setPositiveButton(R.string.confirm_label, (dialogInterface, i) -> UserPreferences.setBackButtonGoToPage(choice[0]));
                        builder.setNegativeButton(R.string.cancel_label, null);
                        builder.create().show();
                        return true;
                    } else {
                        return true;
                    }
                });

        if (Build.VERSION.SDK_INT >= 26) {
            findPreference(UserPreferences.PREF_EXPANDED_NOTIFICATION).setVisible(false);
        }
    }

    private void showDrawerPreferencesDialog() {
        final Context context = getActivity();
        final List<String> hiddenDrawerItems = UserPreferences.getHiddenDrawerItems();
        final String[] navTitles = context.getResources().getStringArray(R.array.nav_drawer_titles);
        final String[] NAV_DRAWER_TAGS = MainActivity.NAV_DRAWER_TAGS;
        boolean[] checked = new boolean[MainActivity.NAV_DRAWER_TAGS.length];
        for(int i=0; i < NAV_DRAWER_TAGS.length; i++) {
            String tag = NAV_DRAWER_TAGS[i];
            if(!hiddenDrawerItems.contains(tag)) {
                checked[i] = true;
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.drawer_preferences);
        builder.setMultiChoiceItems(navTitles, checked, (dialog, which, isChecked) -> {
            if (isChecked) {
                hiddenDrawerItems.remove(NAV_DRAWER_TAGS[which]);
            } else {
                hiddenDrawerItems.add(NAV_DRAWER_TAGS[which]);
            }
        });
        builder.setPositiveButton(R.string.confirm_label, (dialog, which) ->
                UserPreferences.setHiddenDrawerItems(hiddenDrawerItems));
        builder.setNegativeButton(R.string.cancel_label, null);
        builder.create().show();
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

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
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
