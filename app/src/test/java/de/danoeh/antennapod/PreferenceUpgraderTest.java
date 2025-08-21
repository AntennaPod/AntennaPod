package de.danoeh.antennapod;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.test.platform.app.InstrumentationRegistry;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;

import de.danoeh.antennapod.storage.preferences.SleepTimerPreferences;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.screen.AllEpisodesFragment;
import de.danoeh.antennapod.ui.swipeactions.SwipeActions;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class PreferenceUpgraderTest {

    private Context context;
    private SharedPreferences defaultPrefs;
    private SharedPreferences upgraderPrefs;

    private File crashFile;

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // Initialize preference singletons used within upgrade flow
        UserPreferences.init(context);
        SleepTimerPreferences.init(context);

        defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        defaultPrefs.edit().clear().apply();

        upgraderPrefs = context.getSharedPreferences("app_version", Context.MODE_PRIVATE);
        upgraderPrefs.edit().clear().apply();

        // Ensure crash report file directory exists and file is deleted before each test
        crashFile = CrashReportWriter.getFile();
        if (crashFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            crashFile.delete();
        }
        File parent = crashFile.getParentFile();
        if (parent != null) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }
    }

    @After
    public void tearDown() throws Exception {
        defaultPrefs.edit().clear().apply();
        context.getSharedPreferences(AllEpisodesFragment.PREF_NAME, Context.MODE_PRIVATE).edit().clear().apply();
        context.getSharedPreferences(SleepTimerPreferences.PREF_NAME, Context.MODE_PRIVATE).edit().clear().apply();
        context.getSharedPreferences(SwipeActions.PREF_NAME, Context.MODE_PRIVATE).edit().clear().apply();
        upgraderPrefs.edit().clear().apply();
        if (crashFile != null && crashFile.exists()) {
            FileUtils.forceDelete(crashFile);
        }
    }

    @Test
    public void testCheckUpgradesWhenVersionDiffDeletesCrashReportAndPersistsNewVersion() throws Exception {
        // Simulate old version lower than a migration threshold
        upgraderPrefs.edit().putInt("version_code", 2049999).apply();
        // Create a crash report file to be deleted
        assertTrue(crashFile.createNewFile());
        assertTrue(crashFile.exists());

        // Precondition: preference not forced true
        defaultPrefs.edit().putBoolean(UserPreferences.PREF_PAUSE_PLAYBACK_FOR_FOCUS_LOSS, false).apply();

        PreferenceUpgrader.checkUpgrades(context);

        // Crash report is deleted
        assertFalse(crashFile.exists());
        // Migration ran: pref set to true for < 2050000
        assertTrue(defaultPrefs.getBoolean(UserPreferences.PREF_PAUSE_PLAYBACK_FOR_FOCUS_LOSS, false));
        // New version persisted
        int storedVersion = upgraderPrefs.getInt("version_code", -1);
        assertEquals(BuildConfig.VERSION_CODE, storedVersion);
    }

    @Test
    public void testMigrateMobileNetworkSettingsFromLegacyValues_pre1070300() {
        // Simulate legacy version
        upgraderPrefs.edit().putInt("version_code", 1070200).apply();
        // Legacy flags
        defaultPrefs.edit()
                .putBoolean("prefEnableAutoDownloadOnMobile", true)
                .putString("prefMobileUpdateAllowed", "everything")
                .apply();

        PreferenceUpgrader.checkUpgrades(context);

        // New granular settings applied
        assertTrue(UserPreferences.isAllowMobileAutoDownload());
        assertTrue(UserPreferences.isAllowMobileFeedRefresh());
        assertTrue(UserPreferences.isAllowMobileEpisodeDownload());
        assertTrue(UserPreferences.isAllowMobileImages());
    }

    @Test
    public void testMigrateAllEpisodesPrefsToDefaultStore_pre3030000() {
        // Simulate legacy version
        upgraderPrefs.edit().putInt("version_code", 3029999).apply();

        // Legacy storage contains values
        SharedPreferences legacyAllEpisodes =
                context.getSharedPreferences(AllEpisodesFragment.PREF_NAME, Context.MODE_PRIVATE);
        final String sortValue = "42";
        final String filterValue = "is:favorite,include_not_subscribed";
        legacyAllEpisodes.edit()
                .putString(UserPreferences.PREF_SORT_ALL_EPISODES, sortValue)
                .putString("filter", filterValue)
                .apply();

        PreferenceUpgrader.checkUpgrades(context);

        // Values migrated to default shared preferences
        assertEquals(sortValue, defaultPrefs.getString(UserPreferences.PREF_SORT_ALL_EPISODES, ""));
        assertEquals(filterValue, defaultPrefs.getString(UserPreferences.PREF_FILTER_ALL_EPISODES, ""));
    }

    @Test
    public void testCheckUpgradesNoOpWhenVersionUnchanged() throws Exception {
        // Simulate already upgraded to current version
        upgraderPrefs.edit().putInt("version_code", BuildConfig.VERSION_CODE).apply();

        // Create a crash report file to verify no deletion
        assertTrue(crashFile.createNewFile());
        assertTrue(crashFile.exists());

        // Set a pref that would otherwise be changed by a migration to ensure no-op
        defaultPrefs.edit().putBoolean(UserPreferences.PREF_PAUSE_PLAYBACK_FOR_FOCUS_LOSS, false).apply();

        PreferenceUpgrader.checkUpgrades(context);

        // No deletion and no changes
        assertTrue(crashFile.exists());
        assertFalse(defaultPrefs.getBoolean(UserPreferences.PREF_PAUSE_PLAYBACK_FOR_FOCUS_LOSS, true));
        assertEquals(BuildConfig.VERSION_CODE, upgraderPrefs.getInt("version_code", -1));
    }

    @Test
    public void testUpgradeSkipsMigrationsOnNewInstall_oldVersionMinusOne() {
        // No stored version -> oldVersion = -1 (new install)
        upgraderPrefs.edit().clear().apply();

        // Ensure key is absent before
        assertFalse(defaultPrefs.contains(UserPreferences.PREF_PAUSE_PLAYBACK_FOR_FOCUS_LOSS));

        PreferenceUpgrader.checkUpgrades(context);

        // Migrations should be skipped: key remains absent
        assertFalse(defaultPrefs.contains(UserPreferences.PREF_PAUSE_PLAYBACK_FOR_FOCUS_LOSS));
        // New version persisted
        assertEquals(BuildConfig.VERSION_CODE, upgraderPrefs.getInt("version_code", -1));
    }

    @Test
    public void testUpgradeHandlesInvalidSleepTimerPrefsWithoutCrash() {
        // Simulate legacy version before 2080000
        upgraderPrefs.edit().putInt("version_code", 2079999).apply();

        // Set invalid legacy values
        SleepTimerPreferences.setLastTimer("not_a_number");
        context.getSharedPreferences(SleepTimerPreferences.PREF_NAME, Context.MODE_PRIVATE)
                .edit().putInt("LastTimeUnit", 5).apply(); // Out of range

        // Código falha na linha 123 de PreferenceUpgrader.java
        // encontramos um bug!
        PreferenceUpgrader.checkUpgrades(context);
    }
}