package de.danoeh.antennapodSA.preferences;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreference;

import de.danoeh.antennapodSA.R;

public class MasterSwitchPreference extends SwitchPreference {

    public MasterSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
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

        TypedValue typedValue = new TypedValue();
        getContext().getTheme().resolveAttribute(R.attr.master_switch_background, typedValue, true);
        holder.itemView.setBackgroundColor(typedValue.data);

        TextView title = (TextView) holder.findViewById(android.R.id.title);
        if (title != null) {
            title.setTypeface(title.getTypeface(), Typeface.BOLD);
        }
    }
}