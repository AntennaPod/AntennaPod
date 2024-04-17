package de.danoeh.antennapod.ui.screen.playback;

import android.app.Dialog;
import android.content.Context;
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

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import de.danoeh.antennapod.databinding.TranscriptDialogBinding;
import de.danoeh.antennapod.playback.base.PlayerStatus;
import de.danoeh.antennapod.playback.service.PlaybackController;
import de.danoeh.antennapod.storage.database.DBReader;
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

public class TranscriptFragment extends DialogFragment {
    public static final String TAG = "TranscriptFragment";
    private TranscriptDialogBinding viewBinding;
    private PlaybackController controller;
    Transcript transcript;
    TreeMap<Long, TranscriptSegment> segmentsMap;
    TranscriptAdapter adapter = null;
    TranscriptViewholder currentView = null;
    TranscriptViewholder prevView = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        return dialog;
    }

    public View onCreateView(LayoutInflater inflater) {
        viewBinding = TranscriptDialogBinding.inflate(inflater);
        RecyclerView viewTranscript = viewBinding.transcriptList;

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setRecycleChildrenOnDetach(true);
        viewTranscript.setLayoutManager(layoutManager);

        controller = new PlaybackController(getActivity()) {
            @Override
            public void loadMediaInfo() {
                load();
            }
        };
        controller.init();

        Playable media = controller.getMedia();
        if (media != null && media instanceof FeedMedia) {
            FeedMedia feedMedia = ((FeedMedia) media);
            if (feedMedia.getItem() == null) {
                feedMedia.setItem(DBReader.getFeedItem(feedMedia.getItemId()));
            }

            transcript = PodcastIndexTranscriptUtils.loadTranscript(feedMedia);
            if (transcript != null) {
                segmentsMap = transcript.getSegmentsMap();
                adapter = new TranscriptAdapter(transcript, (pos, segment) -> {
                    transcriptClicked(pos, segment);
                });
                viewTranscript.setAdapter(adapter);
            }
        }
        return viewBinding.getRoot();
    }

    private void transcriptClicked(int pos, TranscriptSegment segment) {
        long startTime = segment.getStartTime();
        long endTime = segment.getEndTime();

        // scrollToPosition(startTime);
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
                // TODO
            }
        };
        controller.init();
        EventBus.getDefault().register(this);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(PlaybackPositionEvent event) {
        scrollToPosition(event.getPosition());
    }


    private void load() {
        Log.d(TAG, "load()");
        Context context = getContext();
        if (context == null) {
            return;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void scrollToPosition(long playPosition) {
        if (segmentsMap == null) {
            return;
        }
        Map.Entry<Long, TranscriptSegment> entry = segmentsMap.floorEntry(playPosition);
        if (entry != null) {
            Integer pos = transcript.getIndex(entry);
            RecyclerView rv = viewBinding.transcriptList;
            if (pos != null) {
                final LinearSmoothScroller smoothScroller = new LinearSmoothScroller(getActivity()) {
                    @Override
                    protected int getVerticalSnapPreference() {
                        return LinearSmoothScroller.SNAP_TO_START;
                    }
                };
                //rv.setTop(pos - 1);
                smoothScroller.setTargetPosition(pos - 1);  // pos on which item you want to scroll recycler view
                rv.getLayoutManager().startSmoothScroll(smoothScroller);

                TranscriptViewholder nextView = (TranscriptViewholder) rv.getChildViewHolder(rv.getLayoutManager().findViewByPosition(pos));
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
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        controller.release();
        controller = null;
        EventBus.getDefault().unregister(this);
    }

}