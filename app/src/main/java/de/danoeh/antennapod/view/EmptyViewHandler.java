package de.danoeh.antennapod.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.AttrRes;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import de.danoeh.antennapod.R;

public class EmptyViewHandler {
    private boolean layoutAdded = false;
    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;

    private final Context context;
    private final View emptyView;
    private final TextView tvTitle;
    private final TextView tvMessage;
    private final ImageView ivIcon;

    public EmptyViewHandler(Context context) {
        emptyView = View.inflate(context, R.layout.empty_view_layout, null);
        this.context = context;
        tvTitle = emptyView.findViewById(R.id.emptyViewTitle);
        tvMessage = emptyView.findViewById(R.id.emptyViewMessage);
        ivIcon = emptyView.findViewById(R.id.emptyViewIcon);
    }

    public void setTitle(int title) {
        tvTitle.setText(title);
    }

    public void setMessage(int message) {
        tvMessage.setText(message);
    }

    public void setIcon(@AttrRes int iconAttr) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(iconAttr, typedValue, true);
        Drawable d = ContextCompat.getDrawable(context, typedValue.resourceId);
        ivIcon.setImageDrawable(d);
        ivIcon.setVisibility(View.VISIBLE);
    }

    public void hide() {
        emptyView.setVisibility(View.GONE);
    }

    public void attachToListView(ListView listView) {
        if (layoutAdded) {
            throw new IllegalStateException("Can not attach to ListView multiple times");
        }
        layoutAdded = true;
        ((ViewGroup) listView.getParent()).addView(emptyView);
        listView.setEmptyView(emptyView);
    }

    public void attachToRecyclerView(RecyclerView recyclerView) {
        if (layoutAdded) {
            throw new IllegalStateException("Can not attach to ListView multiple times");
        }
        layoutAdded = true;
        this.recyclerView = recyclerView;
        ViewGroup parent = ((ViewGroup) recyclerView.getParent());
        parent.addView(emptyView);
        updateAdapter(recyclerView.getAdapter());

        if (parent instanceof RelativeLayout) {
            RelativeLayout.LayoutParams layoutParams =
                    (RelativeLayout.LayoutParams)emptyView.getLayoutParams();
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
            emptyView.setLayoutParams(layoutParams);
        }
    }

    public void updateAdapter(RecyclerView.Adapter adapter) {
        if (this.adapter != null) {
            this.adapter.unregisterAdapterDataObserver(adapterObserver);
        }
        this.adapter = adapter;
        if (adapter != null) {
            adapter.registerAdapterDataObserver(adapterObserver);
        }
        updateVisibility();
    }

    private final SimpleAdapterDataObserver adapterObserver = new SimpleAdapterDataObserver() {
        @Override
        public void anythingChanged() {
            updateVisibility();
        }
    };

    private void updateVisibility() {
        boolean empty;
        if (adapter == null) {
            empty = true;
        } else {
            empty = adapter.getItemCount() == 0;
        }
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
    }
}
