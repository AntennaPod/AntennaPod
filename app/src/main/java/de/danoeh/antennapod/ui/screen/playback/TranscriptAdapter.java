package de.danoeh.antennapod.ui.screen.playback;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jsoup.internal.StringUtil;

import de.danoeh.antennapod.databinding.TranscriptItemBinding;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.TranscriptSegment;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.ui.common.Converter;
import de.danoeh.antennapod.ui.common.ThemeUtils;
import de.danoeh.antennapod.ui.transcript.TranscriptViewholder;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class TranscriptAdapter extends RecyclerView.Adapter<TranscriptViewholder> {
    public String tag = "TranscriptAdapter";
    private SegmentClickListener segmentClickListener;
    private final Context context;
    private FeedMedia media;
    int prevHighlightPosition = -1;
    int highlightPosition = -1;

    public TranscriptAdapter(Context context, SegmentClickListener segmentClickListener) {
        this.context = context;
        this.segmentClickListener = segmentClickListener;
    }

    @NonNull
    @Override
    public TranscriptViewholder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        TranscriptViewholder v = new TranscriptViewholder(
                TranscriptItemBinding.inflate(LayoutInflater.from(viewGroup.getContext()),
                viewGroup,
                false));
        return v;
    }

    public void setMedia(Playable media) {
        if (! (media instanceof FeedMedia)) {
            return;
        }
        this.media = (FeedMedia) media;
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull TranscriptViewholder holder, int position) {
        if (media == null || media.getTranscript() == null) {
            return;
        }

        TreeMap<Long, TranscriptSegment> segmentsMap;
        segmentsMap = media.getTranscript().getSegmentsMap();
        TranscriptSegment seg = media.getTranscript().getSegmentAt(position);
        int k = Math.toIntExact(media.getTranscript().getTimeCode(position));
        holder.viewContent.setOnClickListener(v -> {
            if (segmentClickListener != null)  {
                segmentClickListener.onTranscriptClicked(position, seg);
            }
        });

        holder.viewTimecode.setText(Converter.getDurationStringLong(k));
        holder.viewTimecode.setVisibility(View.GONE);
        Set<String> speakers = media.getTranscript().getSpeakers();

        if (! StringUtil.isBlank(seg.getSpeaker())) {
            TreeMap.Entry prevEntry;
            if (position == 0) {
                prevEntry = null;
            } else {
                prevEntry = (TreeMap.Entry) segmentsMap.entrySet().toArray()[position - 1];
            }
            TranscriptSegment prevSeg = null;
            if (prevEntry != null) {
                prevSeg = (TranscriptSegment) prevEntry.getValue();
            }
            if (prevEntry != null && prevSeg.getSpeaker().equals(seg.getSpeaker())) {
                holder.viewTimecode.setVisibility(View.GONE);
                holder.viewContent.setText(seg.getWords());
            } else {
                holder.viewTimecode.setVisibility(View.VISIBLE);
                holder.viewTimecode.setText(Converter.getDurationStringLong(k) + " " + seg.getSpeaker());
                holder.viewContent.setText(seg.getWords());
            }
        } else {
            if (speakers.size() == 0 && (position % 5 == 0)) {
                holder.viewTimecode.setVisibility(View.VISIBLE);
                holder.viewTimecode.setText(Converter.getDurationStringLong(k));
            }
            holder.viewContent.setText(seg.getWords());
        }

        if (position == highlightPosition) {
            holder.viewContent.setTypeface(null, Typeface.BOLD);
            holder.viewContent.setTextColor(
                    ThemeUtils.getColorFromAttr(context, android.R.attr.textColorPrimary)
            );
        } else {
            holder.viewContent.setTypeface(null, Typeface.NORMAL);
            holder.viewContent.setTextColor(
                    ThemeUtils.getColorFromAttr(context, android.R.attr.textColorSecondary));
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(PlaybackPositionEvent event) {
        TreeMap<Long, TranscriptSegment> segmentsMap;
        segmentsMap = media.getTranscript().getSegmentsMap();
        Map.Entry<Long, TranscriptSegment> entry = segmentsMap.floorEntry((long) event.getPosition());
        if (entry != null) {
            if (prevHighlightPosition != highlightPosition) {
                prevHighlightPosition = highlightPosition;
            }
            if (media.getTranscript().getIndex(entry) != highlightPosition) {
                highlightPosition = media.getTranscript().getIndex(entry);
                notifyItemChanged(prevHighlightPosition);
                notifyItemChanged(highlightPosition);
            }
        }
    }

    @Override
    public int getItemCount() {
        if (media == null) {
            return 0;
        }

        if (media.getTranscript() == null) {
            return 0;
        }
        return media.getTranscript().getSegmentsMap().size();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        EventBus.getDefault().unregister(this);
    }


    public interface SegmentClickListener {
        void onTranscriptClicked(int position, TranscriptSegment seg);
    }
}