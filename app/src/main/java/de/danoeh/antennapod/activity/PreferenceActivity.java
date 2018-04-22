package de.danoeh.antennapod.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
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

    public static final String PARAM_RESOURCE = "resource";
    private static WeakReference<PreferenceActivity> instance;
    private PreferenceController preferenceController;
    private final PreferenceController.PreferenceUI preferenceUI = new PreferenceController.PreferenceUI() {
        private PreferenceFragment fragment;

        @Override
        public void setFragment(PreferenceFragment fragment) {
            this.fragment = fragment;
        }

        @Override
        public Preference findPreference(CharSequence key) {
            return fragment.findPreference(key);
        }

        @Override
        public PreferenceScreen getPreferenceScreen() {
            return fragment.getPreferenceScreen();
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

        PreferenceFragment prefFragment = new MainFragment();
        preferenceUI.setFragment(prefFragment);
        Bundle args = new Bundle();
        args.putInt(PARAM_RESOURCE, R.xml.preferences);
        prefFragment.setArguments(args);
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
                if (getFragmentManager().getBackStackEntryCount() == 0) {
                    finish();
                } else {
                    getFragmentManager().popBackStack();
                }
                return true;
            default:
                return false;
        }
    }

    public static class MainFragment extends PreferenceFragment {
        private int screen;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
            screen = getArguments().getInt(PARAM_RESOURCE);
            addPreferencesFromResource(screen);
            PreferenceActivity activity = instance.get();
            if (activity != null && activity.preferenceController != null) {
                activity.preferenceUI.setFragment(this);
                activity.preferenceController.onCreate(screen);
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            PreferenceActivity activity = instance.get();
            if(activity != null && activity.preferenceController != null) {
                activity.setTitle(getTitle(screen));
                activity.preferenceUI.setFragment(this);
                activity.preferenceController.onResume(screen);
            }
        }

        private int getTitle(int preferences) {
            switch (preferences) {
                case R.xml.preferences_network:
                    return R.string.network_pref;
                case R.xml.preferences_autodownload:
                    return R.string.pref_automatic_download_title;
                case R.xml.preferences_playback:
                    return R.string.playback_pref;
                case R.xml.preferences_storage:
                    return R.string.storage_pref;
                case R.xml.preferences_user_interface:
                    return R.string.user_interface_label;
                case R.xml.preferences_integrations:
                    return R.string.integrations_label;
                default:
                    return R.string.settings_label;
            }
        }

        @Override
        public void onPause() {
            PreferenceActivity activity = instance.get();
            if (screen == R.xml.preferences_integrations) {
                activity.preferenceController.unregisterGpodnet();
            }
            super.onPause();
        }

        @Override
        public void onStop() {
            PreferenceActivity activity = instance.get();
            if (screen == R.xml.preferences_storage) {
                activity.preferenceController.unsubscribeExportSubscription();
            }
            super.onStop();
        }
    }
}
