package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import de.danoeh.antennapod.R;

import java.util.ArrayList;

/**
 * Displays a list of items that have a subtitle and an icon.
 */
public class SimpleIconListAdapter extends ArrayAdapter<SimpleIconListAdapter.ListItem> {
    private final Context context;
    private final ArrayList<ListItem> developers;

    public SimpleIconListAdapter(Context context, ArrayList<ListItem> developers) {
        super(context, R.layout.simple_icon_list_item, developers);
        this.context = context;
        this.developers = developers;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null) {
            view = View.inflate(context, R.layout.simple_icon_list_item, null);
        }

        ListItem item = developers.get(position);
        ((TextView) view.findViewById(R.id.title)).setText(item.title);
        ((TextView) view.findViewById(R.id.subtitle)).setText(item.subtitle);
        Glide.with(context)
                .load(item.imageUrl)
                .apply(new RequestOptions()
                        .placeholder(R.color.light_gray)
                        .error(R.color.light_gray)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .fitCenter()
                        .dontAnimate())
                .into(((ImageView) view.findViewById(R.id.icon)));
        return view;
    }

    public static class ListItem {
        final String title;
        final String subtitle;
        final String imageUrl;

        public ListItem(String title, String subtitle, String imageUrl) {
            this.title = title;
            this.subtitle = subtitle;
            this.imageUrl = imageUrl;
        }
    }
}
