package de.danoeh.antennapod.view.viewholder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.ui.common.CircularProgressBar;

public class RunningDownloadViewHolder extends RecyclerView.ViewHolder {
    public final TextView title;
    public final TextView status;
    public final View secondaryActionButton;
    public final ImageView secondaryActionIcon;
    public final CircularProgressBar secondaryActionProgress;


    public RunningDownloadViewHolder(Context context, ViewGroup parent) {
        super(LayoutInflater.from(context).inflate(R.layout.downloadlist_item, parent, false));
        title = itemView.findViewById(R.id.txtvTitle);
        status = itemView.findViewById(R.id.txtvStatus);
        secondaryActionButton = itemView.findViewById(R.id.secondaryActionButton);
        secondaryActionIcon = itemView.findViewById(R.id.secondaryActionIcon);
        secondaryActionProgress = itemView.findViewById(R.id.secondaryActionProgress);
    }
}
