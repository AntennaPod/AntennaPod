package de.danoeh.antennapod.ui.preferences.screen.about;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import de.danoeh.antennapod.ui.common.ImagePlaceholder;
import de.danoeh.antennapod.ui.preferences.R;

import java.util.List;

/**
 * Displays a list of items that have a subtitle and an icon.
 */
public class SimpleIconListAdapter<T extends SimpleIconListAdapter.ListItem> extends ArrayAdapter<T> {
    private final Context context;
    private final List<T> listItems;

    public SimpleIconListAdapter(Context context, List<T> listItems) {
        super(context, R.layout.simple_icon_list_item, listItems);
        this.context = context;
        this.listItems = listItems;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null) {
            view = View.inflate(context, R.layout.simple_icon_list_item, null);
        }

        ListItem item = listItems.get(position);
        ((TextView) view.findViewById(R.id.title)).setText(item.title);
        ((TextView) view.findViewById(R.id.subtitle)).setText(item.subtitle);

        if (item.imageUrl == null) {
            view.findViewById(R.id.icon).setVisibility(View.GONE);
        } else {
            float radius = 4 * context.getResources().getDisplayMetrics().density;
            Glide.with(context)
                    .load(item.imageUrl)
                    .apply(new RequestOptions()
                            .placeholder(ImagePlaceholder.getDrawable(context, radius))
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .transform(new FitCenter(), new RoundedCorners((int) radius))
                            .dontAnimate())
                    .into(((ImageView) view.findViewById(R.id.icon)));
        }
        return view;
    }

    public static class ListItem {
        public final String title;
        public final String subtitle;
        public final String imageUrl;

        public ListItem(String title, String subtitle, String imageUrl) {
            this.title = title;
            this.subtitle = subtitle;
            this.imageUrl = imageUrl;
        }
    }
}
