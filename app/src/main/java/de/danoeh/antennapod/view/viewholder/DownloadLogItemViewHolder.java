package de.danoeh.antennapod.view.viewholder;

import android.content.Context;
import android.os.Build;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.joanzapata.iconify.widget.IconTextView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.ui.common.CircularProgressBar;

public class DownloadLogItemViewHolder extends RecyclerView.ViewHolder {
    public final View secondaryActionButton;
    public final ImageView secondaryActionIcon;
    public final CircularProgressBar secondaryActionProgress;
    public final IconTextView icon;
    public final TextView title;
    public final TextView status;
    public final TextView reason;
    public final TextView tapForDetails;

    public DownloadLogItemViewHolder(Context context, ViewGroup parent) {
        super(LayoutInflater.from(context).inflate(R.layout.downloadlog_item, parent, false));
        status = itemView.findViewById(R.id.status);
        icon = itemView.findViewById(R.id.txtvIcon);
        reason = itemView.findViewById(R.id.txtvReason);
        tapForDetails = itemView.findViewById(R.id.txtvTapForDetails);
        secondaryActionButton = itemView.findViewById(R.id.secondaryActionButton);
        secondaryActionProgress = itemView.findViewById(R.id.secondaryActionProgress);
        secondaryActionIcon = itemView.findViewById(R.id.secondaryActionIcon);
        title = itemView.findViewById(R.id.txtvTitle);
        if (Build.VERSION.SDK_INT >= 23) {
            title.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_FULL);
        }
        itemView.setTag(this);
    }
}
