package de.danoeh.antennapod.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;
import de.danoeh.antennapod.error.CrashReportWriter;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.IntentUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Displays the 'crash report' screen
 */
public class BugReportActivity extends AppCompatActivity {
    private static final String TAG = "BugReportActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setContentView(R.layout.bug_report);

        String stacktrace = "No crash report recorded";
        try {
            File crashFile = CrashReportWriter.getFile();
            if (crashFile.exists()) {
                stacktrace = IOUtils.toString(new FileInputStream(crashFile), Charset.forName("UTF-8"));
            } else {
                Log.d(TAG, stacktrace);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        TextView crashDetailsTextView = findViewById(R.id.crash_report_logs);
        crashDetailsTextView.setText(CrashReportWriter.getSystemInfo() + "\n\n" + stacktrace);

        findViewById(R.id.btn_open_bug_tracker).setOnClickListener(v -> IntentUtils.openInBrowser(
                BugReportActivity.this, "https://github.com/AntennaPod/AntennaPod/issues"));

        findViewById(R.id.btn_copy_log).setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(getString(R.string.bug_report_title), crashDetailsTextView.getText());
            clipboard.setPrimaryClip(clip);
            Snackbar.make(findViewById(android.R.id.content), R.string.copied_to_clipboard, Snackbar.LENGTH_SHORT).show();
        });

        findViewById(R.id.btn_export_logcat).setOnClickListener(v -> {
            try {
                File filename = new File(UserPreferences.getDataFolder(null), "full-logs.txt");
                filename.createNewFile();
                String cmd = "logcat -d -f " + filename.getAbsolutePath();
                Runtime.getRuntime().exec(cmd);
                Snackbar.make(findViewById(android.R.id.content),
                        filename.getAbsolutePath(), Snackbar.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Snackbar.make(findViewById(android.R.id.content), e.getMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
