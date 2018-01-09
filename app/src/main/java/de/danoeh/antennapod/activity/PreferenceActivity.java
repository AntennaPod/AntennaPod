package de.danoeh.antennapod.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.lang.ref.WeakReference;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.preferences.PreferenceController;

/**
 * PreferenceActivity for API 11+. In order to change the behavior of the preference UI, see
 * PreferenceController.
 */
public class PreferenceActivity extends AppCompatActivity {

    private static WeakReference<PreferenceActivity> instance;
    private PreferenceController preferenceController;
    private MainFragment prefFragment;
    private final PreferenceController.PreferenceUI preferenceUI = new PreferenceController.PreferenceUI() {
        @Override
        public Preference findPreference(CharSequence key) {
            return prefFragment.findPreference(key);
        }

        @Override
        public Activity getActivity() {
            return PreferenceActivity.this;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // This must be the FIRST thing we do, otherwise other code may not have the
        // reference it needs
        instance = new WeakReference<>(this);

        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        // set up layout
        FrameLayout root = new FrameLayout(this);
        root.setId(R.id.content);
        root.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        // we need to create the PreferenceController before the MainFragment
        // since the MainFragment depends on the preferenceController already being created
        preferenceController = new PreferenceController(preferenceUI);

        prefFragment = new MainFragment();
        getFragmentManager().beginTransaction().replace(R.id.content, prefFragment).commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        preferenceController.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return false;
        }
    }

    public static class MainFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
            addPreferencesFromResource(R.xml.preferences);
            PreferenceActivity activity = instance.get();
            if(activity != null && activity.preferenceController != null) {
                activity.preferenceController.onCreate();
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            PreferenceActivity activity = instance.get();
            if(activity != null && activity.preferenceController != null) {
                activity.preferenceController.onResume();
            }
        }

        @Override
        public void onPause() {
            PreferenceActivity activity = instance.get();
            if(activity != null && activity.preferenceController != null) {
                activity.preferenceController.onPause();
            }
            super.onPause();
        }

        @Override
        public void onStop() {
            PreferenceActivity activity = instance.get();
            if(activity != null && activity.preferenceController != null) {
                activity.preferenceController.onStop();
            }
            super.onStop();
        }
    }
}
