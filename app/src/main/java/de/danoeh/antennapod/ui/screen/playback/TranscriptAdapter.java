package de.danoeh.antennapod.ui.screen.playback;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jsoup.internal.StringUtil;

import de.danoeh.antennapod.databinding.TranscriptItemBinding;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.model.feed.Transcript;
import de.danoeh.antennapod.model.feed.TranscriptSegment;
import de.danoeh.antennapod.parser.transcript.TranscriptParser;
import de.danoeh.antennapod.ui.common.Converter;
import de.danoeh.antennapod.ui.transcript.TranscriptViewholder;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * {@link RecyclerView.Adapter} that can display a {@link PlaceholderItem}.
 * TODO: Replace the implementation with code for your data type.
 */
public class TranscriptAdapter extends RecyclerView.Adapter<TranscriptViewholder> {

    public String tag = "ItemTranscriptRVAdapter";

    private Transcript transcript;

    public TranscriptAdapter(Transcript t) {
        setTranscript(t);
    }

    @NonNull
    @Override
    public TranscriptViewholder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        return new TranscriptViewholder(TranscriptItemBinding.inflate(LayoutInflater.from(viewGroup.getContext()),
                viewGroup,
                false));

    }

    public void setTranscript(Transcript t) {
        transcript = t;
    }

    @Override
    public void onBindViewHolder(@NonNull TranscriptViewholder holder, int position) {
        TreeMap<Long, TranscriptSegment> segmentsMap;
        SortedMap<Long, TranscriptSegment> map;

        segmentsMap = transcript.getSegmentsMap();
        // TODO: fix this performance problem with getting a new Array
        TranscriptSegment seg = transcript.getSegmentAt(position);
        int k = Math.toIntExact((Long) transcript.getTimeCode(position));

        Log.d(tag, "onBindTranscriptViewholder position " + position + " RV pos " + k);
        holder.transcriptSegment = seg;
        holder.viewTimecode.setText(Converter.getDurationStringLong(k));
        holder.viewTimecode.setVisibility(View.GONE);
        Set<String> speakers = transcript.getSpeakers();

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
            if (prevEntry != null && prevSeg.getSpeaker().equals(seg.getSpeaker()) ) {
                holder.viewTimecode.setVisibility(View.GONE);
                holder.viewContent.setText(seg.getWords());
            } else {
                holder.viewTimecode.setVisibility(View.VISIBLE);
                holder.viewTimecode.setText(Converter.getDurationStringLong(k) + " " + seg.getSpeaker());
                holder.viewContent.setText(seg.getWords());
            }
        } else {
            if (speakers.size() <= 0 && (position % 5 == 0)) {
                holder.viewTimecode.setVisibility(View.VISIBLE);
                holder.viewTimecode.setText(Converter.getDurationStringLong(k));
            }
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
}