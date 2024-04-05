package de.danoeh.antennapod.ui.screen.home.settingsdialog;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.ui.screen.home.HomeFragment;

public class HomePreferences {
    private static final String PREF_HIDDEN_SECTIONS = "PrefHomeSectionsString";
    private static final String PREF_SECTION_ORDER = "PrefHomeSectionOrder";
    private static HashMap<String, String> sectionTagToName;

    public static String getNameFromTag(Context context, String sectionTag) {
        if (sectionTagToName == null) {
            initializeMap(context);
        }

        return sectionTagToName.get(sectionTag);
    }

    private static void initializeMap(Context context) {
        Resources resources = context.getResources();
        String[] sectionLabels = resources.getStringArray(R.array.home_section_titles);
        String[] sectionTags = resources.getStringArray(R.array.home_section_tags);

        sectionTagToName = new HashMap<>(sectionTags.length);

        for (int i = 0; i < sectionLabels.length; i++) {
            String label = sectionLabels[i];
            String tag = sectionTags[i];

            sectionTagToName.put(tag, label);
        }
    }

    public static List<String> getHiddenSectionTags(Context context) {
        return getListPreference(context, PREF_HIDDEN_SECTIONS);
    }

    public static List<String> getSortedSectionTags(Context context) {
        List<String> sectionTagOrder = getListPreference(context, PREF_SECTION_ORDER);
        List<String> hiddenSectionTags = getHiddenSectionTags(context);
        String[] sectionTags = context.getResources().getStringArray(R.array.home_section_tags);
        Arrays.sort(sectionTags, (String a, String b) -> Integer.signum(
                indexOfOrMaxValue(sectionTagOrder, a) - indexOfOrMaxValue(sectionTagOrder, b)));

        List<String> finalSectionTags = new ArrayList<>();
        for (String sectionTag: sectionTags) {
            if (hiddenSectionTags.contains(sectionTag)) {
                continue;
            }

            finalSectionTags.add(sectionTag);
        }

        return finalSectionTags;
    }

    private static List<String> getListPreference(Context context, String preferenceKey) {
        SharedPreferences prefs = context.getSharedPreferences(HomeFragment.PREF_NAME, Context.MODE_PRIVATE);
        String hiddenSectionsString = prefs.getString(preferenceKey, "");
        return new ArrayList<>(Arrays.asList(TextUtils.split(hiddenSectionsString, ",")));
    }

    private static int indexOfOrMaxValue(List<String> haystack, String needle) {
        int index = haystack.indexOf(needle);
        return index == -1 ? Integer.MAX_VALUE : index;
    }

    public static void saveChanges(Context context, List<String> hiddenSections, List<String> sectionOrder) {
        SharedPreferences prefs = context.getSharedPreferences(HomeFragment.PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(PREF_HIDDEN_SECTIONS, TextUtils.join(",", hiddenSections));
        edit.putString(PREF_SECTION_ORDER, TextUtils.join(",", sectionOrder));
        edit.apply();
    }
}
