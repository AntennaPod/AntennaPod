package de.danoeh.antennapod.activity;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.receiver.PlayerWidget;
import de.danoeh.antennapod.core.widget.WidgetUpdaterWorker;

public class WidgetConfigActivity extends AppCompatActivity {
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private SeekBar opacitySeekBar;
    private TextView opacityTextView;
    private View widgetPreview;
    private CheckBox ckPlaybackSpeed;
    private CheckBox ckRewind;
    private CheckBox ckFastForward;
    private CheckBox ckSkip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_config);

        Intent configIntent = getIntent();
        Bundle extras = configIntent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_CANCELED, resultValue);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }

        opacityTextView = findViewById(R.id.widget_opacity_textView);
        opacitySeekBar = findViewById(R.id.widget_opacity_seekBar);
        widgetPreview = findViewById(R.id.widgetLayout);
        findViewById(R.id.butConfirm).setOnClickListener(v -> confirmCreateWidget());
        opacitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                opacityTextView.setText(seekBar.getProgress() + "%");
                int color = getColorWithAlpha(PlayerWidget.DEFAULT_COLOR, opacitySeekBar.getProgress());
                widgetPreview.setBackgroundColor(color);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

        });

        widgetPreview.findViewById(R.id.txtNoPlaying).setVisibility(View.GONE);
        TextView title = widgetPreview.findViewById(R.id.txtvTitle);
        title.setVisibility(View.VISIBLE);
        title.setText(R.string.app_name);
        TextView progress = widgetPreview.findViewById(R.id.txtvProgress);
        progress.setVisibility(View.VISIBLE);
        progress.setText(R.string.position_default_label);

        ckPlaybackSpeed = findViewById(R.id.ckPlaybackSpeed);
        ckPlaybackSpeed.setOnClickListener(v -> displayPreviewPanel());
        ckRewind = findViewById(R.id.ckRewind);
        ckRewind.setOnClickListener(v -> displayPreviewPanel());
        ckFastForward = findViewById(R.id.ckFastForward);
        ckFastForward.setOnClickListener(v -> displayPreviewPanel());
        ckSkip = findViewById(R.id.ckSkip);
        ckSkip.setOnClickListener(v -> displayPreviewPanel());
    }

    private void displayPreviewPanel() {
        boolean showExtendedPreview =
                ckPlaybackSpeed.isChecked() || ckRewind.isChecked() || ckFastForward.isChecked() || ckSkip.isChecked();
        widgetPreview.findViewById(R.id.extendedButtonsContainer)
                .setVisibility(showExtendedPreview ? View.VISIBLE : View.GONE);
        // BUTTON VISIBILITY
        widgetPreview.findViewById(R.id.butPlay).setVisibility(showExtendedPreview ? View.GONE : View.VISIBLE);
        widgetPreview.findViewById(R.id.butPlaybackSpeed)
                .setVisibility(ckPlaybackSpeed.isChecked() ? View.VISIBLE : View.GONE);
        widgetPreview.findViewById(R.id.butRew).setVisibility(ckRewind.isChecked() ? View.VISIBLE : View.GONE);
        widgetPreview.findViewById(R.id.butFastForward)
                .setVisibility(ckFastForward.isChecked() ? View.VISIBLE : View.GONE);
        widgetPreview.findViewById(R.id.butSkip).setVisibility(ckSkip.isChecked() ? View.VISIBLE : View.GONE);
        // SPACER VISIBILITY
        //
        // The spacer to the left of the rewind button should only be shown
        // when the rewind button is enabled, and the only other button to
        // the left (playback speed) is also enabled
        widgetPreview.findViewById(R.id.spacerRew).setVisibility((ckPlaybackSpeed.isChecked() && ckRewind.isChecked()) ? View.VISIBLE : View.GONE);
        // The spacer to the left of the play button should only be shown
        // if either of the two buttons to the left are enabled
        widgetPreview.findViewById(R.id.spacerPlay).setVisibility((ckPlaybackSpeed.isChecked() || ckRewind.isChecked()) ? View.VISIBLE : View.GONE);
        // The spacer to the left of the fast-forward and skip buttons
        // should be enabled if those buttons are enabled, since they are
        // to the right of the play button (which is always enabled)
        widgetPreview.findViewById(R.id.spacerFastForward)
                .setVisibility(ckFastForward.isChecked() ? View.VISIBLE : View.GONE);
        widgetPreview.findViewById(R.id.spacerSkip).setVisibility(ckSkip.isChecked() ? View.VISIBLE : View.GONE);
    }

    private void confirmCreateWidget() {
        int backgroundColor = getColorWithAlpha(PlayerWidget.DEFAULT_COLOR, opacitySeekBar.getProgress());

        SharedPreferences prefs = getSharedPreferences(PlayerWidget.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PlayerWidget.KEY_WIDGET_COLOR + appWidgetId, backgroundColor);
        editor.putBoolean(PlayerWidget.KEY_WIDGET_PLAYBACK_SPEED + appWidgetId, ckPlaybackSpeed.isChecked());
        editor.putBoolean(PlayerWidget.KEY_WIDGET_SKIP + appWidgetId, ckSkip.isChecked());
        editor.putBoolean(PlayerWidget.KEY_WIDGET_REWIND + appWidgetId, ckRewind.isChecked());
        editor.putBoolean(PlayerWidget.KEY_WIDGET_FAST_FORWARD + appWidgetId, ckFastForward.isChecked());
        editor.apply();

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
        WidgetUpdaterWorker.enqueueWork(this);
    }

    private int getColorWithAlpha(int color, int opacity) {
        return (int) Math.round(0xFF * (0.01 * opacity)) * 0x1000000 + color;
    }
}
