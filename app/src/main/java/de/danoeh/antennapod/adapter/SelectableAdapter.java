package de.danoeh.antennapod.adapter;

import android.app.Activity;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.recyclerview.widget.RecyclerView;

import de.danoeh.antennapod.R;

import java.util.HashSet;

/**
 * Used by Recyclerviews that need to provide ability to select items.
 */
abstract class SelectableAdapter<T extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<T> {
    private ActionMode actionMode;
    private final HashSet<Long> selectedIds = new HashSet<>();
    private final Activity activity;
    private OnSelectModeListener onSelectModeListener;

    public SelectableAdapter(Activity activity) {
        this.activity = activity;
    }

    public void startSelectMode(int pos) {
        if (inActionMode()) {
            endSelectMode();
        }

        if (onSelectModeListener != null) {
            onSelectModeListener.onStartSelectMode();
        }

        selectedIds.clear();
        selectedIds.add(getItemId(pos));
        notifyDataSetChanged();

        actionMode = activity.startActionMode(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.multi_select_options, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                updateTitle();
                toggleSelectAllIcon(menu.findItem(R.id.select_toggle), false);
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                if (item.getItemId() == R.id.select_toggle) {
                    boolean allSelected = selectedIds.size() == getItemCount();
                    setSelected(0, getItemCount(), !allSelected);
                    toggleSelectAllIcon(item, !allSelected);
                    updateTitle();
                    return true;
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                callOnEndSelectMode();
                actionMode = null;
                selectedIds.clear();
                notifyDataSetChanged();
            }
        });
        updateTitle();
    }

    /**
     * End action mode if currently in select mode, otherwise do nothing
     */
    public void endSelectMode() {
        if (inActionMode()) {
            callOnEndSelectMode();
            actionMode.finish();
        }
    }

    public boolean isSelected(int pos) {
        return selectedIds.contains(getItemId(pos));
    }

    /**
     * Set the selected state of item at given position
     *
     * @param pos      the position to select
     * @param selected true for selected state and false for unselected
     */
    public void setSelected(int pos, boolean selected) {
        if (selected) {
            selectedIds.add(getItemId(pos));
        } else {
            selectedIds.remove(getItemId(pos));
        }
        updateTitle();
    }

    /**
     * Set the selected state of item for a given range
     *
     * @param startPos start position of range, inclusive
     * @param endPos   end position of range, inclusive
     * @param selected indicates the selection state
     * @throws IllegalArgumentException if start and end positions are not valid
     */
    public void setSelected(int startPos, int endPos, boolean selected) throws IllegalArgumentException {
        for (int i = startPos; i < endPos && i < getItemCount(); i++) {
            setSelected(i, selected);
        }
        notifyItemRangeChanged(startPos, (endPos - startPos));
    }

    protected void toggleSelection(int pos) {
        setSelected(pos, !isSelected(pos));
        notifyItemChanged(pos);

        if (selectedIds.size() == 0) {
            endSelectMode();
        }
    }

    public boolean inActionMode() {
        return actionMode != null;
    }

    public int getSelectedCount() {
        return selectedIds.size();
    }

    private void toggleSelectAllIcon(MenuItem selectAllItem, boolean allSelected) {
        if (allSelected) {
            selectAllItem.setIcon(R.drawable.ic_select_none);
            selectAllItem.setTitle(R.string.deselect_all_label);
        } else {
            selectAllItem.setIcon(R.drawable.ic_select_all);
            selectAllItem.setTitle(R.string.select_all_label);
        }
    }

    private void updateTitle() {
        if (actionMode == null) {
            return;
        }
        actionMode.setTitle(activity.getResources()
                .getQuantityString(R.plurals.num_selected_label, selectedIds.size(),
                selectedIds.size(), getItemCount()));
    }

    public void setOnSelectModeListener(OnSelectModeListener onSelectModeListener) {
        this.onSelectModeListener = onSelectModeListener;
    }

    private void callOnEndSelectMode() {
        if (onSelectModeListener != null) {
            onSelectModeListener.onEndSelectMode();
        }
    }

    public interface OnSelectModeListener {
        void onStartSelectMode();

        void onEndSelectMode();
    }
}
