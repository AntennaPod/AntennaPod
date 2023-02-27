package de.danoeh.antennapod.preferences;

import android.content.Context;
import android.util.AttributeSet;
import androidx.cardview.widget.CardView;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import com.google.android.material.elevation.SurfaceColors;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.ThemePreferenceBinding;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

public class ThemePreference extends Preference {
    ThemePreferenceBinding viewBinding;

    public ThemePreference(Context context) {
        super(context);
        setLayoutResource(R.layout.theme_preference);
    }

    public ThemePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.theme_preference);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        viewBinding = ThemePreferenceBinding.bind(holder.itemView);
        updateUi();
    }

    void updateThemeCard(CardView card, UserPreferences.ThemePreference theme) {
        float density = getContext().getResources().getDisplayMetrics().density;
        int surfaceColor = SurfaceColors.getColorForElevation(getContext(), 1 * density);
        int surfaceColorActive = SurfaceColors.getColorForElevation(getContext(), 32 * density);
        UserPreferences.ThemePreference activeTheme = UserPreferences.getTheme();
        card.setCardBackgroundColor(theme == activeTheme ? surfaceColorActive : surfaceColor);
        card.setOnClickListener(v -> {
            UserPreferences.setTheme(theme);
            if (getOnPreferenceChangeListener() != null) {
                getOnPreferenceChangeListener().onPreferenceChange(this, UserPreferences.getTheme());
            }
            updateUi();
        });
    }

    void updateUi() {
        updateThemeCard(viewBinding.themeSystemCard, UserPreferences.ThemePreference.SYSTEM);
        updateThemeCard(viewBinding.themeLightCard, UserPreferences.ThemePreference.LIGHT);
        updateThemeCard(viewBinding.themeDarkCard, UserPreferences.ThemePreference.DARK);
    }
}
