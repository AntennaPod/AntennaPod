package de.danoeh.antennapod.ui.screen.playback;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import de.danoeh.antennapod.databinding.TranscriptDialogBinding;
import de.danoeh.antennapod.playback.base.PlayerStatus;
import de.danoeh.antennapod.playback.service.PlaybackController;
import de.danoeh.antennapod.ui.chapters.PodcastIndexTranscriptUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Map;
import java.util.TreeMap;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.Transcript;
import de.danoeh.antennapod.model.feed.TranscriptSegment;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.ui.common.ThemeUtils;
import de.danoeh.antennapod.ui.transcript.TranscriptViewholder;
import io.reactivex.Maybe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class TranscriptFragment extends DialogFragment {
    public static final String TAG = "TranscriptFragment";
    private TranscriptDialogBinding viewBinding;
    private PlaybackController controller;
    private Disposable disposable;
    Playable media;
    Transcript transcript;
    TreeMap<Long, TranscriptSegment> segmentsMap;
    private ProgressBar progressBar;
    TranscriptAdapter adapter = null;
    TranscriptViewholder currentView = null;
    TranscriptViewholder prevView = null;
    RecyclerView viewTranscript;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public TranscriptFragment initMedia(Playable m) {
        media = m;
        return this;
    }

    @Override
    public void onResume() {
        ViewGroup.LayoutParams params;
        params = getDialog().getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        getDialog().getWindow().setAttributes((WindowManager.LayoutParams) params);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        super.onResume();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(onCreateView(getLayoutInflater()))
                .setPositiveButton(getString(R.string.close_label), null) //dismisses
                .setTitle(R.string.transcript)
                .create();
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setVisibility(View.INVISIBLE);
        dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            loadMediaInfo(true);
        });
        progressBar.setVisibility(View.VISIBLE);

        return dialog;
    }

    public View onCreateView(LayoutInflater inflater) {
        viewBinding = TranscriptDialogBinding.inflate(inflater);
        viewTranscript = viewBinding.transcriptList;
        progressBar = viewBinding.progLoading;

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setRecycleChildrenOnDetach(true);
        viewTranscript.setLayoutManager(layoutManager);

        adapter = new TranscriptAdapter((pos, segment) -> {
            transcriptClicked(pos, segment);
        });
        viewTranscript.setAdapter(adapter);
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
    }

    @Override
    public void onStart() {
        super.onStart();
        controller = new PlaybackController(getActivity()) {
            @Override
            public void loadMediaInfo() {
                TranscriptFragment.this.loadMediaInfo(true);
            }
        };
        controller.init();
        EventBus.getDefault().register(this);
        loadMediaInfo(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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

        progressBar.setVisibility(View.GONE);
        adapter.setMedia(media);
        ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_NEUTRAL).setVisibility(View.INVISIBLE);
        if (!TextUtils.isEmpty(((FeedMedia) media).getItem().getPodcastIndexTranscriptUrl())) {
            ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_NEUTRAL).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
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
            Log.d(TAG, "scrollToPlayPosition " + playPosition + " RV pos" + pos);
            scrollToPosition(pos);
        }
    }

    public void scrollToPosition(Integer pos) {
        RecyclerView rv = viewBinding.transcriptList;
        if (rv == null) {
            return;
        }
        if (pos == null) {
            return;
        }

        final LinearSmoothScroller smoothScroller = new LinearSmoothScroller(getActivity()) {
            @Override
            protected int getVerticalSnapPreference() {
                return LinearSmoothScroller.SNAP_TO_START;
            }
        };
        if (pos > 0) {
            smoothScroller.setTargetPosition(pos - 1);  // pos on which item you want to scroll recycler view
            rv.getLayoutManager().startSmoothScroll(smoothScroller);
        }

        RecyclerView.LayoutManager lm = rv.getLayoutManager();
        if (lm == null) {
            return;
        }
        View v = lm.findViewByPosition(pos);
        if (v == null) {
            return;
        }
        TranscriptViewholder nextView =
                (TranscriptViewholder) rv.getChildViewHolder(v);
        if (nextView != null && nextView != currentView) {
            prevView = currentView;
            currentView = nextView;
        }
        if (currentView != null) {
            currentView.viewContent.setTypeface(null, Typeface.BOLD);
            currentView.viewContent.setTextColor(
                    ThemeUtils.getColorFromAttr(getContext(), android.R.attr.textColorPrimary)
            );
        }

        if (prevView != null && prevView != currentView && currentView != null) {
            prevView.viewContent.setTypeface(null, Typeface.NORMAL);
            prevView.viewContent.setTextColor(
                    ThemeUtils.getColorFromAttr(getContext(), android.R.attr.textColorSecondary));
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