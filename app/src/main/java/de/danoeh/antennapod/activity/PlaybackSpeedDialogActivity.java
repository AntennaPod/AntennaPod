package de.danoeh.antennapod.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.dialog.VariableSpeedDialog;

public class PlaybackSpeedDialogActivity extends AppCompatActivity {

    VariableSpeedDialog speedDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTranslucentDialogTheme());
        super.onCreate(savedInstanceState);
        speedDialog = new VariableSpeedDialog();
        speedDialog.show(getSupportFragmentManager(), null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        speedDialog.getDialog().setCanceledOnTouchOutside(true);
        speedDialog.getDialog().setOnDismissListener(dialogInterface -> finish());
    }
}
