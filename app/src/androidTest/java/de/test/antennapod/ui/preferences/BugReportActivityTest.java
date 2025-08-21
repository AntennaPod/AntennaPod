package de.test.antennapod.ui.preferences;


import android.content.Intent;

import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import de.danoeh.antennapod.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Instrumentation;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;

import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.intent.matcher.IntentMatchers;

import androidx.appcompat.view.SupportMenuInflater;
import androidx.appcompat.view.menu.MenuBuilder;

import de.danoeh.antennapod.CrashReportWriter;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.screen.preferences.BugReportActivity;

import org.hamcrest.core.AllOf;
import org.junit.Assume;
import org.junit.Ignore;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.containsString;

@RunWith(AndroidJUnit4.class)
public class BugReportActivityTest {

    @Rule
    public IntentsTestRule<BugReportActivity> activityRule =
            new IntentsTestRule<>(BugReportActivity.class, false, false);

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @After
    public void tearDown() {
        // Cleanup crash report file
        File crash = CrashReportWriter.getFile();
        if (crash.exists()) {
            // noinspection ResultOfMethodCallIgnored
            crash.delete();
        }
        // Cleanup exported log file
        File exported = new File(UserPreferences.getDataFolder(null), "full-logs.txt");
        if (exported.exists()) {
            // noinspection ResultOfMethodCallIgnored
            exported.delete();
        }
    }

    @Test
    public void testDisplaysSystemInfoAndCrashLog_whenCrashFileExists() throws Exception {
        // Arrange: write a crash report
        String crashContent = "java.lang.RuntimeException: Boom!\n\tat com.example.Test(Unknown)";
        File crashFile = CrashReportWriter.getFile();
        File parent = crashFile.getParentFile();
        if (parent != null && !parent.exists()) {
            // noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }
        try (FileOutputStream fos = new FileOutputStream(crashFile)) {
            fos.write(crashContent.getBytes(StandardCharsets.UTF_8));
        }

        // Act
        activityRule.launchActivity(new Intent());

        // Assert
        String expectedSystemInfo = CrashReportWriter.getSystemInfo();
        onView(withId(R.id.crash_report_logs))
                .check(matches(withText(containsString(expectedSystemInfo))))
                .check(matches(withText(containsString(crashContent))));
    }

    @Test
    public void testDisplaysDefaultMessage_whenCrashFileMissing() {
        // Ensure crash file does not exist
        File crashFile = CrashReportWriter.getFile();
        if (crashFile.exists()) {
            // noinspection ResultOfMethodCallIgnored
            crashFile.delete();
        }

        activityRule.launchActivity(new Intent());

        String expectedSystemInfo = CrashReportWriter.getSystemInfo();
        onView(withId(R.id.crash_report_logs))
                .check(matches(withText(containsString(expectedSystemInfo))))
                .check(matches(withText(containsString("No crash report recorded"))));
    }

    @Test
    public void testOpenBugTrackerButton_opensBrowserWithIssuesUrl() {
        activityRule.launchActivity(new Intent());
        Intents.init();

        // Click the button that opens the bug tracker
        onView(withId(R.id.btn_open_bug_tracker)).perform(click());

        // Verify the ACTION_VIEW intent with the correct URL
        Intents.intended(AllOf.allOf(
                IntentMatchers.hasAction(Intent.ACTION_VIEW),
                IntentMatchers.hasData(Uri.parse("https://github.com/AntennaPod/AntennaPod/issues"))
        ));

        Intents.release();
    }

    @Test
    public void testCopyLog_noSnackbarOnApi32Plus_butClipboardUpdated() {
        Assume.assumeTrue("This test validates behavior only on API 32+", Build.VERSION.SDK_INT >= 32);

        activityRule.launchActivity(new Intent());

        // Get the expected clipboard text from the TextView
        final String[] expected = new String[1];
        activityRule.getActivity().runOnUiThread(() ->
                expected[0] = activityRule.getActivity().findViewById(R.id.crash_report_logs)
                        .getAccessibilityClassName() != null
                        ? ((android.widget.TextView) activityRule.getActivity().findViewById(R.id.crash_report_logs)).getText().toString()
                        : "");

        // Click copy log
        onView(withId(R.id.btn_copy_log)).perform(click());

        // Verify clipboard updated
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = clipboard.getPrimaryClip();
        String clipboardText = "";
        if (clipData != null && clipData.getItemCount() > 0 && clipData.getItemAt(0).getText() != null) {
            clipboardText = clipData.getItemAt(0).getText().toString();
        }
        org.junit.Assert.assertTrue(clipboardText.contains(CrashReportWriter.getSystemInfo()));

        // Verify no Snackbar with "copied_to_clipboard" appears on API 32+
        onView(withText(R.string.copied_to_clipboard)).check(doesNotExist());
    }

    @Test
    public void testExportLogcat_sharesLogFileAfterConfirmation() throws Exception {
        // Pre-create the file to ensure FileProvider can resolve it
        File exported = new File(UserPreferences.getDataFolder(null), "full-logs.txt");
        File exportedParent = exported.getParentFile();
        if (exportedParent != null && !exportedParent.exists()) {
            // noinspection ResultOfMethodCallIgnored
            exportedParent.mkdirs();
        }
        if (!exported.exists()) {
            // noinspection ResultOfMethodCallIgnored
            exported.createNewFile();
        }

        activityRule.launchActivity(new Intent());
        Intents.init();

        // Stub out external chooser intent to avoid launching external UI
        Intents.intending(IntentMatchers.hasAction(Intent.ACTION_CHOOSER))
                .respondWith(new Instrumentation.ActivityResult(0, null));

        // Programmatically trigger options item selection for export
        BugReportActivity activity = activityRule.getActivity();
        MenuBuilder menu = new MenuBuilder(activity);
        new SupportMenuInflater(activity).inflate(R.menu.bug_report_options, menu);
        final androidx.core.internal.view.SupportMenuItem exportItem =
                (androidx.core.internal.view.SupportMenuItem) menu.findItem(R.id.export_logcat);

        activity.runOnUiThread(() -> activity.onOptionsItemSelected(exportItem));

        // Confirm the dialog to start export and share
        onView(withText(R.string.confirm_label)).perform(click());

        // Verify chooser started
        Intents.intended(IntentMatchers.hasAction(Intent.ACTION_CHOOSER));
        Intents.release();
    }

    @Ignore("Unable to deterministically force FileProvider/chooser failure in instrumentation; kept for completeness")
    @Test
    public void testExportLogcat_sharingFailureShowsErrorSnackbar() throws Exception {
        activityRule.launchActivity(new Intent());

        // Trigger export via options
        BugReportActivity activity = activityRule.getActivity();
        MenuBuilder menu = new MenuBuilder(activity);
        new SupportMenuInflater(activity).inflate(R.menu.bug_report_options, menu);
        final androidx.core.internal.view.SupportMenuItem exportItem =
                (androidx.core.internal.view.SupportMenuItem) menu.findItem(R.id.export_logcat);

        activity.runOnUiThread(() -> activity.onOptionsItemSelected(exportItem));

        // Confirm export; expecting error Snackbar in failure scenario
        onView(withText(R.string.confirm_label)).perform(click());

        onView(withText(R.string.log_file_share_exception)).check(matches(isDisplayed()));
    }
}
