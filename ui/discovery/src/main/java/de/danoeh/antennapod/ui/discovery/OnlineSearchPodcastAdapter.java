package de.danoeh.antennapod.ui.discovery;

import android.annotation.SuppressLint;
import android.content.Context;
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
import de.danoeh.antennapod.net.discovery.PodcastSearchResult;
import de.danoeh.antennapod.ui.common.ImagePlaceholder;
import java.util.ArrayList;
import java.util.List;

public class OnlineSearchPodcastAdapter extends RecyclerView.Adapter<OnlineSearchPodcastAdapter.ViewHolder> {
    private final Context context;
    private final List<PodcastSearchResult> data = new ArrayList<>();
    private final OnPodcastClickListener listener;

    public interface OnPodcastClickListener {
        void onPodcastClick(PodcastSearchResult podcast);
    }

    public OnlineSearchPodcastAdapter(Context context, OnPodcastClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateData(List<PodcastSearchResult> newData) {
        data.clear();
        if (newData != null) {
            data.addAll(newData);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.online_search_listitem, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PodcastSearchResult podcast = data.get(position);
        holder.txtvTitle.setText(podcast.title);
        if (podcast.author != null && !podcast.author.trim().isEmpty()) {
            holder.txtvAuthor.setText(podcast.author);
            holder.txtvAuthor.setVisibility(View.VISIBLE);
        } else {
            holder.txtvAuthor.setVisibility(View.GONE);
        }

        float radius = 8 * context.getResources().getDisplayMetrics().density;
        Glide.with(context)
                .load(podcast.imageUrl)
                .apply(new RequestOptions()
                        .placeholder(ImagePlaceholder.getDrawable(context, radius))
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .transform(new FitCenter(), new RoundedCorners((int) radius))
                        .dontAnimate())
                .into(holder.imgvCover);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPodcastClick(podcast);
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
        final TextView txtvAuthor;

        ViewHolder(View itemView) {
            super(itemView);
            imgvCover = itemView.findViewById(R.id.imgvCover);
            txtvTitle = itemView.findViewById(R.id.txtvTitle);
            txtvAuthor = itemView.findViewById(R.id.txtvAuthor);
        }
    }
}
