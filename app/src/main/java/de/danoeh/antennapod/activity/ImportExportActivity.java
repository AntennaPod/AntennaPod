package de.danoeh.antennapod.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.design.widget.Snackbar;
import android.support.v4.content.IntentCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.PodDBAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

/**
 * Displays the 'import/export' screen
 */
public class ImportExportActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_RESTORE = 43;
    private static final int REQUEST_CODE_BACKUP_DOCUMENT = 44;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setContentView(R.layout.import_export_activity);

        findViewById(R.id.button_export).setOnClickListener(view -> backup());
        findViewById(R.id.button_import).setOnClickListener(view -> restore());
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void backup() {
        if (Build.VERSION.SDK_INT >= 19) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("application/x-sqlite3")
                    .putExtra(Intent.EXTRA_TITLE, "AntennaPodBackup.db");

            startActivityForResult(intent, REQUEST_CODE_BACKUP_DOCUMENT);
        } else {
            try {
                File sd = Environment.getExternalStorageDirectory();
                File backupDB = new File(sd, "AntennaPodBackup.db");
                writeBackupTo(new FileOutputStream(backupDB));
            } catch (Exception e) {
                e.printStackTrace();
                Snackbar.make(findViewById(R.id.import_export_layout), e.getMessage(), Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private void restore() {
        if(Build.VERSION.SDK_INT >= 19) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            startActivityForResult(intent, REQUEST_CODE_RESTORE);
        } else {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(Intent.createChooser(intent,
                    getString(R.string.import_select_file)), REQUEST_CODE_RESTORE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == REQUEST_CODE_RESTORE && resultCode == RESULT_OK && resultData != null) {
            Uri uri = resultData.getData();
            restoreFrom(uri);
        } else if (requestCode == REQUEST_CODE_BACKUP_DOCUMENT && resultCode == RESULT_OK && resultData != null) {
            Uri uri = resultData.getData();
            backupToDocument(uri);
        }
    }

    private void restoreFrom(Uri inputUri) {
        File currentDB = getDatabasePath(PodDBAdapter.DATABASE_NAME);
        try {
            InputStream inputStream = getContentResolver().openInputStream(inputUri);
            copyInputStreamToFile(inputStream, currentDB);
            inputStream.close();
            displayImportSuccessDialog();
        } catch (Exception e) {
            e.printStackTrace();
            Snackbar.make(findViewById(R.id.import_export_layout), e.getMessage(), Snackbar.LENGTH_SHORT).show();
        }
    }

    private void displayImportSuccessDialog() {
        AlertDialog.Builder d = new AlertDialog.Builder(ImportExportActivity.this);
        d.setMessage(R.string.import_ok);
        d.setCancelable(false);
        d.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
            Intent intent = new Intent(getApplicationContext(), SplashActivity.class);
            ComponentName cn = intent.getComponent();
            Intent mainIntent = IntentCompat.makeRestartActivityTask(cn);
            startActivity(mainIntent);
        });
        d.show();
    }

    private void copyInputStreamToFile(InputStream in, File file) {
        try {
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0){
                out.write(buf, 0, len);
            }
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void backupToDocument(Uri uri) {
        try {
            ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
            FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());
            writeBackupTo(fileOutputStream);
            fileOutputStream.close();
            pfd.close();

            Snackbar.make(findViewById(R.id.import_export_layout),
                    R.string.export_ok, Snackbar.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();

            Snackbar.make(findViewById(R.id.import_export_layout),
                    "Can not write SD", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void writeBackupTo(FileOutputStream outFileStream) {
        try {
            File currentDB = getDatabasePath(PodDBAdapter.DATABASE_NAME);

            if (currentDB.exists()) {
                FileChannel src = new FileInputStream(currentDB).getChannel();
                FileChannel dst = outFileStream.getChannel();
                dst.transferFrom(src, 0, src.size());
                src.close();
                dst.close();

                Snackbar.make(findViewById(R.id.import_export_layout),
                        R.string.export_ok, Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(findViewById(R.id.import_export_layout),
                        "Can not access current database", Snackbar.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();

            Snackbar.make(findViewById(R.id.import_export_layout), e.getMessage(), Snackbar.LENGTH_SHORT).show();
        }
    }

}
