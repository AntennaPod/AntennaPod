package de.danoeh.antennapod.view.viewholder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import de.danoeh.antennapod.R;

public class DownloadHeadingViewHolder extends RecyclerView.ViewHolder {
    public final TextView heading;
    public final TextView noItems;

    public DownloadHeadingViewHolder(Context context, ViewGroup parent) {
        super(LayoutInflater.from(context).inflate(R.layout.downloadlog_header, parent, false));
        heading = itemView.findViewById(R.id.headingLabel);
        noItems = itemView.findViewById(R.id.noItemsLabel);
    }
}
