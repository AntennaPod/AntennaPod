package de.danoeh.antennapod.fragment;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BitmapImageViewTarget;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.AudioplayerActivity.AudioplayerContentFragment;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.util.playback.Playable;

/**
 * Displays the cover and the title of a FeedItem.
 */
public class CoverFragment extends Fragment implements
        AudioplayerContentFragment {
    private static final String TAG = "CoverFragment";
    private static final String ARG_PLAYABLE = "arg.playable";

    private Playable media;

    private View root;
    private TextView txtvPodcastTitle;
    private TextView txtvEpisodeTitle;
    private ImageView imgvCover;

    public static CoverFragment newInstance(Playable item) {
        CoverFragment f = new CoverFragment();
        if (item != null) {
            Bundle args = new Bundle();
            args.putParcelable(ARG_PLAYABLE, item);
            f.setArguments(args);
        }
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        Bundle args = getArguments();
        if (args != null) {
            media = args.getParcelable(ARG_PLAYABLE);
        } else {
            Log.e(TAG, TAG + " was called with invalid arguments");
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
        if(imgvCover == null) {
            return;
        }
        if (media != null) {
            Log.d(TAG, "feed title: " + media.getFeedTitle());
            Log.d(TAG, "episode title: " + media.getEpisodeTitle());
            txtvPodcastTitle.setText(media.getFeedTitle());
            txtvEpisodeTitle.setText(media.getEpisodeTitle());
            imgvCover.post(() -> {
                Glide.with(this)
                        .load(media.getImageUri())
                        .asBitmap()
                        .placeholder(R.color.light_gray)
                        .error(R.color.light_gray)
                        .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                        .dontAnimate()
                        .into(new BitmapImageViewTarget(imgvCover) {
                            @Override
                            public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                                super.onResourceReady(bitmap, anim);
                                Palette.Builder builder = new Palette.Builder(bitmap);
                                builder.generate(palette -> {
                                    Palette.Swatch swatch = palette.getMutedSwatch();
                                    if(swatch != null) {
                                        root.setBackgroundColor(swatch.getRgb());
                                        txtvPodcastTitle.setTextColor(swatch.getTitleTextColor());
                                        txtvEpisodeTitle.setTextColor(swatch.getBodyTextColor());
                                    }
                                });
                            }
                        });
            });
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
    public void onDataSetChanged(Playable media) {
        this.media = media;
        loadMediaInfo();
    }

}
