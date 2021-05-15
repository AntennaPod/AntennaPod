package de.danoeh.antennapod.adapter;

import android.app.Activity;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import de.danoeh.antennapod.R;

/**
 * Used by Recyclerviews that need to provide ability to select items
 * @param <T>
 */
class SelectableAdapter<T extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<T>{
    private int selectedCount;

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
    }

    @Override
    public int getItemCount() {
        return 0;
    }

    private ActionMode actionMode;
    private SparseBooleanArray selectedItemPositions = new SparseBooleanArray();
    private Activity activity;

    public void startActionMode(int pos) {
        if (inActionMode()) {
            finish();
        }

        selectedCount = 1;
        selectedItemPositions.clear();

        AttributeSet attrs = null;
        int[] attrsArray = new int[] {
                android.R.attr.windowBackground,
                R.attr.colorAccent
        };

        selectedItemPositions.append(pos, true);
        onSelectChanged(pos, true);
        notifyItemChanged(pos);
        onStartActionMode();

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
     * End action mode if currently in select mode, otherwise do nothing
     */
    public void finish() {
        if (inActionMode()) {
            onEndActionMode();
            actionMode.finish();
        }
    }

    /**
     * Called by subclasses that need to notify this class of a new selection event
     * at the given position and to handle
     * @param pos
     */
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
        if(actionMode != null)
            actionMode.setTitle(getTitle());
        notifyItemChanged(pos);

    }

    public void onStartActionMode() {

    }

    protected void onEndActionMode() {

    }

    public boolean inActionMode() {
        return actionMode != null;
    }

    public int getSelectedCount() {
        return selectedCount;
    }

    /**
     * Allows subclasses to be notified when a selection change has occurred
     * @param pos the position that was changed
     * @param selected the new state of the selection, true if selected, otherwise false
     */
    public void onSelectChanged(int pos, boolean selected) {

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


    /**
     * Selects all items and notifies subclasses of selected items
     */
    private void selectAll() {
        for(int i = 0; i < getItemCount(); ++i) {
            boolean isSelected = selectedItemPositions.get(i);
            if (!isSelected) {
                selectedItemPositions.put(i, true);
                onSelectChanged(i, true);
            }
        }
        selectedCount = getItemCount();
        notifyDataSetChanged();
    }
    /**
     * Un-selects all items and notifies subclasses of un-selected items
     */
    private void selectNone() {
        for(int i = 0; i < getItemCount(); ++i) {
            boolean isSelected = selectedItemPositions.get(i);
            if (isSelected) {
                onSelectChanged(i, false);
            }
        }
        selectedItemPositions.clear();
        selectedCount = 0;
        notifyDataSetChanged();
    }

}
