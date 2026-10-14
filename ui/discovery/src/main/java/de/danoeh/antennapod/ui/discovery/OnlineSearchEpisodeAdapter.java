package de.danoeh.antennapod.ui.discovery;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.button.MaterialButton;
import de.danoeh.antennapod.net.discovery.EpisodeSearchResult;
import de.danoeh.antennapod.ui.common.Converter;
import de.danoeh.antennapod.ui.common.DateFormatter;
import de.danoeh.antennapod.ui.common.ImagePlaceholder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OnlineSearchEpisodeAdapter extends RecyclerView.Adapter<OnlineSearchEpisodeAdapter.ViewHolder> {
    private final Context context;
    private final List<EpisodeSearchResult> data = new ArrayList<>();
    private final Set<String> subscribedFeedUrls = new HashSet<>();
    private final OnEpisodeActionListener listener;

    public interface OnEpisodeActionListener {
        void onEpisodeClick(EpisodeSearchResult episode);

        void onPlayClick(EpisodeSearchResult episode);

        void onSubscribeClick(EpisodeSearchResult episode);
    }

    public OnlineSearchEpisodeAdapter(Context context, OnEpisodeActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateData(List<EpisodeSearchResult> newData) {
        data.clear();
        if (newData != null) {
            data.addAll(newData);
        }
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setSubscribedFeedUrls(Set<String> urls) {
        subscribedFeedUrls.clear();
        if (urls != null) {
            subscribedFeedUrls.addAll(urls);
        }
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addSubscribedFeedUrl(String url) {
        if (url != null) {
            subscribedFeedUrls.add(url);
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.online_search_episode_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EpisodeSearchResult episode = data.get(position);
        holder.txtvTitle.setText(episode.title);

        List<String> metaParts = new ArrayList<>();
        if (!TextUtils.isEmpty(episode.podcastTitle)) {
            metaParts.add(episode.podcastTitle);
        }
        if (episode.pubDate != null) {
            metaParts.add(DateFormatter.formatAbbrev(context, episode.pubDate));
        }
        if (episode.durationMs > 0) {
            metaParts.add(Converter.getDurationStringLong((int) (episode.durationMs / 1000)));
        }
        holder.txtvPodcastAndDate.setText(TextUtils.join(" • ", metaParts));

        if (!TextUtils.isEmpty(episode.description)) {
            holder.txtvDescription.setText(episode.description);
            holder.txtvDescription.setVisibility(View.VISIBLE);
        } else {
            holder.txtvDescription.setVisibility(View.GONE);
        }

        float radius = 8 * context.getResources().getDisplayMetrics().density;
        Glide.with(context)
                .load(episode.imageUrl)
                .apply(new RequestOptions()
                        .placeholder(ImagePlaceholder.getDrawable(context, radius))
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .transform(new FitCenter(), new RoundedCorners((int) radius))
                        .dontAnimate())
                .into(holder.imgvCover);

        boolean isSubscribed = episode.feedUrl != null && subscribedFeedUrls.contains(episode.feedUrl);
        if (isSubscribed) {
            holder.btnSubscribe.setText(R.string.subscribed_label);
            holder.btnSubscribe.setIconResource(R.drawable.ic_check);
            holder.btnSubscribe.setEnabled(false);
        } else {
            holder.btnSubscribe.setText(R.string.subscribe_label);
            holder.btnSubscribe.setIconResource(R.drawable.ic_add);
            holder.btnSubscribe.setEnabled(true);
            holder.btnSubscribe.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSubscribeClick(episode);
                }
            });
        }

        holder.btnPlay.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlayClick(episode);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEpisodeClick(episode);
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgvCover;
        final TextView txtvTitle;
        final TextView txtvPodcastAndDate;
        final TextView txtvDescription;
        final MaterialButton btnSubscribe;
        final MaterialButton btnPlay;

        ViewHolder(View itemView) {
            super(itemView);
            imgvCover = itemView.findViewById(R.id.imgvCover);
            txtvTitle = itemView.findViewById(R.id.txtvTitle);
            txtvPodcastAndDate = itemView.findViewById(R.id.txtvPodcastAndDate);
            txtvDescription = itemView.findViewById(R.id.txtvDescription);
            btnSubscribe = itemView.findViewById(R.id.btnSubscribe);
            btnPlay = itemView.findViewById(R.id.btnPlay);
        }
    }
}
