package de.danoeh.antennapod.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import de.danoeh.antennapod.R;

/**
 * Shows all items in the queue.
 */
public class QueueFragmentInHome extends QueueFragment {
    public static final String TAG = "QueueInHome";

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        root.findViewById(R.id.toolbar).setVisibility(View.GONE);
        root.findViewById(R.id.swipeRefresh).setEnabled(false);
        return root;
    }
}
