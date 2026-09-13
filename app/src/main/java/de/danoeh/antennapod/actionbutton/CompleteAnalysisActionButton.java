package de.danoeh.antennapod.actionbutton;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.net.ai.service.ad.AdAnalysisWorkScheduler;
import de.danoeh.antennapod.net.ai.service.ad.provider.AdAnalysisProviderFactory;
import de.danoeh.antennapod.net.ai.service.ad.vosk.VoskTranscriptionManager;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.storage.preferences.CloudAiPreferences;
import de.danoeh.antennapod.storage.preferences.LocalAiPreferences;
import de.danoeh.antennapod.ui.screen.preferences.PreferenceActivity;

/**
 * Action button that performs complete ad analysis: transcription followed by
 * ad detection.
 * Uses a queue so that only one episode is processed at a time.
 */
@RequiresApi(api = Build.VERSION_CODES.O)
public class CompleteAnalysisActionButton extends ItemActionButton {

    private static final String PREFS_AD_ANALYSIS = "ad_analysis_prefs";
    private static final String KEY_BATTERY_PROMPT_SHOWN = "battery_opt_prompt_shown";

    public CompleteAnalysisActionButton(FeedItem item) {
        super(item);
    }

    @Override
    @StringRes
    public int getLabel() {
        return R.string.action_complete_analysis;
    }

    @Override
    @DrawableRes
    public int getDrawable() {
        return R.drawable.ic_ad_analysis;
    }

    @Override
    public int getVisibility() {
        return item.getMedia() == null ? View.GONE : View.VISIBLE;
    }

    @Override
    public void onClick(Context context) {
        FeedMedia media = item.getMedia();
        if (media == null) {
            return;
        }
        if (TextUtils.isEmpty(media.getLocalFileUrl()) || !new File(media.getLocalFileUrl()).exists()) {
            Toast.makeText(context, R.string.transcription_requires_download, Toast.LENGTH_LONG).show();
            return;
        }

        // Check if feed has a cloud model override
        String feedModelOverride = null;
        if (item.getFeed() != null && item.getFeed().getPreferences() != null) {
            feedModelOverride = item.getFeed().getPreferences().getTranscriptionModel();
        }
        boolean usesCloudTranscription =
                AdAnalysisProviderFactory.usesCloudTranscription(context, feedModelOverride);

        if (usesCloudTranscription && !CloudAiPreferences.hasCredentials(context)) {
            showApiKeyMissingDialog(context);
            return;
        }

        if (!usesCloudTranscription) {
            String model = TextUtils.isEmpty(feedModelOverride)
                    ? LocalAiPreferences.getLocalTranscriptionModel(context) : feedModelOverride;
            if (!new VoskTranscriptionManager(context).isModelDownloaded(model)) {
                new MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.ad_analysis_model_missing_title)
                        .setMessage(R.string.ad_analysis_model_missing_message)
                        .setPositiveButton(R.string.action_download_model, (d, w) -> {
                            Intent intent = new Intent(context, PreferenceActivity.class);
                            intent.putExtra(PreferenceActivity.OPEN_AI_SETTINGS, true);
                            context.startActivity(intent);
                        })
                        .setNeutralButton(R.string.action_use_cloud, (d, w) -> {
                            useCloudTranscription(context);
                            enqueueAnalysis(context, media);
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                return;
            }
        }

        // Check cloud credentials for analysis (always required for ad analysis)
        if (!CloudAiPreferences.hasCredentials(context)) {
            showApiKeyMissingDialog(context);
            return;
        }

        enqueueAnalysis(context, media);
    }

    private void useCloudTranscription(Context context) {
        String feedModelOverride = item.getFeed() == null || item.getFeed().getPreferences() == null
                ? null : item.getFeed().getPreferences().getTranscriptionModel();
        if (!TextUtils.isEmpty(feedModelOverride) && !feedModelOverride.startsWith("cloud:")
                && item.getFeed() != null && item.getFeed().getPreferences() != null) {
            item.getFeed().getPreferences()
                    .setTranscriptionModel(AdAnalysisProviderFactory.CLOUD_TRANSCRIPTION_OVERRIDE);
            DBWriter.setFeedPreferences(item.getFeed().getPreferences());
        } else {
            LocalAiPreferences.setLocalTranscriptionEnabled(context, false);
        }
    }

    private void enqueueAnalysis(Context context, FeedMedia media) {
        if (AdAnalysisWorkScheduler.enqueueManual(context, media)) {
            Toast.makeText(context, R.string.ad_analysis_queued, Toast.LENGTH_SHORT).show();
            maybePromptDisableBatteryOptimization(context);
        } else {
            showApiKeyMissingDialog(context);
        }
    }

    /**
     * Long-running background analysis (transcription + ad detection) survives the screen being off
     * or the app being backgrounded via a foreground service, but aggressive battery optimization can
     * still pause or kill it. Prompt the user once to exempt the app so queued episodes keep running.
     */
    private void maybePromptDisableBatteryOptimization(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager == null || powerManager.isIgnoringBatteryOptimizations(context.getPackageName())) {
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_AD_ANALYSIS, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_BATTERY_PROMPT_SHOWN, false)) {
            return;
        }
        prefs.edit().putBoolean(KEY_BATTERY_PROMPT_SHOWN, true).apply();
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.ad_analysis_battery_opt_title)
                .setMessage(R.string.ad_analysis_battery_opt_message)
                .setPositiveButton(R.string.ad_analysis_battery_opt_allow,
                        (d, w) -> openBatteryOptimizationSettings(context))
                .setNegativeButton(R.string.ad_analysis_battery_opt_later, null)
                .show();
    }

    private void openBatteryOptimizationSettings(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception ignored) {
                // No battery optimization settings screen available on this device.
            }
        }
    }

    private void showApiKeyMissingDialog(Context context) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.transcription_api_key_missing_title)
                .setMessage(R.string.transcription_api_key_missing_message)
                .setPositiveButton(R.string.open_settings, (d, w) -> {
                    Intent intent = new Intent(context, PreferenceActivity.class);
                    intent.putExtra(PreferenceActivity.OPEN_AI_SETTINGS, true);
                    context.startActivity(intent);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
