package de.danoeh.antennapod.ui.screen.playback;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
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
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.TranscriptDialogBinding;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.Transcript;
import de.danoeh.antennapod.model.feed.TranscriptSegment;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.playback.base.PlayerStatus;
import de.danoeh.antennapod.playback.service.PlaybackController;
import de.danoeh.antennapod.ui.chapters.PodcastIndexTranscriptUtils;
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
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(onCreateView(getLayoutInflater()))
                .setPositiveButton(getString(R.string.close_label), null)
                .setNegativeButton(getString(R.string.refresh_label), null)
                .setTitle(R.string.transcript)
                .create();

        CheckBox checkBox = new CheckBox(getContext());
        checkBox.setText(R.string.transcript_follow);
        checkBox.setChecked(true);

        // Set the CheckBox as a custom button
        dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "", (dialogInterface, i) -> {});
        dialog.setOnShowListener(dialogInterface -> {
            Button buttonNeutral = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEUTRAL);
            LinearLayout ll = (LinearLayout) buttonNeutral.getParent();

            LinearLayout.LayoutParams neutralButtonLL = (LinearLayout.LayoutParams) buttonNeutral.getLayoutParams();

            ViewGroup viewGroup = (ViewGroup) buttonNeutral.getParent();
            viewGroup.addView(checkBox, neutralButtonLL);
            neutralButtonLL.width = LinearLayout.LayoutParams.WRAP_CONTENT;

            Button buttonPositive = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
            Button buttonNegative = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE);
            ll.removeView(buttonPositive);
            ll.removeView(buttonNegative);
            ll.removeView(buttonNeutral); // we are replacing this with a checkbox
            ll.addView(buttonNegative);
            ll.addView(buttonPositive);
        });
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setVisibility(View.VISIBLE);
        dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setVisibility(View.VISIBLE);
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

        if (getDialog() != null && getDialog().getWindow() != null) {
            Drawable dialogFragmentBackground = getDialog().getWindow().getDecorView().getBackground();
            viewBinding.followAudio.setBackground(dialogFragmentBackground);
        }

        return viewBinding.getRoot();
    }

    private void transcriptClicked(int pos, TranscriptSegment segment) {
        long startTime = segment.getStartTime();
        long endTime = segment.getEndTime();

        scrollToPosition(pos);
        if (! (controller.getPosition() >= startTime
                && controller.getPosition() <= endTime)) {
            controller.seekTo((int) startTime);

            if (controller.getStatus() == PlayerStatus.PAUSED
                    || controller.getStatus() == PlayerStatus.STOPPED) {
                controller.playPause();
            }
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
                TranscriptDialogFragment.this.loadMediaInfo(true);
            }
        };
        controller.init();
        EventBus.getDefault().register(this);
        loadMediaInfo(true);
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

                transcript = PodcastIndexTranscriptUtils.loadTranscript((FeedMedia) this.media);
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
        ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_NEUTRAL).setVisibility(View.INVISIBLE);
        if (!TextUtils.isEmpty(((FeedMedia) media).getItem().getPodcastIndexTranscriptUrl())) {
            ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_NEUTRAL).setVisibility(View.VISIBLE);
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
        Log.d(TAG, "scrollToPosition: " + scrollPosition + " pos: " + pos );
        if (Math.abs(scrollPosition - pos) > 5) {
            // if we never manually scroll, we don't show the follow audio checkbox
            if (viewBinding.followAudio.isChecked()) {
                viewBinding.transcriptList.scrollToPosition(pos - 1);
            }
        }
        if (viewBinding.followAudio.isChecked()) {
            smoothScroller.setTargetPosition(pos - 1);  // pos on which item you want to scroll recycler view
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