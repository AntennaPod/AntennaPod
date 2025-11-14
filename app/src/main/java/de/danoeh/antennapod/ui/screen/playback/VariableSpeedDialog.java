package de.danoeh.antennapod.ui.screen.playback;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.event.playback.SpeedChangedEvent;
import de.danoeh.antennapod.playback.service.PlaybackController;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.view.ItemOffsetDecoration;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class VariableSpeedDialog extends BottomSheetDialogFragment {
    private static final String TAG = "VariableSpeedDialog";
    private SpeedSelectionAdapter adapter;
    private PlaybackController controller;
    private final List<Float> selectedSpeeds;
    private PlaybackSpeedSeekBar speedSeekBar;
    private Chip addCurrentSpeedChip;
    private CheckBox skipSilenceCheckbox;
    private Disposable disposable;

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
                VariableSpeedDialog.this.loadMediaInfo();
            }
        };
        controller.init();
        EventBus.getDefault().register(this);
        loadMediaInfo();
    }

    @Override
    public void onStop() {
        super.onStop();
        controller.release();
        controller = null;
        EventBus.getDefault().unregister(this);
        if (disposable != null) {
            disposable.dispose();
        }
    }

    private void loadMediaInfo() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(() -> {
            if (controller == null) {
                return null;
            }
            // Make sure the media is loaded in case getCurrentPlaybackSpeedMultiplier has to access it
            return controller.getMedia();
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pair -> {
                    if (controller == null) {
                        return;
                    }
                    updateSpeed(new SpeedChangedEvent(controller.getCurrentPlaybackSpeedMultiplier()));
                    updateSkipSilence(controller.getCurrentPlaybackSkipSilence());
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateSpeed(SpeedChangedEvent event) {
        speedSeekBar.updateSpeed(event.getNewSpeed());
        addCurrentSpeedChip.setText(String.format(Locale.getDefault(), "%1$.2f", event.getNewSpeed()));
    }

    public void updateSkipSilence(boolean skipSilence) {
        skipSilenceCheckbox.setChecked(skipSilence);
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

        skipSilenceCheckbox = root.findViewById(R.id.skipSilence);
        skipSilenceCheckbox.setChecked(UserPreferences.isSkipSilence());
        skipSilenceCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            UserPreferences.setSkipSilence(isChecked);
            controller.setSkipSilence(isChecked);
        });
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
