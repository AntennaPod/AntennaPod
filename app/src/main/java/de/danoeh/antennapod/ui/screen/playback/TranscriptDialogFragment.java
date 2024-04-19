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
import de.danoeh.antennapod.playback.base.PlayerStatus;
import de.danoeh.antennapod.playback.service.PlaybackController;
import de.danoeh.antennapod.ui.chapters.PodcastIndexTranscriptUtils;
import io.reactivex.Maybe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.util.Map;
import java.util.TreeMap;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class TranscriptDialogFragment extends DialogFragment {
    public static final String TAG = "TranscriptFragment";
    private TranscriptDialogBinding viewBinding;
    private PlaybackController controller;
    private Disposable disposable;
    Playable media;
    Transcript transcript;
    TreeMap<Long, TranscriptSegment> segmentsMap;
    TranscriptAdapter adapter = null;

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
                .setNeutralButton(getString(R.string.refresh_label), null)
                .setTitle(R.string.transcript)
                .create();
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setVisibility(View.INVISIBLE);
        dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(v -> {
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
        scrollToPlayPosition(event.getPosition());
    }

    private void loadMediaInfo(boolean forceRefresh) {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Maybe.create(emitter -> {
            Playable feedMedia = controller.getMedia();
            if (feedMedia != null && feedMedia instanceof FeedMedia) {
                this.media = feedMedia;

                transcript = PodcastIndexTranscriptUtils.loadTranscript((FeedMedia) this.media);
                ((FeedMedia) this.media).setTranscript(transcript);

                if (transcript != null) {
                    segmentsMap = transcript.getSegmentsMap();
                }
                emitter.onSuccess(media);
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
        if (media == null || ! (media instanceof FeedMedia)) {
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

    public void scrollToPlayPosition(long playPosition) {
        if (segmentsMap == null) {
            return;
        }
        Map.Entry<Long, TranscriptSegment> entry = segmentsMap.floorEntry(playPosition);
        if (entry != null) {
            if (transcript == null) {
                return;
            }
            Integer pos = transcript.getIndex(entry);
            scrollToPosition(pos);
        }
    }

    public void scrollToPosition(Integer pos) {
        if (pos == null || pos <= 0) {
            return;
        }
        int scrollPosition = ((LinearLayoutManager) viewBinding.transcriptList.getLayoutManager())
                .findFirstVisibleItemPosition();
        if (Math.abs(scrollPosition - pos) > 5) {
            // Too far, no smooth scroll
            viewBinding.transcriptList.scrollToPosition(pos - 1);
            return;
        }
        final LinearSmoothScroller smoothScroller = new LinearSmoothScroller(getContext()) {
            @Override
            protected int getVerticalSnapPreference() {
                return LinearSmoothScroller.SNAP_TO_START;
            }

            protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                return 1000.0F / (float)displayMetrics.densityDpi;
            }
        };
        smoothScroller.setTargetPosition(pos - 1);  // pos on which item you want to scroll recycler view
        viewBinding.transcriptList.getLayoutManager().startSmoothScroll(smoothScroller);
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