package de.danoeh.antennapod;

import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.jsoup.internal.StringUtil;

import de.danoeh.antennapod.core.util.PodcastIndexTranscriptUtils;
import de.danoeh.antennapod.model.feed.Transcript;
import de.danoeh.antennapod.model.feed.TranscriptSegment;
import de.danoeh.antennapod.parser.feed.PodcastIndexTranscriptParser;
import de.danoeh.antennapod.placeholder.PlaceholderContent.PlaceholderItem;
import de.danoeh.antennapod.databinding.FragmentItemTranscriptRvBinding;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * {@link RecyclerView.Adapter} that can display a {@link PlaceholderItem}.
 * TODO: Replace the implementation with code for your data type.
 */
public class ItemTranscriptRVAdapter extends RecyclerView.Adapter<ItemTranscriptRVAdapter.ViewHolder> {

    public String TAG = "ItemTranscriptRVAdapter";
    public Hashtable<Long, Integer> positions;
    public Hashtable<Integer, TranscriptSegment> snippets;

    private Transcript transcript;

    public ItemTranscriptRVAdapter(Transcript t) {
        positions = new Hashtable<Long, Integer>();
        snippets = new Hashtable<Integer, TranscriptSegment>();
        setTranscript(t);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        return new ViewHolder(FragmentItemTranscriptRvBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));

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
        TreeMap.Entry entry = (TreeMap.Entry) segmentsMap.entrySet().toArray()[position];
        TranscriptSegment seg = (TranscriptSegment) entry.getValue();
        Long k = (Long) entry.getKey();

        Log.d(TAG, "onBindViewHolder position " + position + " RV pos " + k);
        holder.mItem = seg;
        holder.mIdView.setText(PodcastIndexTranscriptParser.secondsToTime(k));
        if (! StringUtil.isBlank(seg.getSpeaker())) {
            holder.mContentView.setText(seg.getSpeaker() + " : " + seg.getWords());
        } else {
            holder.mContentView.setText(seg.getWords());
        }
    }

    @Override
    public int getItemCount() {
        if (transcript == null) {
            return 0;
        }
        return transcript.getSegmentsMap().size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView mIdView;
        public final TextView mContentView;
        public TranscriptSegment mItem;

        public ViewHolder(FragmentItemTranscriptRvBinding binding) {
            super(binding.getRoot());
            mIdView = binding.itemNumber;
            mContentView = binding.content;
            //mIdView.setVisibility(View.GONE);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}