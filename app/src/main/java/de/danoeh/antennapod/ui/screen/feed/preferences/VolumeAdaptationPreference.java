package de.danoeh.antennapod.ui.screen.feed.preferences;

import android.content.Context;
import android.util.AttributeSet;

import java.util.Arrays;

import de.danoeh.antennapod.model.feed.VolumeAdaptionSetting;
import de.danoeh.antennapod.ui.preferences.preference.MaterialListPreference;

public class VolumeAdaptationPreference extends MaterialListPreference {
    public VolumeAdaptationPreference(Context context) {
        super(context);
    }

    public VolumeAdaptationPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public CharSequence[] getEntries() {
        if (VolumeAdaptionSetting.isBoostSupported()) {
            return super.getEntries();
        } else {
            return Arrays.copyOfRange(super.getEntries(), 0, 3);
        }
    }
}
