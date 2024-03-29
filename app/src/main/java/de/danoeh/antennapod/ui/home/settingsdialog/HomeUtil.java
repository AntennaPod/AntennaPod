package de.danoeh.antennapod.ui.home.settingsdialog;

import android.content.Context;
import android.content.res.Resources;

import java.util.HashMap;

import de.danoeh.antennapod.R;

public class HomeUtil {

    private static HashMap<String, String> sectionTagToName;
    private static HashMap<String, String> sectionNameToTag;

    public static String getSectionTagFromName(Context context, String sectionName)
    {
        if(sectionNameToTag == null)
            initializeMaps(context);

        return sectionNameToTag.get(sectionName);
    }

    public static String getNameFromTag(Context context, String sectionTag)
    {
        if(sectionNameToTag == null)
            initializeMaps(context);

        return sectionTagToName.get(sectionTag);
    }

    private static void initializeMaps(Context context)
    {
        Resources resources = context.getResources();
        String[] sectionLabels = resources.getStringArray(R.array.home_section_titles);
        String[] sectionTags = resources.getStringArray(R.array.home_section_tags);

        sectionTagToName = new HashMap<>(sectionTags.length);
        sectionNameToTag = new HashMap<>(sectionTags.length);

        for (int i = 0; i < sectionLabels.length; i++) {
            String label = sectionLabels[i];
            String tag = sectionTags[i];

            sectionTagToName.put(tag, label);
            sectionNameToTag.put(label, tag);
        }
    }
}
