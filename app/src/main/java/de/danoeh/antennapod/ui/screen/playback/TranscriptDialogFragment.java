package de.danoeh.antennapod.ui.screen.playback;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.TranscriptDialogBinding;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.Transcript;
import de.danoeh.antennapod.model.feed.TranscriptSegment;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.playback.service.PlaybackController;
import de.danoeh.antennapod.ui.transcript.TranscriptUtils;
import io.reactivex.Maybe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class TranscriptDialogFragment extends DialogFragment {
    public static final String TAG = "TranscriptFragment";
    private TranscriptDialogBinding viewBinding;
    private PlaybackController controller;
    private Disposable disposable;
    private Playable media;
    private Transcript transcript;
    private TranscriptAdapter adapter = null;
    private CheckBox followAudioCheckbox;

    @Override
    public void onResume() {
        ViewGroup.LayoutParams params;
        params = getDialog().getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        getDialog().getWindow().setAttributes((WindowManager.LayoutParams) params);

        super.onResume();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        followAudioCheckbox = new CheckBox(getContext());
        followAudioCheckbox.setText(R.string.transcript_follow);
        followAudioCheckbox.setChecked(true);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(onCreateView(getLayoutInflater()))
                .setPositiveButton(getString(R.string.close_label), null)
                .setNegativeButton(getString(R.string.refresh_label), null)
                .setTitle(R.string.transcript)
                .create();
        // Replace the neutral button with a checkbox for following audio
        dialog.setOnShowListener(dialogInterface -> {
            Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            ViewGroup viewGroup = (ViewGroup) buttonNeutral.getParent();
            Space space = new Space(getContext());
            viewGroup.removeAllViews();
            viewGroup.addView(followAudioCheckbox);
            viewGroup.addView(space);
            space.setLayoutParams(new LinearLayout.LayoutParams(100, LinearLayout.LayoutParams.MATCH_PARENT));

            Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            viewGroup.addView(buttonNegative);
            viewGroup.addView(buttonPositive);
        });
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setVisibility(View.VISIBLE);
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(v -> {
            viewBinding.progLoading.setVisibility(View.VISIBLE);
            loadMediaInfo(true);
        });
        viewBinding.progLoading.setVisibility(View.VISIBLE);
        return dialog;
    }

    public View onCreateView(LayoutInflater inflater) {
        viewBinding = TranscriptDialogBinding.inflate(inflater);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setRecycleChildrenOnDetach(true);
        viewBinding.transcriptList.setLayoutManager(layoutManager);

        adapter = new TranscriptAdapter(getActivity(), (pos, segment) -> {
            transcriptClicked(pos, segment);
        });
        viewBinding.transcriptList.setAdapter(adapter);
        viewBinding.transcriptList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    followAudioCheckbox.setChecked(false);
                }
            }
        });


        return viewBinding.getRoot();
    }

    private void transcriptClicked(int pos, TranscriptSegment segment) {
        long startTime = segment.getStartTime();
        long endTime = segment.getEndTime();

        scrollToPosition(pos);
        if (! (controller.getPosition() >= startTime
                && controller.getPosition() <= endTime)) {
            controller.seekTo((int) startTime);
            Toast.makeText(getContext(), "Seeking to " + startTime, Toast.LENGTH_SHORT).show();
        } else {
            controller.playPause();
        }
        adapter.notifyItemChanged(pos);
    }

    @Override
    public void onStart() {
        super.onStart();
        controller = new PlaybackController(getActivity()) {
            @Override
            public void loadMediaInfo() {
                TranscriptDialogFragment.this.loadMediaInfo(false);
            }
        };
        controller.init();
        EventBus.getDefault().register(this);
        loadMediaInfo(false);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(PlaybackPositionEvent event) {
        if (transcript == null) {
            return;
        }
        int pos = transcript.findSegmentIndexBefore(event.getPosition());
        scrollToPosition(pos);
    }

    private void loadMediaInfo(boolean forceRefresh) {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Maybe.create(emitter -> {
            Playable media = controller.getMedia();
            if (media instanceof FeedMedia) {
                this.media = media;

                transcript = TranscriptUtils.loadTranscript((FeedMedia) this.media, forceRefresh);
                ((FeedMedia) this.media).setTranscript(transcript);
                emitter.onSuccess(this.media);
            } else {
                emitter.onComplete();
            }
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(media -> onMediaChanged((Playable) media),
                error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    private void onMediaChanged(Playable media) {
        if (!(media instanceof FeedMedia)) {
            return;
        }
        this.media = media;

        FeedMedia feedMedia = (FeedMedia) media;
        if (!feedMedia.hasTranscript()) {
            dismiss();
            Toast.makeText(getContext(), R.string.no_transcript_label, Toast.LENGTH_LONG).show();
            return;
        }

        viewBinding.progLoading.setVisibility(View.GONE);
        adapter.setMedia(media);
        ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_NEGATIVE).setVisibility(View.INVISIBLE);
        if (!TextUtils.isEmpty(((FeedMedia) media).getItem().getTranscriptUrl())) {
            ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_NEGATIVE).setVisibility(View.VISIBLE);
        }
    }

    public void scrollToPosition(Integer pos) {
        if (pos == null || pos <= 0) {
            return;
        }
        final LinearSmoothScroller smoothScroller = new LinearSmoothScroller(getContext()) {
            @Override
            protected int getVerticalSnapPreference() {
                return LinearSmoothScroller.SNAP_TO_START;
            }

            protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                return 1000.0F / (float) displayMetrics.densityDpi;
            }
        };
        int scrollPosition = ((LinearLayoutManager) viewBinding.transcriptList.getLayoutManager())
                .findFirstVisibleItemPosition();
        if (Math.abs(scrollPosition - pos) > 5) {
            if (followAudioCheckbox.isChecked()) {
                viewBinding.transcriptList.scrollToPosition(pos - 1);
            }
        }
        if (followAudioCheckbox.isChecked()) {
            smoothScroller.setTargetPosition(pos - 1);
            viewBinding.transcriptList.getLayoutManager().startSmoothScroll(smoothScroller);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (disposable != null) {
            disposable.dispose();
        }
        controller.release();
        controller = null;
        EventBus.getDefault().unregister(this);
    }

}