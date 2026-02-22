package de.danoeh.antennapod.ui.screen.playback;

import android.app.Dialog;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.Tracks;
import androidx.media3.common.TrackSelectionOverride;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.event.PlayerStatusEvent;
import de.danoeh.antennapod.playback.service.PlaybackController;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class PlaybackControlsDialog extends DialogFragment {
    private AlertDialog dialog;
    private final List<TrackOption> trackOptions = new ArrayList<>();

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
        final ViewGroup container = dialog.findViewById(R.id.track_selection_container);

        PlaybackController.bindToMedia3Service(requireContext(), controller -> {
            Tracks tracks = controller.getCurrentTracks();
            trackOptions.clear();
            for (Tracks.Group group : tracks.getGroups()) {
                if (group.getType() != C.TRACK_TYPE_AUDIO || !group.isSupported()) {
                    continue;
                }
                for (int i = 0; i < group.length; i++) {
                    if (!group.isTrackSupported(i)) {
                        continue;
                    }
                    Format format = group.getTrackFormat(i);
                    String label = formatLabel(format, i);
                    trackOptions.add(new TrackOption(group.getMediaTrackGroup(), i, label, group.isTrackSelected(i)));
                }
            }

            if (getActivity() == null || !isAdded()) {
                return;
            }
            getActivity().runOnUiThread(() -> {
                container.removeAllViews();
                int margin = (int) (8 * getResources().getDisplayMetrics().density);
                for (int idx = 0; idx < trackOptions.size(); idx++) {
                    Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_tag_chip, container, false);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.setMargins(margin, 0, margin, 0);
                    chip.setLayoutParams(lp);
                    TrackOption opt = trackOptions.get(idx);
                    chip.setText(opt.label);
                    chip.setChecked(opt.selected);
                    final int trackIndex = idx;
                    chip.setOnClickListener(v -> {
                        selectTrack(trackIndex);
                        chip.setChecked(true);
                    });
                    container.addView(chip);
                }
            });
        });
    }

    private static String formatLabel(Format format, int trackIndex) {
        if (format.label != null && !format.label.isEmpty()) {
            return format.label;
        }
        if (format.language != null && !format.language.isEmpty()) {
            return format.language;
        }
        return "Track " + (trackIndex + 1);
    }

    private void selectTrack(int optionIndex) {
        if (optionIndex < 0 || optionIndex >= trackOptions.size()) {
            return;
        }
        TrackOption opt = trackOptions.get(optionIndex);

        PlaybackController.bindToMedia3Service(requireContext(), controller -> {
            controller.setTrackSelectionParameters(controller.getTrackSelectionParameters()
                    .buildUpon()
                    .setOverrideForType(new TrackSelectionOverride(opt.mediaTrackGroup, opt.trackIndex))
                    .build());
        });
    }

    private static final class TrackOption {
        final TrackGroup mediaTrackGroup;
        final int trackIndex;
        final String label;
        final boolean selected;

        TrackOption(TrackGroup mediaTrackGroup, int trackIndex, String label, boolean selected) {
            this.mediaTrackGroup = mediaTrackGroup;
            this.trackIndex = trackIndex;
            this.label = label;
            this.selected = selected;
        }
    }
}
