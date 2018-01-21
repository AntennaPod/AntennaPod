package de.danoeh.antennapod.fragment;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.playback.Playable;
import de.danoeh.antennapod.core.util.playback.PlaybackController;

/**
 * Fragment which is supposed to be displayed outside of the MediaplayerActivity
 * if the PlaybackService is running
 */
public class ExternalPlayerFragment extends Fragment {
    public static final String TAG = "ExternalPlayerFragment";

    private ViewGroup fragmentLayout;
    private ImageView imgvCover;
    private TextView txtvTitle;
    private ImageButton butPlay;
    private TextView mFeedName;
    private ProgressBar mProgressBar;

    private PlaybackController controller;

    public ExternalPlayerFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.external_player_fragment,
                container, false);
        fragmentLayout = (ViewGroup) root.findViewById(R.id.fragmentLayout);
        imgvCover = (ImageView) root.findViewById(R.id.imgvCover);
        txtvTitle = (TextView) root.findViewById(R.id.txtvTitle);
        butPlay = (ImageButton) root.findViewById(R.id.butPlay);
        mFeedName = (TextView) root.findViewById(R.id.txtvAuthor);
        mProgressBar = (ProgressBar) root.findViewById(R.id.episodeProgress);

        fragmentLayout.setOnClickListener(v -> {
            Log.d(TAG, "layoutInfo was clicked");

            if (controller != null && controller.getMedia() != null) {
                Intent intent = PlaybackService.getPlayerActivityIntent(getActivity(), controller.getMedia());

                if (Build.VERSION.SDK_INT >= 16 && controller.getMedia().getMediaType() == MediaType.AUDIO) {
                    ActivityOptionsCompat options = ActivityOptionsCompat.
                            makeSceneTransitionAnimation(getActivity(), imgvCover, "coverTransition");
                    startActivity(intent, options.toBundle());
                } else {
                    startActivity(intent);
                }
            }
        });
        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        controller = setupPlaybackController();
        butPlay.setOnClickListener(v -> {
            if(controller != null) {
                controller.playPause();
            }
        });
    }

    private PlaybackController setupPlaybackController() {
        return new PlaybackController(getActivity(), true) {

            @Override
            public void onPositionObserverUpdate() {
                ExternalPlayerFragment.this.onPositionObserverUpdate();
            }

            @Override
            public ImageButton getPlayButton() {
                return butPlay;
            }

            @Override
            public boolean loadMediaInfo() {
                ExternalPlayerFragment fragment = ExternalPlayerFragment.this;
                if (fragment != null) {
                    return fragment.loadMediaInfo();
                } else {
                    return false;
                }
            }

            @Override
            public void onShutdownNotification() {
                playbackDone();
            }

            @Override
            public void onPlaybackEnd() {
                playbackDone();
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        controller.init();
        mProgressBar.setProgress((int)
                ((double) controller.getPosition() / controller.getDuration() * 100));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Fragment is about to be destroyed");
        if (controller != null) {
            controller.release();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (controller != null) {
            controller.pause();
        }
    }

    private void playbackDone() {
        if (fragmentLayout != null) {
            fragmentLayout.setVisibility(View.GONE);
        }
        if (controller != null) {
            controller.release();
        }
        controller = setupPlaybackController();
        if (butPlay != null) {
            butPlay.setOnClickListener(v -> {
                if(controller != null) {
                    controller.playPause();
                }
            });
        }
        controller.init();
    }

    private boolean loadMediaInfo() {
        Log.d(TAG, "Loading media info");
        if (controller != null && controller.serviceAvailable()) {
            Playable media = controller.getMedia();
            if (media != null) {
                txtvTitle.setText(media.getEpisodeTitle());
                mFeedName.setText(media.getFeedTitle());
                mProgressBar.setProgress((int)
                        ((double) controller.getPosition() / controller.getDuration() * 100));

                Glide.with(getActivity())
                        .load(media.getImageLocation())
                        .placeholder(R.color.light_gray)
                        .error(R.color.light_gray)
                        .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                        .fitCenter()
                        .dontAnimate()
                        .into(imgvCover);

                fragmentLayout.setVisibility(View.VISIBLE);
                if (controller.isPlayingVideoLocally()) {
                    butPlay.setVisibility(View.GONE);
                } else {
                    butPlay.setVisibility(View.VISIBLE);
                }
                return true;
            } else {
                Log.w(TAG,  "loadMediaInfo was called while the media object of playbackService was null!");
                return false;
            }
        } else {
            Log.w(TAG, "loadMediaInfo was called while playbackService was null!");
            return false;
        }
    }

    private String getPositionString(int position, int duration) {
        return Converter.getDurationStringLong(position) + " / "
                + Converter.getDurationStringLong(duration);
    }

    public PlaybackController getPlaybackControllerTestingOnly() {
        return controller;
    }

    private void onPositionObserverUpdate() {
        mProgressBar.setProgress((int)
                ((double) controller.getPosition() / controller.getDuration() * 100));
    }
}
