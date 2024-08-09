package de.danoeh.antennapod.ui.screen.playback;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.event.playback.SpeedChangedEvent;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.playback.service.PlaybackController;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.view.ItemOffsetDecoration;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class VariableSpeedDialog extends BottomSheetDialogFragment {
    private SpeedSelectionAdapter adapter;
    private PlaybackController controller;
    private final List<Float> selectedSpeeds;
    private PlaybackSpeedSeekBar speedSeekBar;
    private Chip addCurrentSpeedChip;
    private MaterialButtonToggleGroup skipSilenceToggleGroup;

    public VariableSpeedDialog() {
        DecimalFormatSymbols format = new DecimalFormatSymbols(Locale.US);
        format.setDecimalSeparator('.');
        selectedSpeeds = new ArrayList<>(UserPreferences.getPlaybackSpeedArray());
    }

    @Override
    public void onStart() {
        super.onStart();
        controller = new PlaybackController(getActivity()) {
            @Override
            public void loadMediaInfo() {
                updateSpeed(new SpeedChangedEvent(controller.getCurrentPlaybackSpeedMultiplier()));
                updateSkipSilence(controller.getCurrentPlaybackSkipSilence());
            }
        };
        controller.init();
        EventBus.getDefault().register(this);
        updateSpeed(new SpeedChangedEvent(controller.getCurrentPlaybackSpeedMultiplier()));
        updateSkipSilence(controller.getCurrentPlaybackSkipSilence());
    }

    @Override
    public void onStop() {
        super.onStop();
        controller.release();
        controller = null;
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateSpeed(SpeedChangedEvent event) {
        speedSeekBar.updateSpeed(event.getNewSpeed());
        addCurrentSpeedChip.setText(String.format(Locale.getDefault(), "%1$.2f", event.getNewSpeed()));
    }

    public void updateSkipSilence(FeedPreferences.SkipSilence skipSilence) {
        int id = View.NO_ID;
        switch (skipSilence) {
            case OFF: id = R.id.skipSilenceOff; break;
            case MEDIUM: id = R.id.skipSilenceMedium; break;
            case AGGRESSIVE: id = R.id.skipSilenceAggressive; break;
        }
        skipSilenceToggleGroup.check(id);
    }

    private void onSkipSilenceChanged(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
        FeedPreferences.SkipSilence opt;
        final int id = group.getCheckedButtonId();
        if (id == R.id.skipSilenceOff) opt = FeedPreferences.SkipSilence.OFF;
        else if (id == R.id.skipSilenceMedium) opt = FeedPreferences.SkipSilence.MEDIUM;
        else if (id == R.id.skipSilenceAggressive) opt = FeedPreferences.SkipSilence.AGGRESSIVE;
        else opt = FeedPreferences.SkipSilence.GLOBAL;
        if (UserPreferences.getSkipSilence() != opt)
            UserPreferences.setSkipSilence(opt);
        if (controller != null) {
            controller.setSkipSilence(opt);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = View.inflate(getContext(), R.layout.speed_select_dialog, null);
        speedSeekBar = root.findViewById(R.id.speed_seek_bar);
        speedSeekBar.setProgressChangedListener(multiplier -> {
            UserPreferences.setPlaybackSpeed(multiplier);
            if (controller != null) {
                controller.setPlaybackSpeed(multiplier);
            }
        });
        RecyclerView selectedSpeedsGrid = root.findViewById(R.id.selected_speeds_grid);
        selectedSpeedsGrid.setLayoutManager(new GridLayoutManager(getContext(), 3));
        selectedSpeedsGrid.addItemDecoration(new ItemOffsetDecoration(getContext(), 4));
        adapter = new SpeedSelectionAdapter();
        adapter.setHasStableIds(true);
        selectedSpeedsGrid.setAdapter(adapter);

        addCurrentSpeedChip = root.findViewById(R.id.add_current_speed_chip);
        addCurrentSpeedChip.setCloseIconVisible(true);
        addCurrentSpeedChip.setCloseIconResource(R.drawable.ic_add);
        addCurrentSpeedChip.setOnCloseIconClickListener(v -> addCurrentSpeed());
        addCurrentSpeedChip.setCloseIconContentDescription(getString(R.string.add_preset));
        addCurrentSpeedChip.setOnClickListener(v -> addCurrentSpeed());

        skipSilenceToggleGroup = root.findViewById(R.id.skipSilence);
        skipSilenceToggleGroup.addOnButtonCheckedListener(
            (group, checkedId, isChecked) -> onSkipSilenceChanged(group, checkedId, isChecked));
        updateSkipSilence(UserPreferences.getSkipSilence());
        return root;
    }

    private void addCurrentSpeed() {
        float newSpeed = speedSeekBar.getCurrentSpeed();
        if (selectedSpeeds.contains(newSpeed)) {
            Snackbar.make(addCurrentSpeedChip,
                    getString(R.string.preset_already_exists, newSpeed), Snackbar.LENGTH_LONG).show();
        } else {
            selectedSpeeds.add(newSpeed);
            Collections.sort(selectedSpeeds);
            UserPreferences.setPlaybackSpeedArray(selectedSpeeds);
            adapter.notifyDataSetChanged();
        }
    }

    public class SpeedSelectionAdapter extends RecyclerView.Adapter<SpeedSelectionAdapter.ViewHolder> {

        @Override
        @NonNull
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Chip chip = new Chip(getContext());
            chip.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            return new ViewHolder(chip);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            float speed = selectedSpeeds.get(position);

            holder.chip.setText(String.format(Locale.getDefault(), "%1$.2f", speed));
            holder.chip.setOnLongClickListener(v -> {
                selectedSpeeds.remove(speed);
                UserPreferences.setPlaybackSpeedArray(selectedSpeeds);
                notifyDataSetChanged();
                return true;
            });
            holder.chip.setOnClickListener(v -> {
                UserPreferences.setPlaybackSpeed(speed);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (controller != null) {
                        controller.setPlaybackSpeed(speed);
                        dismiss();
                    }
                }, 200);
            });
        }

        @Override
        public int getItemCount() {
            return selectedSpeeds.size();
        }

        @Override
        public long getItemId(int position) {
            return selectedSpeeds.get(position).hashCode();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            Chip chip;

            ViewHolder(Chip itemView) {
                super(itemView);
                chip = itemView;
            }
        }
    }
}
