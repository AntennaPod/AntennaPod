package de.danoeh.antennapod.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MediaplayerInfoActivity.MediaplayerInfoContentFragment;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.util.playback.Playable;

/**
 * Displays the cover and the title of a FeedItem.
 */
public class CoverFragment extends Fragment implements MediaplayerInfoContentFragment {

    private static final String TAG = "CoverFragment";
    private static final String ARG_PLAYABLE = "arg.playable";

    private Playable media;

    private View root;
    private TextView txtvPodcastTitle;
    private TextView txtvEpisodeTitle;
    private ImageView imgvCover;

    public static CoverFragment newInstance(Playable item) {
        CoverFragment f = new CoverFragment();
        f.media = item;
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (media == null) {
            Log.e(TAG, TAG + " was called without media");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.cover_fragment, container, false);
        txtvPodcastTitle = (TextView) root.findViewById(R.id.txtvPodcastTitle);
        txtvEpisodeTitle = (TextView) root.findViewById(R.id.txtvEpisodeTitle);
        imgvCover = (ImageView) root.findViewById(R.id.imgvCover);
        return root;
    }

    private void loadMediaInfo() {
        if (media != null) {
            txtvPodcastTitle.setText(media.getFeedTitle());
            txtvEpisodeTitle.setText(media.getEpisodeTitle());
            Glide.with(this)
                    .load(media.getImageLocation())
                    .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                    .dontAnimate()
                    .fitCenter()
                    .into(imgvCover);
        } else {
            Log.w(TAG, "loadMediaInfo was called while media was null");
        }
    }

    @Override
    public void onStart() {
        Log.d(TAG, "On Start");
        super.onStart();
        if (media != null) {
            Log.d(TAG, "Loading media info");
            loadMediaInfo();
        } else {
            Log.w(TAG, "Unable to load media info: media was null");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // prevent memory leaks
        root = null;
    }

    @Override
    public void onMediaChanged(Playable media) {
        if(this.media == media) {
            return;
        }
        this.media = media;
        if (isAdded()) {
            loadMediaInfo();
        }
    }

}
