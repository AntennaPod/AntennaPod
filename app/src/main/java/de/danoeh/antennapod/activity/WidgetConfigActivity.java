package de.danoeh.antennapod.activity;

import android.Manifest;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.receiver.PlayerWidget;
import de.danoeh.antennapod.core.service.PlayerWidgetJobService;

public class WidgetConfigActivity extends AppCompatActivity {
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private SeekBar opacitySeekBar;
    private TextView opacityTextView;
    private View widgetPreview;
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

        displayDeviceBackground();
        opacityTextView = findViewById(R.id.widget_opacity_textView);
        opacitySeekBar = findViewById(R.id.widget_opacity_seekBar);
        widgetPreview = findViewById(R.id.widgetLayout);
        findViewById(R.id.butConfirm).setOnClickListener(this::confirmCreateWidget);
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

        ckRewind = findViewById(R.id.ckRewind);
        ckRewind.setOnClickListener(v -> displayPreviewPanel());
        ckFastForward = findViewById(R.id.ckFastForward);
        ckFastForward.setOnClickListener(v -> displayPreviewPanel());
        ckSkip = findViewById(R.id.ckSkip);
        ckSkip.setOnClickListener(v -> displayPreviewPanel());
    }

    private void displayPreviewPanel() {
        boolean showExtendedPreview = ckRewind.isChecked() || ckFastForward.isChecked() || ckSkip.isChecked();
        widgetPreview.findViewById(R.id.extendedButtonsContainer)
                .setVisibility(showExtendedPreview ? View.VISIBLE : View.GONE);
        widgetPreview.findViewById(R.id.butPlay).setVisibility(showExtendedPreview ? View.GONE : View.VISIBLE);
        widgetPreview.findViewById(R.id.butFastForward)
                .setVisibility(ckFastForward.isChecked() ? View.VISIBLE : View.GONE);
        widgetPreview.findViewById(R.id.butSkip).setVisibility(ckSkip.isChecked() ? View.VISIBLE : View.GONE);
        widgetPreview.findViewById(R.id.butRew).setVisibility(ckRewind.isChecked() ? View.VISIBLE : View.GONE);
    }

    private void displayDeviceBackground() {
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (Build.VERSION.SDK_INT < 27 || permission == PackageManager.PERMISSION_GRANTED) {
            final WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
            final Drawable wallpaperDrawable = wallpaperManager.getDrawable();
            ImageView background = findViewById(R.id.widget_config_background);
            background.setImageDrawable(wallpaperDrawable);
        }
    }

    private void confirmCreateWidget(View v) {
        int backgroundColor = getColorWithAlpha(PlayerWidget.DEFAULT_COLOR, opacitySeekBar.getProgress());

        SharedPreferences prefs = getSharedPreferences(PlayerWidget.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PlayerWidget.KEY_WIDGET_COLOR + appWidgetId, backgroundColor);
        editor.putBoolean(PlayerWidget.KEY_WIDGET_SKIP + appWidgetId, ckSkip.isChecked());
        editor.putBoolean(PlayerWidget.KEY_WIDGET_REWIND + appWidgetId, ckRewind.isChecked());
        editor.putBoolean(PlayerWidget.KEY_WIDGET_FAST_FORWARD + appWidgetId, ckFastForward.isChecked());
        editor.apply();

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
        PlayerWidgetJobService.updateWidget(this);
    }

    private int getColorWithAlpha(int color, int opacity) {
        return (int) Math.round(0xFF * (0.01 * opacity)) * 0x1000000 + color;
    }
}
