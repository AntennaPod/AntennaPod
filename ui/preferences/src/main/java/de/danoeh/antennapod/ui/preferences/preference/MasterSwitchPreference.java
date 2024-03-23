package de.danoeh.antennapod.ui.preferences.preference;

import android.content.Context;
import android.graphics.Typeface;
import androidx.preference.SwitchPreferenceCompat;
import androidx.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.TextView;

import de.danoeh.antennapod.ui.common.ThemeUtils;
import de.danoeh.antennapod.ui.preferences.R;

public class MasterSwitchPreference extends SwitchPreferenceCompat {

    public MasterSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MasterSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public MasterSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MasterSwitchPreference(Context context) {
        super(context);
    }


    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        holder.itemView.setBackgroundColor(ThemeUtils.getColorFromAttr(getContext(), R.attr.colorSurfaceVariant));
        TextView title = (TextView) holder.findViewById(android.R.id.title);
        if (title != null) {
            title.setTypeface(title.getTypeface(), Typeface.BOLD);
        }
    }
}