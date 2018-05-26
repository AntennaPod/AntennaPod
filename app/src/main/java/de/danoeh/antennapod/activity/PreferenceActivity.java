package de.danoeh.antennapod.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.lang.ref.WeakReference;

import com.bytehamster.lib.preferencesearch.SearchPreference;
import com.bytehamster.lib.preferencesearch.SearchPreferenceResult;
import com.bytehamster.lib.preferencesearch.SearchPreferenceResultListener;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.preferences.PreferenceController;

/**
 * PreferenceActivity for API 11+. In order to change the behavior of the preference UI, see
 * PreferenceController.
 */
public class PreferenceActivity extends AppCompatActivity implements SearchPreferenceResultListener {

    public static final String PARAM_RESOURCE = "resource";
    private static WeakReference<PreferenceActivity> instance;
    private PreferenceController preferenceController;
    private final PreferenceController.PreferenceUI preferenceUI = new PreferenceController.PreferenceUI() {
        private PreferenceFragmentCompat fragment;

        @Override
        public void setFragment(PreferenceFragmentCompat fragment) {
            this.fragment = fragment;
        }

        @Override
        public PreferenceFragmentCompat getFragment() {
            return fragment;
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
        public AppCompatActivity getActivity() {
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

        showPreferenceScreen(R.xml.preferences, false);
    }

    private void showPreferenceScreen(int screen, boolean addHistory) {
        PreferenceFragmentCompat prefFragment = new MainFragment();
        preferenceUI.setFragment(prefFragment);
        Bundle args = new Bundle();
        args.putInt(PARAM_RESOURCE, screen);
        prefFragment.setArguments(args);
        if (addHistory) {
            getSupportFragmentManager().beginTransaction().replace(R.id.content, prefFragment)
                    .addToBackStack(getString(PreferenceController.getTitleOfPage(screen))).commit();
        } else {
            getSupportFragmentManager().beginTransaction().replace(R.id.content, prefFragment).commit();
        }
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
                if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                    finish();
                } else {
                    getSupportFragmentManager().popBackStack();
                }
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onSearchResultClicked(SearchPreferenceResult result) {
        showPreferenceScreen(result.getResourceFile(), true);
        result.highlight(preferenceUI.getFragment());
    }

    public static class MainFragment extends PreferenceFragmentCompat {
        private int screen;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
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
                activity.setTitle(PreferenceController.getTitleOfPage(screen));
                activity.preferenceUI.setFragment(this);
                activity.preferenceController.onResume(screen);
            }
        }

        @Override
        public void onPause() {
            PreferenceActivity activity = instance.get();
            if (screen == R.xml.preferences_gpodder) {
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
