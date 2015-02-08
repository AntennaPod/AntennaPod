package de.danoeh.antennapod.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources.Theme;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.preferences.PreferenceController;

/**
 * PreferenceActivity for API 10. In order to change the behavior of the preference UI, see
 * PreferenceController.
 */
public class PreferenceActivityGingerbread extends android.preference.PreferenceActivity {
    private static final String TAG = "PreferenceActivity";

    private PreferenceController preferenceController;

    private final PreferenceController.PreferenceUI preferenceUI = new PreferenceController.PreferenceUI() {

        @SuppressWarnings("deprecation")
        @Override
        public Preference findPreference(CharSequence key) {
            return PreferenceActivityGingerbread.this.findPreference(key);
        }

        @Override
        public Activity getActivity() {
            return PreferenceActivityGingerbread.this;
        }
    };

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
        preferenceController = new PreferenceController(preferenceUI);
        preferenceController.onCreate();
    }


    @Override
    protected void onResume() {
        super.onResume();
        preferenceController.onResume();
    }

    @Override
    protected void onApplyThemeResource(Theme theme, int resid, boolean first) {
        theme.applyStyle(UserPreferences.getTheme(), true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        preferenceController.onActivityResult(requestCode, resultCode, data);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         Preference preference) {
        super.onPreferenceTreeClick(preferenceScreen, preference);
        if (preference != null)
            if (preference instanceof PreferenceScreen)
                if (((PreferenceScreen) preference).getDialog() != null)
                    ((PreferenceScreen) preference)
                            .getDialog()
                            .getWindow()
                            .getDecorView()
                            .setBackgroundDrawable(
                                    this.getWindow().getDecorView()
                                            .getBackground().getConstantState()
                                            .newDrawable()
                            );
        return false;
    }
}
