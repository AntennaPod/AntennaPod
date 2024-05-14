package de.danoeh.antennapod.ui.screen.playback;

import android.content.Context;
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
import de.danoeh.antennapod.model.feed.TranscriptSegment;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.ui.common.Converter;
import de.danoeh.antennapod.ui.transcript.TranscriptViewholder;
import java.util.Set;
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
            }
            holder.viewContent.setText(seg.getWords());
        }

        if (position == highlightPosition) {
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


    public interface SegmentClickListener {
        void onTranscriptClicked(int position, TranscriptSegment seg);
    }
}