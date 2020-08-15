package de.danoeh.antennapod.view;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Source: https://stackoverflow.com/a/30794046
 */
public class ItemOffsetDecoration extends RecyclerView.ItemDecoration {
    private final int itemOffset;

    public ItemOffsetDecoration(@NonNull Context context, int itemOffsetDp) {
        itemOffset = (int) (itemOffsetDp * context.getResources().getDisplayMetrics().density);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        outRect.set(itemOffset, itemOffset, itemOffset, itemOffset);
    }
}
