package de.danoeh.antennapod.adapter;

import android.app.Activity;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.FeedItem;

/**
 * Used by Recyclerviews that need to provide ability to select items
 * @param <T>
 */
class SelectableAdapter<T extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<T>{
    private int selectedCount = 1;

    public SelectableAdapter(Activity activity) {
        this.activity = activity;
    }
    @NonNull
    @Override
    public T onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull T holder, int position) {
        if (inActionMode() && isSelected(position)) {
            holder.itemView.setBackgroundColor(selectedItemBgColor);
        } else {
            holder.itemView.setBackgroundColor(unSelectedItemBgColor);
        }
    }

    @Override
    public int getItemCount() {
        return 0;
    }

    private ActionMode actionMode;
    private SparseBooleanArray selectedItemPositions = new SparseBooleanArray();
    private Activity activity;
    protected Integer selectedItemBgColor = Color.LTGRAY;
    protected Integer unSelectedItemBgColor = Color.WHITE;

    public void startActionMode(int pos) {
        if (inActionMode()) {
            finish();
        }
        onStartActionMode();
        AttributeSet attrs = null;
        int[] attrsArray = new int[] {
                android.R.attr.windowBackground,
                R.attr.colorAccent
        };
        TypedArray ta = activity.obtainStyledAttributes(null, attrsArray);
        Resources res =  activity.getResources();
        unSelectedItemBgColor = res.getColor(ta.getResourceId(0, Color.LTGRAY));
        selectedItemBgColor = res.getColor(ta.getResourceId(1, Color.WHITE));
        onStartActionMode();
        selectedItemPositions.append(pos, true);
        notifyItemChanged(pos);

        actionMode = activity.startActionMode(new ActionMode.Callback() {
            private MenuItem selectAllItem = null;
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.multi_select_options, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                mode.setTitle(getTitle());
                selectAllItem = menu.findItem(R.id.select_toggle);
                selectAllItem.setIcon(R.drawable.ic_select_all);
                toggleSelectAllIcon(selectAllItem,false);
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                if (item.getItemId() == R.id.select_toggle) {
                    if (selectedCount == getItemCount()) {
                        selectNone();
                        toggleSelectAllIcon(item, false);
                    } else {
                        selectAll();
                        toggleSelectAllIcon(item, true);
                    }
                    mode.setTitle(getTitle());
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                onEndActionMode();
                actionMode = null;
                selectedItemPositions.clear();
                selectedCount = 0;
                notifyDataSetChanged();

            }

        });

        actionMode.setTitle(getTitle());
    }


    /**
     * End action mode if currently action mode, otherwise do nothing
     */
    public void finish() {
        if (inActionMode()) {
            actionMode.finish();
            onEndActionMode();
        }
    }
    public void selectHandler(int pos) {
        if (selectedItemPositions.get(pos, false)) {
            selectedItemPositions.put(pos, false);
            onSelectChanged(pos, false);
            selectedCount--;

        } else {
            selectedItemPositions.put(pos, true);
            onSelectChanged(pos, true);
            selectedCount++;

        }
        actionMode.setTitle(getTitle());
        notifyItemChanged(pos);
    }

    public void onStartActionMode() {

    }

    public void onEndActionMode() {

    }

    public boolean inActionMode() {
        return actionMode != null;
    }

    protected boolean isSelected(int pos) {
        return selectedItemPositions.get(pos, false);
    }

    public void onSelectChanged(int pos, boolean selected) {

    }
    private void toggleSelectAll(MenuItem selectAllItem, boolean toggle) {
        if (toggle) {
            selectAll();
            toggleSelectAllIcon(selectAllItem, toggle);
        } else {
            selectNone();
            toggleSelectAllIcon(selectAllItem, toggle);

        }
    }

    private void toggleSelectAllIcon(MenuItem selectAllItem, boolean toggle) {
        if (toggle) {
            selectAllItem.setIcon(R.drawable.ic_select_none);
            selectAllItem.setTitle(R.string.deselect_all_label);
        } else {
            selectAllItem.setIcon(R.drawable.ic_select_all);
            selectAllItem.setTitle(R.string.select_all_label);

        }
    }


    private String getTitle() {
        return selectedCount + " / " + getItemCount() + " selected";
    }


    private void selectAll() {
        for(int i = 0; i < getItemCount(); ++i) {
            selectedItemPositions.put(i, true);
            ++selectedCount;
            onSelectChanged(i, true);
        }
        notifyDataSetChanged();
    }

    private void selectNone() {
        selectedItemPositions.clear();
        for(int i = 0; i < getItemCount(); ++i) {
            onSelectChanged(i, false);
        }
        selectedCount = 0;
        notifyDataSetChanged();
    }

}
