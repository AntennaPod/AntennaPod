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

import com.bumptech.glide.request.RequestOptions;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.event.ServiceEvent;
import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.util.playback.Playable;
import de.danoeh.antennapod.core.util.playback.PlaybackController;
import de.greenrobot.event.EventBus;
import io.reactivex.Maybe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

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
    private Disposable disposable;

    public ExternalPlayerFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.external_player_fragment,
                container, false);
        fragmentLayout = root.findViewById(R.id.fragmentLayout);
        imgvCover = root.findViewById(R.id.imgvCover);
        txtvTitle = root.findViewById(R.id.txtvTitle);
        butPlay = root.findViewById(R.id.butPlay);
        mFeedName = root.findViewById(R.id.txtvAuthor);
        mProgressBar = root.findViewById(R.id.episodeProgress);

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
            if (controller != null) {
                controller.playPause();
            }
        });
        loadMediaInfo();
    }

    public void onEventMainThread(ServiceEvent event) {
        Log.d(TAG, "onEvent(" + event + ")");
        if (event.action == ServiceEvent.Action.SERVICE_STARTED) {
            controller.init();
        }
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
                return ExternalPlayerFragment.this.loadMediaInfo();
            }

            @Override
            public void setupGUI() {
                ExternalPlayerFragment.this.loadMediaInfo();
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
        onPositionObserverUpdate();
    }

    @Override
    public void onStart() {
        super.onStart();
        controller = setupPlaybackController();
        controller.init();
        loadMediaInfo();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (controller != null) {
            controller.release();
            controller = null;
        }
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Fragment is about to be destroyed");
        if (disposable != null) {
            disposable.dispose();
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
                if (controller != null) {
                    controller.playPause();
                }
            });
        }
        controller.init();
    }

    private boolean loadMediaInfo() {
        Log.d(TAG, "Loading media info");
        if (controller == null) {
            Log.w(TAG, "loadMediaInfo was called while PlaybackController was null!");
            return false;
        }

        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Maybe.create(emitter -> {
                    Playable media = controller.getMedia();
                    if (media != null) {
                        emitter.onSuccess(media);
                    } else {
                        emitter.onComplete();
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(media -> updateUi((Playable) media),
                        error -> Log.e(TAG, Log.getStackTraceString(error)));
        return true;
    }

    private void updateUi(Playable media) {
        if (media != null) {
            txtvTitle.setText(media.getEpisodeTitle());
            mFeedName.setText(media.getFeedTitle());
            onPositionObserverUpdate();

            Glide.with(getActivity())
                    .load(media.getImageLocation())
                    .apply(new RequestOptions()
                        .placeholder(R.color.light_gray)
                        .error(R.color.light_gray)
                        .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                        .fitCenter()
                        .dontAnimate())
                    .into(imgvCover);

            fragmentLayout.setVisibility(View.VISIBLE);
            if (controller != null && controller.isPlayingVideoLocally()) {
                butPlay.setVisibility(View.GONE);
            } else {
                butPlay.setVisibility(View.VISIBLE);
            }
        } else {
            Log.w(TAG, "loadMediaInfo was called while the media object of playbackService was null!");
        }
    }

    public PlaybackController getPlaybackControllerTestingOnly() {
        return controller;
    }

    private void onPositionObserverUpdate() {
        if (controller == null) {
            return;
        } else if (controller.getPosition() == PlaybackService.INVALID_TIME
                || controller.getDuration() == PlaybackService.INVALID_TIME) {
            return;
        }
        mProgressBar.setProgress((int)
                ((double) controller.getPosition() / controller.getDuration() * 100));
    }
}
