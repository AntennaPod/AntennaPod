package de.danoeh.antennapod.activity;

import android.Manifest;
import android.app.WallpaperManager;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.receiver.PlayerWidget;
import de.danoeh.antennapod.core.service.PlayerWidgetJobService;

public class WidgetConfigActivity extends AppCompatActivity {
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private SeekBar opacitySeekBar;
    private TextView opacityTextView;
    private RelativeLayout widgetPreview;
    private RelativeLayout widgetExtendedPreview;
    private FrameLayout previewLayout;
    private FrameLayout previewExtendedLayout;
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
        previewLayout = findViewById(R.id.widget_config_preview);
        widgetPreview = previewLayout.findViewById(R.id.widgetLayout);
        widgetExtendedPreview = findViewById(R.id.widgetExtendedLayout);
        previewExtendedLayout = findViewById(R.id.widget_config_extended_preview);
        findViewById(R.id.butConfirm).setOnClickListener(this::confirmCreateWidget);
        opacitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                opacityTextView.setText(seekBar.getProgress() + "%");
                int color = getColorWithAlpha(PlayerWidget.DEFAULT_COLOR, opacitySeekBar.getProgress());
                widgetPreview.setBackgroundColor(color);
                widgetExtendedPreview.setBackgroundColor(color);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

        });

        ckRewind = findViewById(R.id.ckRewind);
        ckRewind.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayPreviewPanel();
            }
        });
        ckFastForward = findViewById(R.id.ckFastForward);
        ckFastForward.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayPreviewPanel();
            }
        });
        ckSkip = findViewById(R.id.ckSkip);
        ckSkip.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayPreviewPanel();
            }
        });
    }

    private void displayPreviewPanel() {
        Boolean boolExtendedPreview = false;
        ImageButton butFastForward;
        ImageButton butRew;
        ImageButton butSkip;

        if (ckRewind.isChecked() || ckFastForward.isChecked() || ckSkip.isChecked()) {
            boolExtendedPreview = true;
        }
        if (boolExtendedPreview) {
            previewLayout.setVisibility(View.INVISIBLE);
            widgetExtendedPreview = findViewById(R.id.widgetExtendedLayout);
            previewExtendedLayout.setVisibility(View.VISIBLE);
            butFastForward = previewExtendedLayout.findViewById(R.id.butFastForward);
            butSkip = previewExtendedLayout.findViewById(R.id.butSkip);
            butRew = previewExtendedLayout.findViewById(R.id.butRew);
            butFastForward.setVisibility(ckFastForward.isChecked() ? View.VISIBLE : View.GONE);
            butRew.setVisibility(ckRewind.isChecked() ? View.VISIBLE : View.GONE);
            butSkip.setVisibility(ckSkip.isChecked() ? View.VISIBLE : View.GONE);
        } else {
            previewLayout.setVisibility(View.VISIBLE);
            previewExtendedLayout.setVisibility(View.INVISIBLE);
        }
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
