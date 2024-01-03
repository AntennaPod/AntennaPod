package de.danoeh.antennapod.ui.home;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatCheckedTextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Arrays;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.fragment.EpisodesFragementInHome;
import de.danoeh.antennapod.fragment.InboxFragmentInHome;

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

            ListView listView = ((AlertDialog) dialog).getListView();
            int cutoff = (EpisodesFragementInHome.TAG.equals(sectionTags[which])
                    || InboxFragmentInHome.TAG.equals(sectionTags[which])) ? 3 : 2;
            toggleExpanableSections(listView, sectionTags, hiddenSections, cutoff);
        });
        https://stackoverflow.com/questions/39053333/disable-checkbox-items-in-alertdialog
        builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> {
            SharedPreferences prefs = context.getSharedPreferences(HomeFragment.PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(HomeFragment.PREF_HIDDEN_SECTIONS, TextUtils.join(",", hiddenSections)).apply();
            onSettingsChanged.onClick(dialog, which);
        });
        builder.setNegativeButton(R.string.cancel_label, null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            toggleExpanableSections(dialog.getListView(), sectionTags, hiddenSections, 2);
        });
        dialog.show();
    }

    private static void toggleExpanableSections(ListView listView, String[] sectionTags, List<String> hiddenSections, int cutoff) {
        int episodesIndex = Arrays.asList(sectionTags).indexOf(EpisodesFragementInHome.TAG);
        int inboxIndex = Arrays.asList(sectionTags).indexOf(InboxFragmentInHome.TAG);

        /*boolean notNew = hiddenSections.contains(InboxSection.TAG)
                && hiddenSections.contains(EpisodesSurpriseSection.TAG);*/
        if (!hiddenSections.contains(InboxFragmentInHome.TAG)) { //only one
            listView.getChildAt(episodesIndex).setEnabled(false);
            ((AppCompatCheckedTextView) listView.getChildAt(episodesIndex)).setChecked(false);
        } else if (!hiddenSections.contains(EpisodesFragementInHome.TAG)) {
            listView.getChildAt(inboxIndex).setEnabled(false);
            ((AppCompatCheckedTextView) listView.getChildAt(inboxIndex)).setChecked(false);
        } else if (sectionTags.length-hiddenSections.size() < cutoff /*&& notNewNorSuprise*/) {
            listView.getChildAt(episodesIndex).setEnabled(true);
            listView.getChildAt(inboxIndex).setEnabled(true);
        } else {
            listView.getChildAt(episodesIndex).setEnabled(false);
            listView.getChildAt(inboxIndex).setEnabled(false);
            //TODO make clickable
        }
    }
}
