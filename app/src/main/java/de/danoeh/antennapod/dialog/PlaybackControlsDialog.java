package de.danoeh.antennapod.dialog;

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
import android.widget.CheckBox;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.playback.PlaybackController;
import java.util.List;

public class PlaybackControlsDialog extends DialogFragment {
    private PlaybackController controller;
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
        controller = new PlaybackController(getActivity()) {
            @Override
            public void loadMediaInfo() {
                setupUi();
                setupAudioTracks();
            }
        };
        controller.init();
        setupUi();
    }

    @Override
    public void onStop() {
        super.onStop();
        controller.release();
        controller = null;
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

    private void setupUi() {
        final CheckBox skipSilence = dialog.findViewById(R.id.skipSilence);
        skipSilence.setChecked(UserPreferences.isSkipSilence());
        skipSilence.setOnCheckedChangeListener((buttonView, isChecked) -> {
            UserPreferences.setSkipSilence(isChecked);
            controller.setSkipSilence(isChecked);
        });
    }

    private void setupAudioTracks() {
        List<String> audioTracks = controller.getAudioTracks();
        int selectedAudioTrack = controller.getSelectedAudioTrack();
        final Button butAudioTracks = dialog.findViewById(R.id.audio_tracks);
        if (audioTracks.size() < 2 || selectedAudioTrack < 0) {
            butAudioTracks.setVisibility(View.GONE);
            return;
        }

        butAudioTracks.setVisibility(View.VISIBLE);
        butAudioTracks.setText(audioTracks.get(selectedAudioTrack));
        butAudioTracks.setOnClickListener(v -> {
            controller.setAudioTrack((selectedAudioTrack + 1) % audioTracks.size());
            new Handler(Looper.getMainLooper()).postDelayed(this::setupAudioTracks, 500);
        });
    }
}
