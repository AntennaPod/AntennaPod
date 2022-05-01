package de.danoeh.antennapod.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.feed.util.ImageResourceUtils;
import de.danoeh.antennapod.core.util.DateFormatter;
import de.danoeh.antennapod.core.util.FeedItemUtil;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.ui.common.SquareImageView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class HorizontalItemListAdapter extends RecyclerView.Adapter<HorizontalItemListAdapter.Holder> {
    private final WeakReference<MainActivity> mainActivityRef;
    private final List<FeedItem> data = new ArrayList<>();

    public HorizontalItemListAdapter(MainActivity mainActivity) {
        this.mainActivityRef = new WeakReference<>(mainActivity);
    }

    public void updateData(List<FeedItem> newData) {
        data.clear();
        data.addAll(newData);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View convertView = View.inflate(mainActivityRef.get(), R.layout.cover_play_title_item, null);
        return new Holder(convertView);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        final FeedItem item = data.get(position);

        new CoverLoader(mainActivityRef.get())
                .withUri(ImageResourceUtils.getEpisodeListImageLocation(item))
                .withFallbackUri(item.getFeed().getImageUrl())
                .withCoverView(holder.cover)
                .load();
        holder.title.setText(item.getTitle());
        holder.date.setText(DateFormatter.formatAbbrev(mainActivityRef.get(), item.getPubDate()));
        boolean isPlaying = item.getMedia() != null && FeedItemUtil.isCurrentlyPlaying(item.getMedia());
        holder.playButton.setImageResource(isPlaying ? R.drawable.ic_pause_circle : R.drawable.ic_play_circle);
    }

    @Override
    public long getItemId(int position) {
        return data.get(position).getId();
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        SquareImageView cover;
        TextView title;
        TextView date;
        ImageView playButton;

        public Holder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.cover_play);
            title = itemView.findViewById(R.id.playTitle);
            date = itemView.findViewById(R.id.playDate);
            playButton = itemView.findViewById(R.id.play_icon);
        }
    }
}
