package de.danoeh.antennapod.ui.screen.playback;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.fragment.app.DialogFragment;
import android.widget.Button;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.event.PlayerStatusEvent;
import de.danoeh.antennapod.playback.service.PlaybackController;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

public class PlaybackControlsDialog extends DialogFragment {
    private AlertDialog dialog;

    public static PlaybackControlsDialog newInstance() {
        Bundle arguments = new Bundle();
        PlaybackControlsDialog dialog = new PlaybackControlsDialog();
        dialog.setArguments(arguments);
        return dialog;
    }

    public PlaybackControlsDialog() {
        // Empty constructor required for DialogFragment
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        setupAudioTracks();
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerStatusEvent(PlayerStatusEvent event) {
        setupAudioTracks();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        dialog = new MaterialAlertDialogBuilder(getContext())
                .setTitle(R.string.audio_controls)
                .setView(R.layout.audio_controls)
                .setPositiveButton(R.string.close_label, null).create();
        return dialog;
    }

    private void setupAudioTracks() {
        final Button butAudioTracks = dialog.findViewById(R.id.audio_tracks);
        PlaybackController.bindToService(getActivity(), playbackService -> {
            List<String> audioTracks = playbackService.getAudioTracks();
            int selectedAudioTrack = playbackService.getSelectedAudioTrack();
            if (audioTracks.size() < 2 || selectedAudioTrack < 0) {
                butAudioTracks.setVisibility(View.GONE);
                return;
            }
            butAudioTracks.setVisibility(View.VISIBLE);
            butAudioTracks.setText(audioTracks.get(selectedAudioTrack));
        });
        butAudioTracks.setOnClickListener(v -> {
            PlaybackController.bindToService(getActivity(), playbackService -> {
                List<String> audioTracks = playbackService.getAudioTracks();
                int selectedAudioTrack = playbackService.getSelectedAudioTrack();
                playbackService.setAudioTrack((selectedAudioTrack + 1) % audioTracks.size());
                new Handler(Looper.getMainLooper()).postDelayed(this::setupAudioTracks, 500);
            });
        });
    }
}
