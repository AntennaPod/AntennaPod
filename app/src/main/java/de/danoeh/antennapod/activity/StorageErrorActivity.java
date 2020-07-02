package de.danoeh.antennapod.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.StorageUtils;
import de.danoeh.antennapod.dialog.ChooseDataFolderDialog;

/**
 * Is show if there is now external storage available.
 */
public class StorageErrorActivity extends AppCompatActivity {
    private static final String TAG = "StorageErrorActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);

        setContentView(R.layout.storage_error);

        Button btnChooseDataFolder = findViewById(R.id.btnChooseDataFolder);
        btnChooseDataFolder.setOnClickListener(v ->
                ChooseDataFolderDialog.showDialog(this, path -> {
                    UserPreferences.setDataFolder(path);
                    leaveErrorState();
                }));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (StorageUtils.storageAvailable()) {
            leaveErrorState();
        } else {
            registerReceiver(mediaUpdate, new IntentFilter(Intent.ACTION_MEDIA_MOUNTED));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(mediaUpdate);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    private void leaveErrorState() {
        finish();
        startActivity(new Intent(this, MainActivity.class));
    }

    private final BroadcastReceiver mediaUpdate = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (TextUtils.equals(intent.getAction(), Intent.ACTION_MEDIA_MOUNTED)) {
                if (intent.getBooleanExtra("read-only", true)) {
                    Log.d(TAG, "Media was mounted; Finishing activity");
                    leaveErrorState();
                } else {
                    Log.d(TAG, "Media seemed to have been mounted read only");
                }
            }
        }

    };

}
