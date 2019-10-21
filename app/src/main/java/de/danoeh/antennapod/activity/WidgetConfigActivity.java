package de.danoeh.antennapod.activity;

import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.receiver.PlayerWidget;

public class WidgetConfigActivity extends AppCompatActivity {

    private static final String TAG = "WidgetConfigActivity";
    private final int DEFAULT_COLOR = 0x00262C31;
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private SeekBar opacitySeekBar;
    private TextView opacityTextView;
    private RelativeLayout widgetPreview;

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
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,appWidgetId);
        setResult(RESULT_CANCELED,resultValue);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }

        opacityTextView = findViewById(R.id.widget_opacity_textView);
        opacitySeekBar = findViewById(R.id.widget_opacity_seekBar);
        widgetPreview = findViewById(R.id.widgetLayout);
        opacitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                opacityTextView.setText(seekBar.getProgress() + "%");
                widgetPreview.setBackgroundColor(getColorWithAlpha(DEFAULT_COLOR,opacitySeekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

        });
    }

    public void confirmCreateWidget(View v) {
        int backgroundColor = getColorWithAlpha(DEFAULT_COLOR,opacitySeekBar.getProgress());

        RemoteViews views = new RemoteViews(this.getPackageName(), R.layout.player_widget);
        views.setInt(R.id.widgetLayout,"setBackgroundColor",backgroundColor);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        appWidgetManager.updateAppWidget(appWidgetId,views);

        SharedPreferences prefs = getSharedPreferences(PlayerWidget.WIDGET_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PlayerWidget.WIDGET_COLOR + appWidgetId,backgroundColor);
        editor.apply();

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,appWidgetId);
        setResult(RESULT_OK,resultValue);
        finish();
    }

    public int getColorWithAlpha(int color,int opacity) {
        Log.d(TAG,"Widget background color : " + ((int)Math.round(0xFF * (0.01 * opacity)) << 24 | color));
      return  (int)Math.round(0xFF * (0.01 * opacity)) * 0x1000000 + color ;
    }

}