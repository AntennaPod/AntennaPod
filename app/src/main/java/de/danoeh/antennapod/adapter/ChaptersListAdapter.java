package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.EmbeddedChapterImage;
import de.danoeh.antennapod.core.util.IntentUtils;
import de.danoeh.antennapod.core.util.ThemeUtils;
import de.danoeh.antennapod.core.util.playback.Playable;
import de.danoeh.antennapod.view.CircularProgressBar;

public class ChaptersListAdapter extends RecyclerView.Adapter<ChaptersListAdapter.ChapterHolder> {
    private Playable media;
    private final Callback callback;
    private final Context context;
    private int currentChapterIndex = -1;
    private long currentChapterPosition = -1;
    private boolean hasImages = false;

    public ChaptersListAdapter(Context context, Callback callback) {
        this.callback = callback;
        this.context = context;
    }

    public void setMedia(Playable media) {
        this.media = media;
        hasImages = false;
        if (media.getChapters() != null) {
            for (Chapter chapter : media.getChapters()) {
                if (!ignoreChapter(chapter) && !TextUtils.isEmpty(chapter.getImageUrl())) {
                    hasImages = true;
                }
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull ChapterHolder holder, int position) {
        Chapter sc = getItem(position);
        holder.title.setText(sc.getTitle());
        holder.start.setText(Converter.getDurationStringLong((int) sc
                .getStart()));

        long duration;
        if (position + 1 < media.getChapters().size()) {
            duration = media.getChapters().get(position + 1).getStart() - sc.getStart();
        } else {
            duration = media.getDuration() - sc.getStart();
        }
        holder.duration.setText(context.getString(R.string.chapter_duration,
                Converter.getDurationStringLocalized(context, (int) duration)));

        if (TextUtils.isEmpty(sc.getLink())) {
            holder.link.setVisibility(View.GONE);
        } else {
            holder.link.setVisibility(View.VISIBLE);
            holder.link.setText(sc.getLink());
            holder.link.setOnClickListener(v -> IntentUtils.openInBrowser(context, sc.getLink()));
        }
        holder.secondaryActionIcon.setImageResource(ThemeUtils.getDrawableFromAttr(context, R.attr.av_play));
        holder.secondaryActionButton.setContentDescription(context.getString(R.string.play_chapter));
        holder.secondaryActionButton.setOnClickListener(v -> {
            if (callback != null) {
                callback.onPlayChapterButtonClicked(position);
            }
        });

        if (position == currentChapterIndex) {
            int playingBackGroundColor = ThemeUtils.getColorFromAttr(context, R.attr.currently_playing_background);
            holder.itemView.setBackgroundColor(playingBackGroundColor);
            float progress = ((float) (currentChapterPosition - sc.getStart())) / duration;
            progress = Math.max(progress, CircularProgressBar.MINIMUM_PERCENTAGE);
            progress = Math.min(progress, CircularProgressBar.MAXIMUM_PERCENTAGE);
            holder.progressBar.setPercentage(progress, position);
            holder.secondaryActionIcon.setImageResource(ThemeUtils.getDrawableFromAttr(context, R.attr.av_replay));
        } else {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
            holder.progressBar.setPercentage(0, null);
        }

        if (hasImages) {
            holder.image.setVisibility(View.VISIBLE);
            if (TextUtils.isEmpty(sc.getImageUrl())) {
                Glide.with(context).clear(holder.image);
            } else {
                Glide.with(context)
                        .load(EmbeddedChapterImage.getModelFor(media, position))
                        .apply(new RequestOptions()
                                .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                                .dontAnimate()
                                .transforms(new FitCenter(), new RoundedCorners((int)
                                        (4 * context.getResources().getDisplayMetrics().density))))
                        .into(holder.image);
            }
        } else {
            holder.image.setVisibility(View.GONE);
        }
    }

    @NonNull
    @Override
    public ChapterHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        return new ChapterHolder(inflater.inflate(R.layout.simplechapter_item, parent, false));
    }

    @Override
    public int getItemCount() {
        if (media == null || media.getChapters() == null) {
            return 0;
        }
        // ignore invalid chapters
        int counter = 0;
        for (Chapter chapter : media.getChapters()) {
            if (!ignoreChapter(chapter)) {
                counter++;
            }
        }
        return counter;
    }

    static class ChapterHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView start;
        final TextView link;
        final TextView duration;
        final ImageView image;
        final View secondaryActionButton;
        final ImageView secondaryActionIcon;
        final CircularProgressBar progressBar;

        public ChapterHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.txtvTitle);
            start = itemView.findViewById(R.id.txtvStart);
            link = itemView.findViewById(R.id.txtvLink);
            image = itemView.findViewById(R.id.imgvCover);
            duration = itemView.findViewById(R.id.txtvDuration);
            secondaryActionButton = itemView.findViewById(R.id.secondaryActionButton);
            secondaryActionIcon = itemView.findViewById(R.id.secondaryActionIcon);
            progressBar = itemView.findViewById(R.id.secondaryActionProgress);
        }
    }

    public void notifyChapterChanged(int newChapterIndex) {
        currentChapterIndex = newChapterIndex;
        currentChapterPosition = getItem(newChapterIndex).getStart();
        notifyDataSetChanged();
    }

    public void notifyTimeChanged(long timeMs) {
        currentChapterPosition = timeMs;
        // Passing an argument prevents flickering.
        // See EpisodeItemListAdapter.notifyItemChangedCompat.
        notifyItemChanged(currentChapterIndex, "foo");
    }

    private boolean ignoreChapter(Chapter c) {
        return media.getDuration() > 0 && media.getDuration() < c.getStart();
    }

    public Chapter getItem(int position) {
        int i = 0;
        for (Chapter chapter : media.getChapters()) {
            if (!ignoreChapter(chapter)) {
                if (i == position) {
                    return chapter;
                } else {
                    i++;
                }
            }
        }
        return null;
    }

    public interface Callback {
        void onPlayChapterButtonClicked(int position);
    }

}
