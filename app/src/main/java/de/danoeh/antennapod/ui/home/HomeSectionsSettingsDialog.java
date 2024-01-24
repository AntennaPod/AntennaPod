package de.danoeh.antennapod.ui.home;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatCheckedTextView;
import androidx.appcompat.widget.AppCompatSpinner;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.HomeSettingsDialogBinding;
import de.danoeh.antennapod.fragment.EpisodesFragementInHome;
import de.danoeh.antennapod.fragment.InboxFragmentInHome;

public class HomeSectionsSettingsDialog {
    public static void open(Activity activity, DialogInterface.OnClickListener onSettingsChanged) {
        final List<String> hiddenSections = HomeFragment.getHiddenSections(activity);
        String[] sectionLabels = activity.getResources().getStringArray(R.array.home_section_titles);
        String[] bottomHalfLabels = activity.getResources().getStringArray(R.array.home_bottomhalf_titles);
        String[] bottomHalfTags = activity.getResources().getStringArray(R.array.home_bottomhalf_tags);
        String[] sectionTags = ArrayUtils.addAll(
                activity.getResources().getStringArray(R.array.home_section_tags),
                bottomHalfTags);
        final boolean[] checked = new boolean[sectionLabels.length];
        for (int i = 0; i < sectionLabels.length; i++) {
            String tag = sectionTags[i];
            if (!hiddenSections.contains(tag)) {
                checked[i] = true;
            }
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
        builder.setTitle(R.string.configure_home);
        View view = activity.getLayoutInflater().inflate(R.layout.home_settings_dialog, null);
        HomeSettingsDialogBinding binding = HomeSettingsDialogBinding.bind(view);
        MaterialAutoCompleteTextView bottomhalfDropdown = binding.bottomhalfDropdown;
        bottomhalfDropdown.setSimpleItems(bottomHalfLabels);
        bottomhalfDropdown.setOnItemClickListener((adapterView, view1, i, l) -> {
            for (int i1 = 0; i1 < bottomHalfTags.length; i1++) {
                if (!hiddenSections.contains(bottomHalfTags[i1])) {
                    hiddenSections.add(bottomHalfTags[i1]);
                }
            }
            hiddenSections.remove(bottomHalfTags[i]);
        });
        for (int i = 0; i < sectionLabels.length; i++) {
            MaterialCheckBox materialCheckBox = new MaterialCheckBox(view.getContext());
            materialCheckBox.setChecked(checked[i]);
            materialCheckBox.setText(sectionLabels[i]);
            String tag = sectionTags[i];
            materialCheckBox.setOnCheckedChangeListener((compoundButton, isChecked) -> {
                if (isChecked) {
                    hiddenSections.remove(tag);
                } else {
                    hiddenSections.add(tag);
                }
                toggleExpanableSections(binding, sectionTags, hiddenSections, bottomHalfTags);
                for (int i1 = 0; i1 < bottomHalfTags.length; i1++) {
                    if (!hiddenSections.contains(bottomHalfTags[i1])) {
                        hiddenSections.add(bottomHalfTags[i1]);
                    }
                }
            });
            binding.homeSettingsLinearlayout.addView(materialCheckBox, i);
        }
        builder.setView(view);
        builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> {
            SharedPreferences prefs = activity.getSharedPreferences(HomeFragment.PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(HomeFragment.PREF_HIDDEN_SECTIONS, TextUtils.join(",", hiddenSections)).apply();
            Log.d("ufuefh", hiddenSections.toString());
            onSettingsChanged.onClick(dialog, which);
        });
        builder.setNegativeButton(R.string.cancel_label, null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            toggleExpanableSections(binding, sectionTags, hiddenSections, bottomHalfTags);
            for (int i = 0; i < bottomHalfTags.length; i++) {
                if (!hiddenSections.contains(bottomHalfTags[i])) {
                    binding.bottomhalfDropdown.setText(bottomHalfLabels[i], false);
                }
            }
        });
        dialog.show();
    }

    private static void toggleExpanableSections(HomeSettingsDialogBinding binding, String[] sectionTags, List hiddenTags, String[] bottomhalfTags) {
        int offset = 0;
        for (int i = 0; i < bottomhalfTags.length; i++) {
            if (!hiddenTags.contains(bottomhalfTags[i])) offset++;
        }
        int selectedSections = sectionTags.length-hiddenTags.size();
        if (selectedSections < 2 + offset) {
            binding.bottomhalfDropdown.setEnabled(true);
            binding.bottomhalfInput.setEnabled(true);
        } else {
            binding.bottomhalfDropdown.setEnabled(false);
            binding.bottomhalfInput.setEnabled(false);
        }
    }
}
