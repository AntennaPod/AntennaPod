package de.danoeh.antennapod.ui.home;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatCheckedTextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.fragment.EpisodesFragementInHome;
import de.danoeh.antennapod.fragment.InboxFragmentInHome;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class HomeSectionsSettingsDialog {
    public static void open(Context context, DialogInterface.OnClickListener onSettingsChanged) {
        final List<String> hiddenSections = HomeFragment.getHiddenSections(context);
        String[] sectionLabels = context.getResources().getStringArray(R.array.home_section_titles);
        String[] sectionTags = context.getResources().getStringArray(R.array.home_section_tags);
        final boolean[] checked = new boolean[sectionLabels.length];
        for (int i = 0; i < sectionLabels.length; i++) {
            String tag = sectionTags[i];
            if (!hiddenSections.contains(tag)) {
                checked[i] = true;
            }
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(R.string.configure_home);
        builder.setMultiChoiceItems(sectionLabels, checked, (dialog, which, isChecked) -> {
            if (isChecked) {
                hiddenSections.remove(sectionTags[which]);
            } else {
                hiddenSections.add(sectionTags[which]);
            }

            int episodesIndex = Arrays.asList(sectionTags).indexOf("Episodes");
            int inboxIndex = Arrays.asList(sectionTags).indexOf("Inbox");
            ListView listView = ((AlertDialog) dialog).getListView();
            boolean notNewNorSuprise = !EpisodesFragementInHome.TAG.equals(sectionTags[which])
                    && !InboxFragmentInHome.TAG.equals(sectionTags[which]);
            if (sectionTags.length-hiddenSections.size() < 2 && notNewNorSuprise) {
                ((View) listView.getChildAt(episodesIndex)).setEnabled(true);
                ((View) listView.getChildAt(inboxIndex)).setEnabled(true);
                ((AppCompatCheckedTextView) listView.getChildAt(episodesIndex)).setClickable(true);
                ((AppCompatCheckedTextView) listView.getChildAt(inboxIndex)).setClickable(true);
            } else {
                ((View) listView.getChildAt(episodesIndex)).setEnabled(false);
                ((View) listView.getChildAt(inboxIndex)).setEnabled(false);
                ((AppCompatCheckedTextView) listView.getChildAt(episodesIndex)).setClickable(false);
                ((AppCompatCheckedTextView) listView.getChildAt(inboxIndex)).setClickable(false);
            }
        });
        https://stackoverflow.com/questions/39053333/disable-checkbox-items-in-alertdialog
        builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> {
            SharedPreferences prefs = context.getSharedPreferences(HomeFragment.PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(HomeFragment.PREF_HIDDEN_SECTIONS, TextUtils.join(",", hiddenSections)).apply();
            onSettingsChanged.onClick(dialog, which);
        });
        builder.setNegativeButton(R.string.cancel_label, null);
        builder.create().show();
    }
}
