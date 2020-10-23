package de.danoeh.antennapod.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;


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
import java.util.List;

/**
 * Displays the 'crash report' screen
 */
public class BugReportActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setContentView(R.layout.bug_report);

        String crashDetailsText = CrashReportWriter.getSystemInfo() + "\n\n";
        TextView crashDetailsTextView = findViewById(R.id.crash_report_logs);

        try {
            File crashFile = CrashReportWriter.getFile();
            crashDetailsText += IOUtils.toString(new FileInputStream(crashFile), Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
            crashDetailsText += "No crash report recorded";
        }
        crashDetailsTextView.setText(crashDetailsText);

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
                //share file
                try {
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("text/*");
                    String authString = getString(de.danoeh.antennapod.core.R.string.provider_authority);
                    Uri fileUri = FileProvider.getUriForFile(this, authString, filename);
                    i.putExtra(Intent.EXTRA_STREAM,  fileUri);
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                        PackageManager pm = getPackageManager();
                        List<ResolveInfo> resInfos = pm.queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY);
                        for (ResolveInfo resolveInfo : resInfos) {
                            String packageName = resolveInfo.activityInfo.packageName;
                            grantUriPermission(packageName, fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        }
                    }
                    String chooserTitle =  getString(de.danoeh.antennapod.core.R.string.share_file_label);
                    startActivity(Intent.createChooser(i, chooserTitle));
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
        });
    }

}
