package de.danoeh.antennapod.ui.screen.playback;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.media3.common.TrackSelectionParameters;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.playback.service.PlaybackController;

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

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        dialog = new MaterialAlertDialogBuilder(getContext())
                .setTitle(R.string.audio_controls)
                .setView(R.layout.audio_controls)
                .setPositiveButton(R.string.close_label, null).create();
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        populateTrackList();
    }

    private void populateTrackList() {
        trackOptions.clear();
        ViewGroup container = dialog.findViewById(R.id.track_selection_container);
        container.removeAllViews();

        PlaybackController.bindToMedia3Service(requireContext(), controller -> {
            Tracks tracks = controller.getCurrentTracks();
            for (Tracks.Group group : tracks.getGroups()) {
                if (group.getType() != C.TRACK_TYPE_AUDIO || !group.isSupported()) {
                    continue;
                }
                for (int i = 0; i < group.length; i++) {
                    if (!group.isTrackSupported(i)) {
                        continue;
                    }
                    Format format = group.getTrackFormat(i);
                    String label = formatLabel(format, group.length, i);
                    trackOptions.add(new TrackOption(group.getMediaTrackGroup(), i, label, group.isTrackSelected(i)));
                }
            }

            requireActivity().runOnUiThread(() -> {
                if (getActivity() == null || !isAdded()) {
                    return;
                }
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
                    chip.setOnClickListener(v -> selectTrack(trackIndex));
                    container.addView(chip);
                }
            });
        });
    }

    private static String formatLabel(Format format, int groupLength, int trackIndex) {
        if (format.label != null && !format.label.isEmpty()) {
            return format.label;
        }
        if (format.language != null && !format.language.isEmpty()) {
            return format.language;
        }
        if (groupLength > 1) {
            return "Track " + (trackIndex + 1);
        }
        return "Audio";
    }

    private void selectTrack(int optionIndex) {
        if (optionIndex < 0 || optionIndex >= trackOptions.size()) {
            return;
        }
        TrackOption opt = trackOptions.get(optionIndex);

        PlaybackController.bindToMedia3Service(requireContext(), controller -> {
            TrackSelectionParameters params = controller.getTrackSelectionParameters()
                    .buildUpon()
                    .setOverrideForType(new TrackSelectionOverride(opt.mediaTrackGroup, opt.trackIndex))
                    .build();
            controller.setTrackSelectionParameters(params);
            requireActivity().runOnUiThread(() ->
                    new Handler(Looper.getMainLooper()).postDelayed(this::populateTrackList, 500));
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
