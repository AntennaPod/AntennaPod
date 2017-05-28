package de.danoeh.antennapod.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.StatisticsListAdapter;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.PodDBAdapter;
import de.danoeh.antennapod.core.util.Converter;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

/**
 * Displays the 'statistics' screen
 */
public class ImportExportActivity extends AppCompatActivity {
    private static final int READ_REQUEST_CODE = 41;
    private static final int READ_REQUEST_CODE_DOCUMENT = 42;

    private static final String TAG = ImportExportActivity.class.getSimpleName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setContentView(R.layout.import_export_activity);


        //backup();
        //restore();
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
            startActivityForResult(Intent.createChooser(intent, "Select a File to import"), READ_REQUEST_CODE);
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
            }
        }
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    private void backup() {
        try {
            File sd = Environment.getExternalStorageDirectory();

            if (sd.canWrite()) {
                File currentDB = getDatabasePath(PodDBAdapter.DATABASE_NAME);
                File backupDB = new File(sd, "AntennaPodBackup.db");

                if (currentDB.exists()) {
                    FileChannel src = new FileInputStream(currentDB).getChannel();
                    FileChannel dst = new FileOutputStream(backupDB).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
