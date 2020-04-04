package de.danoeh.antennapod.core.util.playback;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceHolder;

import org.antennapod.audio.MediaPlayer;

import de.danoeh.antennapod.core.preferences.UserPreferences;

import java.util.Collections;
import java.util.List;

public class AudioPlayer extends MediaPlayer implements IPlayer {
    private static final String TAG = "AudioPlayer";

    public AudioPlayer(Context context) {
        super(context);
        PreferenceManager.getDefaultSharedPreferences(context)
                .registerOnSharedPreferenceChangeListener((sharedPreferences, key) -> {
                    if (key.equals(UserPreferences.PREF_MEDIA_PLAYER)) {
                        checkMpi();
                    }
                });
    }

    @Override
    public void setDisplay(SurfaceHolder sh) {
        if (sh != null) {
            Log.e(TAG, "Setting display not supported in Audio Player");
            throw new UnsupportedOperationException("Setting display not supported in Audio Player");
        }
    }

    @Override
    public void setPlaybackParams(float speed, boolean skipSilence) {
        if (canSetSpeed()) {
            setPlaybackSpeed(speed);
        }
        //Default player does not support silence skipping
    }

    @Override
    protected boolean useSonic() {
        return UserPreferences.useSonic();
    }

    @Override
    protected boolean downmix() {
        return UserPreferences.stereoToMono();
    }

    public List<String> getAudioTracks() {
        return Collections.emptyList();
    }

    @Override
    public void setAudioTrack(int track) {
    }

    @Override
    public int getSelectedAudioTrack() {
        return -1;
    }
}
