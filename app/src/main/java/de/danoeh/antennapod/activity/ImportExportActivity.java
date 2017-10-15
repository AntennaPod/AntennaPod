package de.danoeh.antennapod.activity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.design.widget.Snackbar;
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
    private static final int READ_REQUEST_CODE = 42;
    private static final int READ_REQUEST_CODE_DOCUMENT = 43;
    private static final int WRITE_REQUEST_CODE_DOCUMENT = 44;

    private static final String TAG = ImportExportActivity.class.getSimpleName();


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

    private void restore() {
        if(Build.VERSION.SDK_INT >= 19) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            startActivityForResult(intent, READ_REQUEST_CODE_DOCUMENT);
        } else {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(Intent.createChooser(intent, getString(R.string.import_select_file)), READ_REQUEST_CODE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        File currentDB = getDatabasePath(PodDBAdapter.DATABASE_NAME);

        if (requestCode == READ_REQUEST_CODE_DOCUMENT && resultCode == RESULT_OK) {
            if (resultData != null) {
                Uri uri = resultData.getData();

                try {
                    InputStream inputStream = getContentResolver().openInputStream(uri);
                    copyInputStreamToFile(inputStream, currentDB);
                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        } else if (requestCode == WRITE_REQUEST_CODE_DOCUMENT && resultCode == RESULT_OK) {
            if (resultData != null) {
                Uri uri = resultData.getData();
                writeBackupDocument(uri);
            }
        } else if(requestCode == READ_REQUEST_CODE && resultCode == RESULT_OK) {
            if (resultData != null) {
                Uri uri = resultData.getData();
                try {
                    File backupDB = new File(getPath(getBaseContext(), uri));

                    if (backupDB.exists()) {
                        FileChannel src = new FileInputStream(currentDB).getChannel();
                        FileChannel dst = new FileOutputStream(backupDB).getChannel();
                        dst.transferFrom(src, 0, src.size());
                        src.close();
                        dst.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void copyInputStreamToFile(InputStream in, File file) {
        try {
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while((len=in.read(buf))>0){
                out.write(buf,0,len);
            }
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getPath(Context context, Uri uri) {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { "_data" };
            Cursor cursor = null;

            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                // Eat it
            } finally {
                cursor.close();
            }
        }
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    private void backup() {
        if (Build.VERSION.SDK_INT >= 19) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("application/x-sqlite3")
                    .putExtra(Intent.EXTRA_TITLE, "AntennaPodBackup.db");

            startActivityForResult(intent, WRITE_REQUEST_CODE_DOCUMENT);
        } else {
            try {
                File sd = Environment.getExternalStorageDirectory();

                if (sd.canWrite()) {
                    File backupDB = new File(sd, "AntennaPodBackup.db");
                    writeBackup(new FileOutputStream(backupDB));
                } else {
                    Snackbar.make(findViewById(R.id.import_export_layout),
                            "Can not write SD", Snackbar.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();

                Snackbar.make(findViewById(R.id.import_export_layout), e.getMessage(), Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    void writeBackup(FileOutputStream outFileStream) {
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

    private void writeBackupDocument(Uri uri) {
        try {
            ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
            FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());
            writeBackup(fileOutputStream);
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

}
