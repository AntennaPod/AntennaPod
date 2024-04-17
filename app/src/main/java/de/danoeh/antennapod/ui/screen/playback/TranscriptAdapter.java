package de.danoeh.antennapod.ui.screen.playback;

import static java.security.AccessController.getContext;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jsoup.internal.StringUtil;

import de.danoeh.antennapod.databinding.FragmentItemTranscriptRvBinding;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.model.feed.Transcript;
import de.danoeh.antennapod.model.feed.TranscriptSegment;
import de.danoeh.antennapod.parser.transcript.TranscriptParser;
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
    public Hashtable<Long, Integer> positions;
    public Hashtable<Integer, TranscriptSegment> snippets;

    private Transcript transcript;

    public TranscriptAdapter(Transcript t) {
        positions = new Hashtable<Long, Integer>();
        snippets = new Hashtable<Integer, TranscriptSegment>();
        setTranscript(t);
    }

    @NonNull
    @Override
    public TranscriptViewholder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        return new TranscriptViewholder(FragmentItemTranscriptRvBinding.inflate(LayoutInflater.from(viewGroup.getContext()),
                viewGroup,
                false));

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
    public void onBindViewHolder(@NonNull TranscriptViewholder holder, int position) {
        TreeMap<Long, TranscriptSegment> segmentsMap;
        SortedMap<Long, TranscriptSegment> map;

        segmentsMap = transcript.getSegmentsMap();
        // TODO: fix this performance problem with getting a new Array
        TreeMap.Entry entry = (TreeMap.Entry) segmentsMap.entrySet().toArray()[position];
        TranscriptSegment seg = (TranscriptSegment) entry.getValue();
        Long k = (Long) entry.getKey();

        Log.d(tag, "onBindTranscriptViewholder position " + position + " RV pos " + k);
        holder.transcriptSegment = seg;
        holder.viewTimecode.setText(TranscriptParser.secondsToTime(k));
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
                holder.viewTimecode.setText(TranscriptParser.secondsToTime(k) + " " + seg.getSpeaker());
                holder.viewContent.setText(seg.getWords());
            }
        } else {
            if (speakers.size() <= 0 && (position % 5 == 0)) {
                holder.viewTimecode.setVisibility(View.VISIBLE);
                holder.viewTimecode.setText(TranscriptParser.secondsToTime(k));
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