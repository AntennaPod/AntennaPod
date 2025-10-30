package de.danoeh.antennapod.ui.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.Transformation;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.ui.common.ToolbarActivity;
import de.danoeh.antennapod.ui.glide.FastBlurTransformation;

import java.util.Locale;

public class WidgetConfigActivity extends ToolbarActivity {
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private SeekBar opacitySeekBar;
    private TextView opacityTextView;
    private View widgetPreview;
    private CheckBox ckPlaybackSpeed;
    private CheckBox ckRewind;
    private CheckBox ckFastForward;
    private CheckBox ckSkip;
    private CheckBox ckCoverAsBcg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        setResult(Activity.RESULT_CANCELED, resultValue);
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
                opacityTextView.setText(String.format(Locale.getDefault(), "%d%%", seekBar.getProgress()));
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
        ckCoverAsBcg = findViewById(R.id.ckCoverAsBcg);
        ckCoverAsBcg.setOnClickListener(v -> displayPreviewPanel());

        setInitialState();
    }

    private void setInitialState() {
        SharedPreferences prefs = getSharedPreferences(PlayerWidget.PREFS_NAME, Context.MODE_PRIVATE);
        ckPlaybackSpeed.setChecked(prefs.getBoolean(PlayerWidget.KEY_WIDGET_PLAYBACK_SPEED + appWidgetId, false));
        ckRewind.setChecked(prefs.getBoolean(PlayerWidget.KEY_WIDGET_REWIND + appWidgetId, false));
        ckFastForward.setChecked(prefs.getBoolean(PlayerWidget.KEY_WIDGET_FAST_FORWARD + appWidgetId, false));
        ckSkip.setChecked(prefs.getBoolean(PlayerWidget.KEY_WIDGET_SKIP + appWidgetId, false));
        ckCoverAsBcg.setChecked(prefs.getBoolean(PlayerWidget.KEY_WIDGET_COVER_BACKGROUND + appWidgetId, false));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            int color = prefs.getInt(PlayerWidget.KEY_WIDGET_COLOR + appWidgetId, PlayerWidget.DEFAULT_COLOR);
            int opacity = Color.alpha(color) * 100 / 0xFF;

            opacitySeekBar.setProgress(opacity, false);
        }
        displayPreviewPanel();
    }

    private void displayPreviewPanel() {
        boolean showExtendedPreview =
                ckPlaybackSpeed.isChecked() || ckRewind.isChecked() || ckFastForward.isChecked() || ckSkip.isChecked();
        widgetPreview.findViewById(R.id.extendedButtonsContainer)
                .setVisibility(showExtendedPreview ? View.VISIBLE : View.GONE);
        widgetPreview.findViewById(R.id.butPlay).setVisibility(showExtendedPreview ? View.GONE : View.VISIBLE);
        widgetPreview.findViewById(R.id.butPlaybackSpeed)
                .setVisibility(ckPlaybackSpeed.isChecked() ? View.VISIBLE : View.GONE);
        widgetPreview.findViewById(R.id.butFastForward)
                .setVisibility(ckFastForward.isChecked() ? View.VISIBLE : View.GONE);
        widgetPreview.findViewById(R.id.butSkip).setVisibility(ckSkip.isChecked() ? View.VISIBLE : View.GONE);
        widgetPreview.findViewById(R.id.butRew).setVisibility(ckRewind.isChecked() ? View.VISIBLE : View.GONE);

        if (ckCoverAsBcg.isChecked()) {
            widgetPreview.findViewById(R.id.imgvCover).setVisibility(View.GONE);
            widgetPreview.findViewById(R.id.imgvBackground).setVisibility(View.VISIBLE);
            loadCover(R.id.imgvBackground, new FastBlurTransformation());
            opacitySeekBar.setEnabled(false);
            opacitySeekBar.setProgress(100);
        } else {
            widgetPreview.findViewById(R.id.imgvCover).setVisibility(View.VISIBLE);
            widgetPreview.findViewById(R.id.imgvBackground).setVisibility(View.GONE);
            widgetPreview.findViewById(R.id.widgetLayout).setBackgroundColor(PlayerWidget.DEFAULT_COLOR);
            opacitySeekBar.setEnabled(true);
            int radius = getResources().getDimensionPixelSize(R.dimen.widget_inner_radius);
            loadCover(R.id.imgvCover, new RoundedCorners(radius));
        }
    }

    private void loadCover(int viewId, Transformation<android.graphics.Bitmap> transform) {
        ImageView target = findViewById(viewId);
        Glide.with(this)
                .asBitmap()
                .load(Feed.PREFIX_GENERATIVE_COVER)
                .dontAnimate()
                .transform(new FitCenter(), transform)
                .into(target);
    }

    private void confirmCreateWidget() {
        int backgroundColor = getColorWithAlpha(PlayerWidget.DEFAULT_COLOR, opacitySeekBar.getProgress());

        SharedPreferences prefs = getSharedPreferences(PlayerWidget.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PlayerWidget.KEY_WIDGET_COLOR + appWidgetId, backgroundColor);
        editor.putBoolean(PlayerWidget.KEY_WIDGET_PLAYBACK_SPEED + appWidgetId, ckPlaybackSpeed.isChecked());
        editor.putBoolean(PlayerWidget.KEY_WIDGET_SKIP + appWidgetId, ckSkip.isChecked());
        editor.putBoolean(PlayerWidget.KEY_WIDGET_REWIND + appWidgetId, ckRewind.isChecked());
        editor.putBoolean(PlayerWidget.KEY_WIDGET_FAST_FORWARD + appWidgetId, ckFastForward.isChecked());
        editor.putBoolean(PlayerWidget.KEY_WIDGET_COVER_BACKGROUND + appWidgetId, ckCoverAsBcg.isChecked());
        editor.apply();

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(Activity.RESULT_OK, resultValue);
        finish();
        WidgetUpdaterWorker.enqueueWork(this);
    }

    private int getColorWithAlpha(int color, int opacity) {
        return (int) Math.round(0xFF * (0.01 * opacity)) * 0x1000000 + (color & 0xffffff);
    }
}
