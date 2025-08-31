package de.danoeh.antennapod.ui.view;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ItemOffsetDecoration extends RecyclerView.ItemDecoration {
    private final int itemOffsetHorizontal;
    private final int itemOffsetVertical;

    public ItemOffsetDecoration(@NonNull Context context, int itemOffsetDp) {
        itemOffsetHorizontal = (int) (itemOffsetDp * context.getResources().getDisplayMetrics().density);
        itemOffsetVertical = (int) (itemOffsetDp * context.getResources().getDisplayMetrics().density);
    }

    public ItemOffsetDecoration(@NonNull Context context, int itemOffsetHorizontalDp, int itemOffsetVerticalDp) {
        itemOffsetHorizontal = (int) (itemOffsetHorizontalDp * context.getResources().getDisplayMetrics().density);
        itemOffsetVertical = (int) (itemOffsetVerticalDp * context.getResources().getDisplayMetrics().density);
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        outRect.set(itemOffsetHorizontal, itemOffsetVertical, itemOffsetHorizontal, itemOffsetVertical);
    }
}
