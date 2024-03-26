package de.danoeh.antennapod.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

public class InboxFragmentInHome extends InboxFragment {
    public static final String TAG = "InboxInHome";

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        toolbar.setVisibility(View.GONE);
        swipeRefreshLayout.setEnabled(false);
        return root;
    }
}
