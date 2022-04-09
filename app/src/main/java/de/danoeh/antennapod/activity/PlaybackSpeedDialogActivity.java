package de.danoeh.antennapod.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.dialog.VariableSpeedDialog;

public class PlaybackSpeedDialogActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTranslucentDialogTheme());
        super.onCreate(savedInstanceState);
        VariableSpeedDialog speedDialog = new InnerVariableSpeedDialog();
        speedDialog.show(getSupportFragmentManager(), null);
    }

    public static class InnerVariableSpeedDialog extends VariableSpeedDialog {
        @Override
        public void onDestroy() {
            super.onDestroy();
            getActivity().finish();
        }
    }
}
