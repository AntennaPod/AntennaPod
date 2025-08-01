package de.danoeh.antennapod.ui.view;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.Gravity;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import androidx.annotation.DrawableRes;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import de.danoeh.antennapod.R;

public class EmptyViewHandler {
    private boolean layoutAdded = false;
    private ListAdapter listAdapter;
    private RecyclerView.Adapter<?> recyclerAdapter;
    private View listView;

    private final View emptyView;
    private final TextView tvTitle;
    private final TextView tvMessage;
    private final ImageView ivIcon;
    private final Button button;

    public EmptyViewHandler(Context context) {
        emptyView = View.inflate(context, R.layout.empty_view_layout, null);
        tvTitle = emptyView.findViewById(R.id.emptyViewTitle);
        tvMessage = emptyView.findViewById(R.id.emptyViewMessage);
        ivIcon = emptyView.findViewById(R.id.emptyViewIcon);
        button = emptyView.findViewById(R.id.button);
    }

    public void setTitle(int title) {
        tvTitle.setText(title);
    }

    public void setTitle(String title) {
        tvTitle.setText(title);
    }

    public void setMessage(int message) {
        tvMessage.setText(message);
    }

    public void setMessage(String message) {
        tvMessage.setText(message);
    }

    public void setButtonText(int message) {
        button.setText(message);
    }

    public void setButtonVisibility(int visibility) {
        button.setVisibility(visibility);
    }

    public void setButtonOnClickListener(View.OnClickListener onClickListener) {
        button.setOnClickListener(onClickListener);
    }

    public void setIcon(@DrawableRes int icon) {
        ivIcon.setImageResource(icon);
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
        this.listView = listView;
        listView.setEmptyView(emptyView);
        updateAdapter(listView.getAdapter());
    }

    public void attachToRecyclerView(RecyclerView recyclerView) {
        if (layoutAdded) {
            throw new IllegalStateException("Can not attach EmptyView multiple times");
        }
        addToParentView(recyclerView);
        layoutAdded = true;
        this.listView = recyclerView;
        updateAdapter(recyclerView.getAdapter());
    }

    private void addToParentView(View view) {
        ViewGroup parent = ((ViewGroup) view.getParent());
        while (parent != null) {
            if (parent instanceof RelativeLayout) {
                parent.addView(emptyView);
                RelativeLayout.LayoutParams layoutParams =
                        (RelativeLayout.LayoutParams) emptyView.getLayoutParams();
                layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                emptyView.setLayoutParams(layoutParams);
                break;
            } else if (parent instanceof FrameLayout) {
                parent.addView(emptyView);
                FrameLayout.LayoutParams layoutParams =
                        (FrameLayout.LayoutParams) emptyView.getLayoutParams();
                layoutParams.gravity = Gravity.CENTER;
                emptyView.setLayoutParams(layoutParams);
                break;
            } else if (parent instanceof CoordinatorLayout) {
                parent.addView(emptyView);
                CoordinatorLayout.LayoutParams layoutParams =
                        (CoordinatorLayout.LayoutParams) emptyView.getLayoutParams();
                layoutParams.gravity = Gravity.CENTER;
                emptyView.setLayoutParams(layoutParams);
                break;
            }
            parent = (ViewGroup) parent.getParent();
        }
    }

    public void updateAdapter(RecyclerView.Adapter<?> adapter) {
        if (this.recyclerAdapter != null) {
            this.recyclerAdapter.unregisterAdapterDataObserver(adapterObserver);
        }
        this.recyclerAdapter = adapter;
        if (adapter != null) {
            adapter.registerAdapterDataObserver(adapterObserver);
        }
        updateVisibility();
    }

    private void updateAdapter(ListAdapter adapter) {
        if (this.listAdapter != null) {
            this.listAdapter.unregisterDataSetObserver(listAdapterObserver);
        }
        this.listAdapter = adapter;
        if (adapter != null) {
            adapter.registerDataSetObserver(listAdapterObserver);
        }
        updateVisibility();
    }

    private final SimpleAdapterDataObserver adapterObserver = new SimpleAdapterDataObserver() {
        @Override
        public void anythingChanged() {
            updateVisibility();
        }
    };

    private final DataSetObserver listAdapterObserver = new DataSetObserver() {
        public void onChanged() {
            updateVisibility();
        }
    };

    public void updateVisibility() {
        boolean empty;
        if (recyclerAdapter != null) {
            empty = recyclerAdapter.getItemCount() == 0;
        } else if (listAdapter != null) {
            empty = listAdapter.isEmpty();
        } else {
            empty = true;
        }
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        listView.setVisibility(empty ? View.INVISIBLE : View.VISIBLE);
    }
}
