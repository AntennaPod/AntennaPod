package de.danoeh.antennapodSA.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import de.danoeh.antennapodSA.R;

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

    public void attachToListView(AbsListView listView) {
        if (layoutAdded) {
            throw new IllegalStateException("Can not attach EmptyView multiple times");
        }
        addToParentView(listView);
        layoutAdded = true;
        listView.setEmptyView(emptyView);
    }

    public void attachToRecyclerView(RecyclerView recyclerView) {
        if (layoutAdded) {
            throw new IllegalStateException("Can not attach EmptyView multiple times");
        }
        addToParentView(recyclerView);
        layoutAdded = true;
        this.recyclerView = recyclerView;
        updateAdapter(recyclerView.getAdapter());
    }

    private void addToParentView(View view) {
        ViewGroup parent = ((ViewGroup) view.getParent());
        parent.addView(emptyView);
        if (parent instanceof RelativeLayout) {
            RelativeLayout.LayoutParams layoutParams =
                    (RelativeLayout.LayoutParams) emptyView.getLayoutParams();
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

    public void updateVisibility() {
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
