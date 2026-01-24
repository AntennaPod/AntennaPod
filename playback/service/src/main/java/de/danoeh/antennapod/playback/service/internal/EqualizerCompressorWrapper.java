package de.danoeh.antennapod.playback.service.internal;

import android.content.Context;
import android.media.audiofx.DynamicsProcessing;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import de.danoeh.antennapod.event.settings.CompressorPreferenceChangedEvent;
import de.danoeh.antennapod.event.settings.EqualizerPreferenceChangedEvent;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

public class EqualizerCompressorWrapper {
    static final float[] EQUALIZER_BAND_CUTOFFS = {100, 230, 430, 775, 1750, 4250, 8500, 13000, 15000, 20000};
    static final int EQUALIZER_BAND_COUNT = EQUALIZER_BAND_CUTOFFS.length;
    private static final String TAG = "EqualizerCompressorWrpr";

    @Nullable
    private DynamicsProcessing dynamicsProcessing = null;

    @SuppressWarnings({"this-escape", "unused"})
    public EqualizerCompressorWrapper(@Nullable Context context) {
        EventBus.getDefault().register(this);
    }

    public void initDynamicsProcessing(int audioSessionId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return;
        }

        DynamicsProcessing.Config config = buildDynamicsProcessingConfig();
        if (config == null) {
            return;
        }
        initDynamicsProcessingMbc(config);          // Set up MBC (multi band compressor) initial config
        initDynamicsProcessingEqualizer(config);    // Set up post equalizer initial config

        // Create DynamicsProcessing object and subobjects and attach to the supplied audioSessionId
        boolean enableDynProc = shallDynamicsProcessingBeEnabled();
        DynamicsProcessing oldDynamicsProcessing = this.dynamicsProcessing;
        synchronized (this) {
            this.dynamicsProcessing = new DynamicsProcessing(0, audioSessionId, config);
            this.dynamicsProcessing.setEnabled(enableDynProc);
        }
        Log.i(TAG, "DynamicsProcessing Enabled=" + this.dynamicsProcessing.getEnabled());

        if (oldDynamicsProcessing != null) {
            oldDynamicsProcessing.release();
        }
    }

    @Nullable
    private static DynamicsProcessing.Config buildDynamicsProcessingConfig() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return null;
        }

        int channelCount = 1;
        boolean preEqInUse = false;
        boolean mbcInUse = true;
        int mbcBandCount = 1;
        boolean postEqInUse = true;
        boolean limiterInUse = false;
        DynamicsProcessing.Config.Builder builder = new DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                channelCount,
                preEqInUse, EQUALIZER_BAND_COUNT,
                mbcInUse, mbcBandCount,
                postEqInUse, EQUALIZER_BAND_COUNT,
                limiterInUse);
        builder.setPreferredFrameDuration(25);
        return builder.build();
    }

    private void initDynamicsProcessingMbc(DynamicsProcessing.Config config) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return;
        }

        DynamicsProcessing.Mbc mbc = config.getMbcByChannelIndex(0);
        config.setMbcAllChannelsTo(mbc);

        DynamicsProcessing.MbcBand mbcBand = mbc.getBand(0);
        setMbcBandParameters(
                mbcBand,
                UserPreferences.isCompressorEnabled(),
                UserPreferences.getCompressorThreshold(),
                UserPreferences.getCompressorRatio(),
                UserPreferences.getCompressorAttackTime(),
                UserPreferences.getCompressorReleaseTime(),
                UserPreferences.getCompressorNoiseGateThreshold(),
                UserPreferences.getCompressorPostGain());
        applyMbcBand(config, mbcBand);
    }

    private void initDynamicsProcessingEqualizer(DynamicsProcessing.Config config) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return;
        }

        DynamicsProcessing.Eq postEq = config.getPostEqByChannelIndex(0);
        config.setPostEqAllChannelsTo(postEq);

        boolean isPostEqEnabled = UserPreferences.isEqualizerEnabled();
        float[] userPreferencesGains = UserPreferences.getEqualizerGains();
        if (userPreferencesGains.length != EQUALIZER_BAND_COUNT) {
            Log.e(TAG, "Invalid equalizer preferences band count: " + EQUALIZER_BAND_COUNT + " bands exist, but "
                    + userPreferencesGains.length + "are set in preferences!");
            isPostEqEnabled = false;
        } else if (postEq.getBandCount() != EQUALIZER_BAND_COUNT) {
            Log.e(TAG, "Invalid post equalizer band count: " + EQUALIZER_BAND_COUNT + " requested, but "
                    + postEq.getBandCount() + "are available!");
            isPostEqEnabled = false;
        }

        setPostEqualizerParameters(postEq, isPostEqEnabled, userPreferencesGains);
        applyPostEqualizer(config, postEq);
    }

    private boolean shallDynamicsProcessingBeEnabled() {
        return shallDynamicsProcessingBeEnabled(
                UserPreferences.isCompressorEnabled(),
                UserPreferences.isEqualizerEnabled());
    }

    private boolean shallDynamicsProcessingBeEnabled(boolean isCompressorEnabled, boolean isEqualizerEnabled) {
        return isCompressorEnabled || isEqualizerEnabled;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    public void compressorPresetChanged(CompressorPreferenceChangedEvent event) {
        setAndApplyMbcBandParameters(
                this.dynamicsProcessing,
                event.enabled(),
                event.threshold(),
                event.ratio(),
                event.attackTime(),
                event.releaseTime(),
                event.noiseGateThreshold(),
                event.postGain());
    }

    private synchronized void setAndApplyMbcBandParameters(
            DynamicsProcessing dynProc, boolean enabled,
            float threshold, float ratio, float attackTime, float releaseTime, float noiseGateThreshold, float postGain
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return;
        }

        if (dynProc.getChannelCount() < 1) {
            Log.e(TAG, "No channel found to apply MbcBandParameters! "
                    + "(getChannelCount()=" + dynProc.getChannelCount() + ")");
            return;
        }
        DynamicsProcessing.Mbc mbc = dynProc.getMbcByChannelIndex(0);
        if (mbc.getBandCount() < 1) {
            Log.e(TAG, "No band found to apply MbcBandParameters! (getBandCount()=" + mbc.getBandCount() + ")");
            return;
        }
        DynamicsProcessing.MbcBand mbcBand = mbc.getBand(0);

        boolean enableDynProc = shallDynamicsProcessingBeEnabled(enabled, UserPreferences.isEqualizerEnabled());
        if (!enableDynProc) {
            dynProc.setEnabled(false);
        }

        setMbcBandParameters(mbcBand, enabled, threshold, ratio, attackTime, releaseTime, noiseGateThreshold, postGain);
        applyMbcBand(dynProc, mbcBand);

        if (enableDynProc) {
            dynProc.setEnabled(true);
            Log.i(TAG, "Dynamics Processing enabled=" + dynProc.getEnabled());
        }
    }

    private void setMbcBandParameters(
            DynamicsProcessing.MbcBand mbcBand, boolean enabled,
            float threshold, float ratio, float attackTime, float releaseTime, float noiseGateThreshold, float postGain
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return;
        }

        if (enabled) {
            mbcBand.setPreGain(0);
            mbcBand.setExpanderRatio(20);
            mbcBand.setCutoffFrequency(20000);

            mbcBand.setThreshold(threshold);
            mbcBand.setRatio(ratio);
            mbcBand.setAttackTime(attackTime);
            mbcBand.setReleaseTime(releaseTime);
            mbcBand.setNoiseGateThreshold(noiseGateThreshold);
            mbcBand.setPostGain(postGain);

            mbcBand.setEnabled(true);
        } else {
            mbcBand.setEnabled(false);

            mbcBand.setThreshold(-45);
            mbcBand.setRatio(1);
            mbcBand.setAttackTime(3.0f);
            mbcBand.setReleaseTime(80.0f);
            mbcBand.setNoiseGateThreshold(-90);
            mbcBand.setPostGain(0);
        }
    }

    private void applyMbcBand(DynamicsProcessing.Config config, DynamicsProcessing.MbcBand mbcBand) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            logMbcBandParameters("applyMbcBand(config, mbcBand)", mbcBand);
            config.setMbcBandAllChannelsTo(0, mbcBand);
        }
    }

    private void applyMbcBand(DynamicsProcessing dynProc, DynamicsProcessing.MbcBand mbcBand) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            logMbcBandParameters("applyMbcBand(dynProc, mbcBand)", mbcBand);
            dynProc.setMbcBandAllChannelsTo(0, mbcBand);
        }
    }

    private void logMbcBandParameters(String funcName, DynamicsProcessing.MbcBand mbcBand) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Log.d(TAG, funcName + " Enabled=" + mbcBand.isEnabled()
                    + " AttackTime=" + mbcBand.getAttackTime() + " ReleaseTime=" + mbcBand.getReleaseTime()
                    + " PreGain=" + mbcBand.getPreGain() + " PostGain=" + mbcBand.getPostGain()
                    + " Threshold=" + mbcBand.getThreshold() + " Ratio=" + mbcBand.getRatio()
                    + " KneeWidth=" + mbcBand.getKneeWidth() + " CutoffFrequency=" + mbcBand.getCutoffFrequency()
                    + " NoiseGateThreshold=" + mbcBand.getNoiseGateThreshold()
                    + " ExpanderRatio=" + mbcBand.getExpanderRatio());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    public void equalizerPresetChanged(EqualizerPreferenceChangedEvent event) {
        setAndApplyPostEqualizerParameters(
                this.dynamicsProcessing,
                event.enabled(),
                event.gains());
    }

    private synchronized void setAndApplyPostEqualizerParameters(
            DynamicsProcessing dynProc, boolean enabled, float[] gains
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return;
        }

        if (dynProc.getChannelCount() < 1) {
            return;
        }
        DynamicsProcessing.Eq postEq = dynProc.getPostEqByChannelIndex(0);
        if (postEq.getBandCount() != EQUALIZER_BAND_COUNT) {
            return;
        }

        boolean enableDynProc = shallDynamicsProcessingBeEnabled(UserPreferences.isCompressorEnabled(), enabled);
        if (!enableDynProc) {
            dynProc.setEnabled(false);
        }

        setPostEqualizerParameters(postEq, enabled, gains);
        applyPostEqualizer(dynProc, postEq);

        if (enableDynProc) {
            dynProc.setEnabled(true);
        }
    }

    private void setPostEqualizerParameters(DynamicsProcessing.Eq postEq, boolean enabled, float[] gains) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return;
        }

        for (int i = 0; i < EQUALIZER_BAND_COUNT; i++) {
            DynamicsProcessing.EqBand band = postEq.getBand(i);
            band.setEnabled(enabled);
            band.setGain(gains[i]);
            band.setCutoffFrequency(EQUALIZER_BAND_CUTOFFS[i]);
        }
    }

    private void applyPostEqualizer(DynamicsProcessing.Config config, DynamicsProcessing.Eq postEq) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            config.setPostEqAllChannelsTo(postEq);
        }
    }

    private void applyPostEqualizer(DynamicsProcessing dynProc, DynamicsProcessing.Eq postEq) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            dynProc.setPostEqAllChannelsTo(postEq);
        }
    }
}