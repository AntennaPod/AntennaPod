package de.danoeh.antennapod.core.util.gui;

import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import de.danoeh.antennapod.core.R;

/**
 * Utility methods for the more_content_list_footer layout.
 */
public class MoreContentListFooterUtil {

    private final View root;

    private boolean loading;

    private Listener listener;

    public MoreContentListFooterUtil(View root) {
        this.root = root;
        root.setOnClickListener(v -> {
            if (listener != null && !loading) {
                listener.onClick();
            }
        });
    }

    public void setLoadingState(boolean newState) {
        final ImageView imageView = root.findViewById(R.id.imgExpand);
        final ProgressBar progressBar = root.findViewById(R.id.progBar);
        if (newState) {
            imageView.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            imageView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }
        loading = newState;
    }

    public void setClickListener(Listener l) {
        listener = l;
    }

    public interface Listener {
        void onClick();
    }

    public View getRoot() {
        return root;
    }
}
