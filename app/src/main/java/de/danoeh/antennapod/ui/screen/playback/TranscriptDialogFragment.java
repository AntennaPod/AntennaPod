package de.danoeh.antennapod.ui.screen.playback;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
    private boolean doInitialScroll = true;
    private LinearLayoutManager layoutManager;

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
        viewBinding = TranscriptDialogBinding.inflate(getLayoutInflater());
        layoutManager = new LinearLayoutManager(getContext());
        viewBinding.transcriptList.setLayoutManager(layoutManager);

        adapter = new TranscriptAdapter(getContext(), this::transcriptClicked);
        viewBinding.transcriptList.setAdapter(adapter);
        viewBinding.transcriptList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    viewBinding.followAudioCheckbox.setChecked(false);
                }
            }
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(viewBinding.getRoot())
                .setPositiveButton(getString(R.string.close_label), null)
                .setNegativeButton(getString(R.string.refresh_label), null)
                .setTitle(R.string.transcript)
                .create();
        viewBinding.followAudioCheckbox.setChecked(true);
        dialog.setOnShowListener(dialog1 -> {
            dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(v -> {
                viewBinding.progLoading.setVisibility(View.VISIBLE);
                v.setClickable(false);
                v.setEnabled(false);
                loadMediaInfo(true);
            });
        });
        viewBinding.progLoading.setVisibility(View.VISIBLE);
        doInitialScroll = true;


        return dialog;
    }

    private void transcriptClicked(int pos, TranscriptSegment segment) {
        long startTime = segment.getStartTime();
        long endTime = segment.getEndTime();

        scrollToPosition(pos);
        if (!(controller.getPosition() >= startTime && controller.getPosition() <= endTime)) {
            controller.seekTo((int) startTime);
        } else {
            controller.playPause();
        }
        adapter.notifyItemChanged(pos);
        viewBinding.followAudioCheckbox.setChecked(true);
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
                doInitialScroll = true;
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

        if (!((FeedMedia) media).hasTranscript()) {
            dismiss();
            Toast.makeText(getContext(), R.string.no_transcript_label, Toast.LENGTH_LONG).show();
            return;
        }

        viewBinding.progLoading.setVisibility(View.GONE);
        adapter.setMedia(media);
        ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_NEGATIVE).setVisibility(View.INVISIBLE);
        if (!TextUtils.isEmpty(((FeedMedia) media).getItem().getTranscriptUrl())) {
            ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_NEGATIVE).setVisibility(View.VISIBLE);
            ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(true);
            ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_NEGATIVE).setClickable(true);
        }
    }

    public void scrollToPosition(int pos) {
        if (pos <= 0) {
            return;
        }
        if (!viewBinding.followAudioCheckbox.isChecked() && !doInitialScroll) {
            return;
        }
        doInitialScroll = false;

        boolean quickScroll = Math.abs(layoutManager.findFirstVisibleItemPosition() - pos) > 5;
        if (quickScroll) {
            viewBinding.transcriptList.scrollToPosition(pos - 1);
            // Additionally, smooth scroll, so that currently active segment is on top of screen
        }
        LinearSmoothScroller smoothScroller = new LinearSmoothScroller(getContext()) {
            @Override
            protected int getVerticalSnapPreference() {
                return LinearSmoothScroller.SNAP_TO_START;
            }

            protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                return (quickScroll ? 200 : 1000) / (float) displayMetrics.densityDpi;
            }
        };
        smoothScroller.setTargetPosition(pos - 1);
        layoutManager.startSmoothScroll(smoothScroller);
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