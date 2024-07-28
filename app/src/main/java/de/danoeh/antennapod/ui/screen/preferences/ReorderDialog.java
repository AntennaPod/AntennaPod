package de.danoeh.antennapod.ui.screen.preferences;

import android.content.Context;
import android.view.LayoutInflater;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.ReorderDialogBinding;

import java.util.ArrayList;
import java.util.List;

public abstract class ReorderDialog {
    protected final Context context;
    protected final List<ReorderDialogItem> dialogItems;
    private final ReorderDialogAdapter adapter;

    public ReorderDialog(Context context) {
        this.context = context;
        dialogItems = getInitialItems();
        adapter = new ReorderDialogAdapter(dialogItems);
    }

    public void show() {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        ReorderDialogBinding viewBinding = ReorderDialogBinding.inflate(layoutInflater);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(getTitle());
        builder.setView(viewBinding.getRoot());
        RecyclerView recyclerView = viewBinding.recyclerView;

        configureRecyclerView(recyclerView, adapter, context);

        builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> onConfirmed());
        builder.setNegativeButton(R.string.cancel_label, null);
        builder.setNeutralButton(R.string.reset, (dialog, which) -> onReset());
        builder.show();
    }

    private static void configureRecyclerView(RecyclerView recyclerView,
                                              ReorderDialogAdapter adapter, Context context) {
        ReorderItemTouchCallback itemMoveCallback = new ReorderItemTouchCallback() {
            @Override
            protected boolean onItemMove(int fromPosition, int toPosition) {
                return adapter.onItemMove(fromPosition, toPosition);
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(itemMoveCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
        adapter.setDragListener(itemTouchHelper::startDrag);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(adapter);
    }

    @NonNull
    public List<String> getTagsWithoutHeaders() {
        List<String> orderedSectionTags = new ArrayList<>();
        for (ReorderDialogItem item : dialogItems) {
            if (item.getViewType() == ReorderDialogItem.ViewType.Header) {
                continue;
            }
            orderedSectionTags.add(item.getTag());
        }
        return orderedSectionTags;
    }

    @NonNull
    public List<String> getTagsAfterHeader(String tag) {
        List<String> itemsAfterHeader = new ArrayList<>();
        int i = 0;
        while (dialogItems.get(i).getViewType() != ReorderDialogItem.ViewType.Header
                || !dialogItems.get(i).getTag().equals(tag)) {
            i++;
        }
        i++;
        while (i < dialogItems.size() && dialogItems.get(i).getViewType() != ReorderDialogItem.ViewType.Header) {
            itemsAfterHeader.add(dialogItems.get(i).getTag());
            i++;
        }
        return itemsAfterHeader;
    }

    protected abstract @StringRes int getTitle();

    @NonNull
    protected abstract List<ReorderDialogItem> getInitialItems();

    protected abstract void onReset();

    protected abstract void onConfirmed();
}
