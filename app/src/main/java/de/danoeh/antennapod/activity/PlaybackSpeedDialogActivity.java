package de.danoeh.antennapod.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.dialog.VariableSpeedDialog;

public class PlaybackSpeedDialogActivity extends AppCompatActivity {

    VariableSpeedDialog speedDialog = new VariableSpeedDialog();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTranslucentDialogTheme());
        super.onCreate(savedInstanceState);
        speedDialog.show(getSupportFragmentManager(), null);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
