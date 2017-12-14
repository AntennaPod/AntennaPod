package de.danoeh.antennapod.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import de.danoeh.antennapod.core.service.playback.PlaybackService;

/**
 * Activity for controlling the remote playback on a Cast device.
 */
public class CastplayerActivity extends MediaplayerInfoActivity {
    public static final String TAG = "CastPlayerActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!PlaybackService.isCasting()) {
            Intent intent = PlaybackService.getPlayerActivityIntent(getContext());
            if (!intent.getComponent().getClassName().equals(CastplayerActivity.class.getName())) {
                getActivity().finish();
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onReloadNotification(int notificationCode) {
        if (notificationCode == PlaybackService.EXTRA_CODE_AUDIO) {
            Log.d(TAG, "ReloadNotification received, switching to Audioplayer now");
            saveCurrentFragment();
            getActivity().finish();
            startActivity(new Intent(getContext(), AudioplayerActivity.class));
        } else {
            super.onReloadNotification(notificationCode);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        if (butPlaybackSpeed != null) {
            butPlaybackSpeed.setVisibility(View.GONE);
        }
        return root;
    }

    @Override
    public void onResume() {
        if (!PlaybackService.isCasting()) {
            Intent intent = PlaybackService.getPlayerActivityIntent(getContext());
            if (!intent.getComponent().getClassName().equals(CastplayerActivity.class.getName())) {
                saveCurrentFragment();
                getActivity().finish();
                startActivity(intent);
            }
        }
        super.onResume();
    }

    @Override
    protected void onBufferStart() {
        //sbPosition.setIndeterminate(true);
        sbPosition.setEnabled(false);
    }

    @Override
    protected void onBufferEnd() {
        //sbPosition.setIndeterminate(false);
        sbPosition.setEnabled(true);
    }
}
