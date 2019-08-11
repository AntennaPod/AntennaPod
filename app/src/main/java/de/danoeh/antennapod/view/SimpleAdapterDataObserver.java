package de.danoeh.antennapod.view;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;

/**
 * AdapterDataObserver that relays all events to the method anythingChanged().
 */
public abstract class SimpleAdapterDataObserver extends RecyclerView.AdapterDataObserver {
    public abstract void anythingChanged();

    @Override
    public void onChanged() {
        anythingChanged();
    }

    @Override
    public void onItemRangeChanged(int positionStart, int itemCount) {
        anythingChanged();
    }

    @Override
    public void onItemRangeChanged(int positionStart, int itemCount, @Nullable Object payload) {
        anythingChanged();
    }

    @Override
    public void onItemRangeInserted(int positionStart, int itemCount) {
        anythingChanged();
    }

    @Override
    public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
        anythingChanged();
    }

    @Override
    public void onItemRangeRemoved(int positionStart, int itemCount) {
        anythingChanged();
    }
}
