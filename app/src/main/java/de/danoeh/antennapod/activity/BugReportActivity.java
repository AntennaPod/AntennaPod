package de.danoeh.antennapod.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;


import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;


import de.danoeh.antennapod.core.preferences.ThemeSwitcher;
import de.danoeh.antennapod.error.CrashReportWriter;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
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
        setTheme(ThemeSwitcher.getTheme(this));
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
            if (Build.VERSION.SDK_INT < 32) {
                Snackbar.make(findViewById(android.R.id.content), R.string.copied_to_clipboard,
                        Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bug_report_options, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.export_logcat) {
            MaterialAlertDialogBuilder alertBuilder = new MaterialAlertDialogBuilder(this);
            alertBuilder.setMessage(R.string.confirm_export_log_dialog_message);
            alertBuilder.setPositiveButton(R.string.confirm_label, (dialog, which) -> {
                exportLog();
                dialog.dismiss();
            });
            alertBuilder.setNegativeButton(R.string.cancel_label, null);
            alertBuilder.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void exportLog() {
        try {
            File filename = new File(UserPreferences.getDataFolder(null), "full-logs.txt");
            String cmd = "logcat -d -f " + filename.getAbsolutePath();
            Runtime.getRuntime().exec(cmd);
            //share file
            try {
                String authority = getString(R.string.provider_authority);
                Uri fileUri = FileProvider.getUriForFile(this, authority, filename);

                new ShareCompat.IntentBuilder(this)
                        .setType("text/*")
                        .addStream(fileUri)
                        .setChooserTitle(R.string.share_file_label)
                        .startChooser();
            } catch (Exception e) {
                e.printStackTrace();
                int strResId = R.string.log_file_share_exception;
                Snackbar.make(findViewById(android.R.id.content), strResId, Snackbar.LENGTH_LONG)
                        .show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Snackbar.make(findViewById(android.R.id.content), e.getMessage(), Snackbar.LENGTH_LONG).show();
        }
    }


}
