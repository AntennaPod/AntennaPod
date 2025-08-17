package de.danoeh.antennapod.ui.screen.playback;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.elevation.SurfaceColors;
import de.danoeh.antennapod.databinding.TranscriptItemBinding;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.Transcript;
import de.danoeh.antennapod.model.feed.TranscriptSegment;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.ui.common.Converter;
import de.danoeh.antennapod.ui.transcript.TranscriptViewholder;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.ObjectUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jsoup.internal.StringUtil;

public class TranscriptAdapter extends RecyclerView.Adapter<TranscriptViewholder> {

    public String tag = "TranscriptAdapter";
    private final SegmentClickListener segmentClickListener;
    private final Context context;
    private FeedMedia media;
    private int prevHighlightPosition = -1;
    private int highlightPosition = -1;
    private boolean inMultiselectMode = false;
    private final HashSet<Integer> selectedPositions = new HashSet<>();

    public TranscriptAdapter(Context context, SegmentClickListener segmentClickListener) {
        this.context = context;
        this.segmentClickListener = segmentClickListener;
    }

    @NonNull
    @Override
    public TranscriptViewholder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        return new TranscriptViewholder(TranscriptItemBinding.inflate(LayoutInflater.from(context), viewGroup, false));
    }

    public void setMedia(Playable media) {
        if (!(media instanceof FeedMedia)) {
            return;
        }
        this.media = (FeedMedia) media;
        notifyDataSetChanged();
    }

    public void setMultiselectMode(boolean multiselectMode) {
        if (this.inMultiselectMode == multiselectMode) {
            return;
        }
        this.inMultiselectMode = multiselectMode;
        if (!multiselectMode) {
            selectedPositions.clear();
        }
        notifyDataSetChanged();
    }

    public boolean isMultiselectMode() {
        return inMultiselectMode;
    }

    @Override
    public void onBindViewHolder(@NonNull TranscriptViewholder holder, int position) {
        if (media == null || media.getTranscript() == null) {
            return;
        }

        TranscriptSegment seg = media.getTranscript().getSegmentAt(position);
        holder.viewContent.setOnClickListener(v -> {
            if (segmentClickListener != null)  {
                segmentClickListener.onTranscriptClicked(position, seg);
            }
        });

        holder.viewContent.setOnLongClickListener(v -> {
            if (segmentClickListener != null) {
                segmentClickListener.onTranscriptLongClicked(position, seg);
            }
            return true;
        });

        String timecode = Converter.getDurationStringLong((int) seg.getStartTime());
        if (!StringUtil.isBlank(seg.getSpeaker())) {
            if (position > 0 && media.getTranscript()
                    .getSegmentAt(position - 1).getSpeaker().equals(seg.getSpeaker())) {
                holder.viewTimecode.setVisibility(View.GONE);
                holder.viewContent.setText(seg.getWords());
            } else {
                holder.viewTimecode.setVisibility(View.VISIBLE);
                holder.viewTimecode.setText(timecode + " • " + seg.getSpeaker());
                holder.viewContent.setText(seg.getWords());
            }
        } else {
            Set<String> speakers = media.getTranscript().getSpeakers();
            if (speakers.isEmpty() && (position % 5 == 0)) {
                holder.viewTimecode.setVisibility(View.VISIBLE);
                holder.viewTimecode.setText(timecode);
            } else {
                holder.viewTimecode.setVisibility(View.GONE);
            }
            holder.viewContent.setText(seg.getWords());
        }

        if (inMultiselectMode) {
            highlightViewHolder(holder, selectedPositions.contains(position));
        } else {
            highlightViewHolder(holder, position == highlightPosition);
        }
    }

    private void highlightViewHolder(TranscriptViewholder holder, boolean highlight) {
        if (highlight) {
            float density = context.getResources().getDisplayMetrics().density;
            holder.viewContent.setBackgroundColor(SurfaceColors.getColorForElevation(context, 32 * density));
            holder.viewContent.setAlpha(1.0f);
            holder.viewTimecode.setAlpha(1.0f);
            holder.viewContent.setAlpha(1.0f);
        } else {
            holder.viewContent.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
            holder.viewContent.setAlpha(0.5f);
            holder.viewTimecode.setAlpha(0.5f);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(PlaybackPositionEvent event) {
        if (media == null || media.getTranscript() == null) {
            return;
        }
        int index = media.getTranscript().findSegmentIndexBefore(event.getPosition());
        if (index < 0 || index > media.getTranscript().getSegmentCount()) {
            return;
        }
        if (prevHighlightPosition != highlightPosition) {
            prevHighlightPosition = highlightPosition;
        }
        if (index != highlightPosition) {
            highlightPosition = index;
            notifyItemChanged(prevHighlightPosition);
            notifyItemChanged(highlightPosition);
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
        return media.getTranscript().getSegmentCount();
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

    public void toggleSelection(int pos) {
        if (selectedPositions.contains(pos)) {
            selectedPositions.remove(pos);
        } else {
            selectedPositions.add(pos);
        }
        notifyItemChanged(pos);
    }

    public void selectAll() {
        if (media == null || media.getTranscript() == null) {
            return;
        }
        selectedPositions.clear();
        int count = getItemCount();
        for (int i = 0; i < count; i++) {
            selectedPositions.add(i);
        }
        notifyDataSetChanged();
    }

    public String getSelectedText() {
        if (!inMultiselectMode) {
            return null;
        }
        Transcript transcript = media.getTranscript();
        StringBuilder ss = new StringBuilder();
        String lastSpeaker = null;
        if (selectedPositions.isEmpty()) {
            return "";
        }
        java.util.List<Integer> sortedPositions = new java.util.ArrayList<>(selectedPositions);
        java.util.Collections.sort(sortedPositions);
        int prevIndex = -2;
        for (int index : sortedPositions) {
            if (prevIndex != -2 && index != prevIndex + 1) {
                ss.append("\n[...]\n");
            }
            TranscriptSegment seg = transcript.getSegmentAt(index);
            if (!StringUtil.isBlank(seg.getSpeaker())) {
                if (ObjectUtils.notEqual(lastSpeaker, seg.getSpeaker())) {
                    ss.append("\n").append(seg.getSpeaker()).append(" : ");
                    lastSpeaker = seg.getSpeaker();
                }
            } else {
                lastSpeaker = null;
            }
            if (!TextUtils.isEmpty(ss) && ss.charAt(ss.length() - 1) != ' ') {
                ss.append(' ');
            }
            ss.append(seg.getWords());
            prevIndex = index;
        }

        return ss.toString().strip();
    }

    public interface SegmentClickListener {
        void onTranscriptClicked(int position, TranscriptSegment seg);

        void onTranscriptLongClicked(int position, TranscriptSegment seg);
    }
}