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
     *  Set the selected state of item at given position
     * @param pos the position to select
     * @param selected true for selected state and false for unselected
     */
    public void setSelected(int pos, boolean selected) {
        if (selectedItemPositions.get(pos, false)) {
            if(!selected) {
                selectedItemPositions.put(pos, false);
                onSelectChanged(pos, false);
                selectedCount--;
            }
        } else {
            if(selected) {
                selectedItemPositions.put(pos, true);
                onSelectChanged(pos, true);
                selectedCount++;
            }
        }
        if(actionMode != null)
            actionMode.setTitle(getTitle());
        notifyItemChanged(pos);
    }

    /**
     *  Set the selected state of item for a given range
     * @param startPos start position of range, inclusive
     * @param endPos end position of range, inclusive
     * @param selected
     * @throws IllegalArgumentException if start and end positions are not valid
     */
    public void setSelected(int startPos, int endPos, boolean selected) throws IllegalArgumentException {
        if (startPos < 0 || endPos < 0 || startPos > endPos) {
            throw new IllegalArgumentException();
        }

        for (int i = startPos; i < endPos; i++) {
            setSelected(i, selected);
        }
    }

    public boolean isSelected(int pos) {
        if (selectedItemPositions.get(pos, false)) {
            return true;
        } else {
           return false;
        }
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
