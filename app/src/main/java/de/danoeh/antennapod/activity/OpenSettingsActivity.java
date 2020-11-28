package de.danoeh.antennapod.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.ActivityOpenSettingsBinding;

public class OpenSettingsActivity extends AppCompatActivity {
    private boolean taskStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityOpenSettingsBinding viewBinding = ActivityOpenSettingsBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

//        To trigger settings for the app when clicked on the settings button.
        viewBinding.settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
            taskStarted = true;
        });
    }

    //    when this activity is resumed again after returning back from the settings page,
    //    send users to main activity.
    @Override
    protected void onResume() {
        super.onResume();
        if (taskStarted) {
            Intent intent = new Intent(OpenSettingsActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
}