package de.danoeh.antennapod.ui.screen.playback;

import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.danoeh.antennapod.playback.service.PlaybackController;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jsoup.internal.StringUtil;

import de.danoeh.antennapod.databinding.FragmentItemTranscriptRvBinding;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.model.feed.Transcript;
import de.danoeh.antennapod.model.feed.TranscriptSegment;
import de.danoeh.antennapod.parser.transcript.TranscriptParser;
import de.danoeh.antennapod.playback.base.PlayerStatus;

import java.util.Hashtable;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * {@link RecyclerView.Adapter} that can display a {@link PlaceholderItem}.
 * TODO: Replace the implementation with code for your data type.
 */
public class TranscriptAdapter extends RecyclerView.Adapter<TranscriptAdapter.ViewHolder> {

    public String tag = "ItemTranscriptRVAdapter";
    public Hashtable<Long, Integer> positions;
    public Hashtable<Integer, TranscriptSegment> snippets;
    PlaybackController controller;

    private Transcript transcript;

    public TranscriptAdapter(Transcript t) {
        positions = new Hashtable<Long, Integer>();
        snippets = new Hashtable<Integer, TranscriptSegment>();
        setTranscript(t);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        return new ViewHolder(FragmentItemTranscriptRvBinding.inflate(LayoutInflater.from(parent.getContext()),
                parent,
                false));

    }

    public void setController(PlaybackController controller) {
        this.controller = controller;
    }

    public void setTranscript(Transcript t) {
        transcript = t;
        if (transcript == null) {
            return;
        }
        TreeMap<Long, TranscriptSegment> segmentsMap = transcript.getSegmentsMap();
        Object[] objs = segmentsMap.entrySet().toArray();
        for (int i = 0; i < objs.length; i++) {
            Map.Entry<Long, TranscriptSegment> seg;
            seg = (Map.Entry<Long, TranscriptSegment>) objs[i];
            positions.put((Long) seg.getKey(), i);
            snippets.put(i, seg.getValue());
        }
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        TreeMap<Long, TranscriptSegment> segmentsMap;
        SortedMap<Long, TranscriptSegment> map;

        segmentsMap = transcript.getSegmentsMap();
        // TODO: fix this performance
        TreeMap.Entry entry = (TreeMap.Entry) segmentsMap.entrySet().toArray()[position];
        TranscriptSegment seg = (TranscriptSegment) entry.getValue();
        Long k = (Long) entry.getKey();

        Log.d(tag, "onBindViewHolder position " + position + " RV pos " + k);
        holder.transcriptSegment = seg;
        holder.viewTimecode.setText(TranscriptParser.secondsToTime(k));
        holder.viewTimecode.setVisibility(View.GONE);
        if (! StringUtil.isBlank(seg.getSpeaker())) {
            TreeMap.Entry prevEntry = null;
            try {
                prevEntry = (TreeMap.Entry) segmentsMap.entrySet().toArray()[position - 1];
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.d(tag, "ArrayIndexOutOfBoundsException");
            }
            TranscriptSegment prevSeg = null;
            if (prevEntry != null) {
                prevSeg = (TranscriptSegment) prevEntry.getValue();
            }
            if (prevEntry != null && prevSeg.getSpeaker().equals(seg.getSpeaker())) {
                holder.viewTimecode.setVisibility(View.GONE);
                holder.viewContent.setText(seg.getWords());
            } else {
                holder.viewTimecode.setText(TranscriptParser.secondsToTime(k) + " " + seg.getSpeaker());
                holder.viewContent.setText(seg.getWords());
                holder.viewTimecode.setVisibility(View.VISIBLE);
            }
        } else {
            holder.viewContent.setText(seg.getWords());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(PlaybackPositionEvent event) {
        Log.d(tag, "onEventMainThread ItemTranscriptRVAdapter");
    }

    @Override
    public int getItemCount() {
        if (transcript == null) {
            return 0;
        }
        return transcript.getSegmentsMap().size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView viewTimecode;
        public final TextView viewContent;
        public TranscriptSegment transcriptSegment;

        public ViewHolder(FragmentItemTranscriptRvBinding binding) {
            super(binding.getRoot());
            viewTimecode = binding.speaker;
            viewContent = binding.content;
            viewContent.setOnClickListener(v -> {
                Log.d(tag, "Clicked on " + transcriptSegment.getWords());
                long startTime = transcriptSegment.getStartTime();
                long endTime = transcriptSegment.getEndTime();
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
            });
        }

        @Override
        public String toString() {
            return super.toString() + " '" + viewContent.getText() + "'";
        }
    }
}