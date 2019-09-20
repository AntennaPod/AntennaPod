package de.danoeh.antennapod.activity;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;
import de.danoeh.antennapod.CrashReportWriter;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Displays the 'crash report' screen
 */
public class CrashReportActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setContentView(R.layout.crash_report);

        TextView crashDetailsText = findViewById(R.id.crash_report_logs);

        try {
            File crashFile = CrashReportWriter.getFile();
            String crashReportContent = IOUtils.toString(new FileInputStream(crashFile), Charset.forName("UTF-8"));
            crashDetailsText.setText(crashReportContent);
        } catch (IOException e) {
            e.printStackTrace();
            crashDetailsText.setText("No crash report recorded\n" + CrashReportWriter.getSystemInfo());
        }

        findViewById(R.id.btn_open_bug_tracker).setOnClickListener(v -> {
            try {
                Intent myIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/AntennaPod/AntennaPod/issues"));
                startActivity(myIntent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.pref_no_browser_found, Toast.LENGTH_LONG).show();
            }
        });

        findViewById(R.id.btn_copy_log).setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(getString(R.string.crash_report_title), crashDetailsText.getText());
            clipboard.setPrimaryClip(clip);
            Snackbar.make(findViewById(android.R.id.content), R.string.copied_to_clipboard, Snackbar.LENGTH_SHORT).show();
        });
    }
}
