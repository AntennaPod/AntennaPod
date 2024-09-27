package de.danoeh.antennapod.ui.screen.drawer;

import android.content.Context;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import de.danoeh.antennapod.R;

import java.util.List;

public class BottomNavigationMoreAdapter extends ArrayAdapter<MenuItem> {
    private final Context context;
    private final List<MenuItem> listItems;

    public BottomNavigationMoreAdapter(Context context, List<MenuItem> listItems) {
        super(context, R.layout.bottom_navigation_more_listitem, listItems);
        this.context = context;
        this.listItems = listItems;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null) {
            view = View.inflate(context, R.layout.bottom_navigation_more_listitem, null);
        }

        MenuItem item = listItems.get(position);
        ((ImageView) view.findViewById(R.id.coverImage)).setImageDrawable(item.getIcon());
        ((TextView) view.findViewById(R.id.titleLabel)).setText(item.getTitle());
        return view;
    }
}
