package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.feed.util.ImageResourceUtils;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.util.ChapterUtils;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.EmbeddedChapterImage;
import de.danoeh.antennapod.core.util.IntentUtils;
import de.danoeh.antennapod.core.util.ThemeUtils;
import de.danoeh.antennapod.core.util.playback.Playable;

public class ChaptersListAdapter extends ArrayAdapter<Chapter> {
    private static final String TAG = "ChapterListAdapter";

    private Playable media;
    private final Callback callback;
    private int currentChapterIndex = -1;
    private boolean hasImages = false;

    public ChaptersListAdapter(Context context, int textViewResourceId, Callback callback) {
        super(context, textViewResourceId);
        this.callback = callback;
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

    @NonNull
    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
        Holder holder;

        Chapter sc = getItem(position);

        // Inflate Layout
        if (convertView == null) {
            holder = new Holder();
            LayoutInflater inflater = (LayoutInflater) getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            convertView = inflater.inflate(R.layout.simplechapter_item, parent, false);
            holder.view = convertView;
            holder.title = convertView.findViewById(R.id.txtvTitle);
            holder.start = convertView.findViewById(R.id.txtvStart);
            holder.link = convertView.findViewById(R.id.txtvLink);
            holder.image = convertView.findViewById(R.id.imgvCover);
            holder.duration = convertView.findViewById(R.id.txtvDuration);
            holder.secondaryActionButton = convertView.findViewById(R.id.secondaryActionButton);
            holder.secondaryActionIcon = convertView.findViewById(R.id.secondaryActionIcon);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();

        }

        holder.title.setText(sc.getTitle());
        holder.start.setText(Converter.getDurationStringLong((int) sc
                .getStart()));

        long duration;
        if (position + 1 < media.getChapters().size()) {
            duration = media.getChapters().get(position + 1).getStart() - sc.getStart();
        } else {
            duration = media.getDuration() - sc.getStart();
        }
        holder.duration.setText(getContext().getString(R.string.chapter_duration,
                Converter.getDurationStringLong((int) duration)));

        if (sc.getLink() == null) {
            holder.link.setVisibility(View.GONE);
        } else {
            holder.link.setVisibility(View.VISIBLE);
            holder.link.setText(sc.getLink());
            holder.link.setOnClickListener(v -> IntentUtils.openInBrowser(getContext(), sc.getLink()));
        }
        holder.secondaryActionIcon.setImageResource(ThemeUtils.getDrawableFromAttr(getContext(), R.attr.av_play));
        holder.secondaryActionButton.setOnClickListener(v -> {
            if (callback != null) {
                callback.onPlayChapterButtonClicked(position);
            }
        });

        if (position == currentChapterIndex) {
            int playingBackGroundColor = ThemeUtils.getColorFromAttr(getContext(), R.attr.currently_playing_background);
            holder.view.setBackgroundColor(playingBackGroundColor);
        } else {
            holder.view.setBackgroundColor(ContextCompat.getColor(getContext(), android.R.color.transparent));
        }

        if (hasImages) {
            holder.image.setVisibility(View.VISIBLE);
            if (TextUtils.isEmpty(sc.getImageUrl())) {
                Glide.with(getContext()).clear(holder.image);
            } else {
                Glide.with(getContext())
                        .load(EmbeddedChapterImage.getModelFor(media, position))
                        .apply(new RequestOptions()
                                .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                                .dontAnimate()
                                .transforms(new FitCenter(), new RoundedCorners((int)
                                        (4 * getContext().getResources().getDisplayMetrics().density))))
                        .into(holder.image);
            }
        } else {
            holder.image.setVisibility(View.GONE);
        }

        return convertView;
    }

    static class Holder {
        View view;
        TextView title;
        TextView start;
        TextView link;
        TextView duration;
        ImageView image;
        View secondaryActionButton;
        ImageView secondaryActionIcon;
    }

    @Override
    public int getCount() {
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

    public void notifyChapterChanged(int newChapterIndex) {
        currentChapterIndex = newChapterIndex;
        notifyDataSetChanged();
    }

    private boolean ignoreChapter(Chapter c) {
        return media.getDuration() > 0 && media.getDuration() < c.getStart();
    }

    @Override
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
        return super.getItem(position);
    }

    public interface Callback {
        void onPlayChapterButtonClicked(int position);
    }

}
