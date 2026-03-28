package de.danoeh.antennapod.ui.screen.playback.video;

import android.app.PictureInPictureParams;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Rational;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.media3.common.Player;
import androidx.media3.common.util.Util;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.databinding.Media3VideoPlayerActivityBinding;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.playback.service.Media3PlaybackService;
import de.danoeh.antennapod.playback.service.PlaybackController;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import de.danoeh.antennapod.ui.appstartintent.MainActivityStarter;
import de.danoeh.antennapod.ui.common.IntentUtils;
import de.danoeh.antennapod.ui.episodeslist.FeedItemMenuHandler;
import de.danoeh.antennapod.ui.screen.chapter.ChaptersFragment;
import de.danoeh.antennapod.ui.screen.playback.SleepTimerDialog;
import de.danoeh.antennapod.ui.screen.playback.TranscriptDialogFragment;
import de.danoeh.antennapod.ui.screen.playback.VariableSpeedDialog;
import de.danoeh.antennapod.ui.screen.playback.PlaybackControlsDialog;
import de.danoeh.antennapod.ui.share.ShareDialog;
import de.danoeh.antennapod.event.FavoritesEvent;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class Media3VideoPlayerActivity extends AppCompatActivity implements Toolbar.OnMenuItemClickListener {
    private static final String TAG = "M3VideoPlayerActivity";
    private Media3VideoPlayerActivityBinding viewBinding;
    private MediaController mediaController;
    private ListenableFuture<MediaController> controllerFuture;
    private FeedMedia currentMedia;
    private Disposable mediaLoadDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        supportRequestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        setTheme(R.style.Theme_AntennaPod_Dark);
        getSupportActionBar().hide();
        super.onCreate(savedInstanceState);
        viewBinding = Media3VideoPlayerActivityBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        viewBinding.playerView.setUseController(false);
        setupControlsView();
        setupFullScreenMode();
        setupPictureInPicture();
    }

    private void setupControlsView() {
        Toolbar toolbar = viewBinding.controlsView.getToolbar();
        toolbar.inflateMenu(R.menu.mediaplayer);
        toolbar.setOnMenuItemClickListener(this);
        toolbar.setNavigationOnClickListener(v -> {
            Intent intent = new Intent(Media3VideoPlayerActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
        toolbar.getMenu().findItem(R.id.player_switch_to_audio_only).setVisible(true);
        toolbar.getMenu().findItem(R.id.playback_speed).setVisible(true);
        toolbar.getMenu().findItem(R.id.player_show_chapters).setVisible(true);
        toolbar.getMenu().findItem(R.id.audio_controls).setVisible(true);

        viewBinding.controlsView.setListener(new VideoPlayerControlsView.ControlsListener() {
            @Override
            public void onPlayPause() {
                PlaybackController.bindToMedia3Service(Media3VideoPlayerActivity.this, controller -> {
                    if (controller.isPlaying()) {
                        controller.pause();
                    } else {
                        controller.play();
                    }
                });
            }

            @Override
            public void onRewind() {
                PlaybackController.bindToMedia3Service(Media3VideoPlayerActivity.this, MediaController::seekBack);
            }

            @Override
            public void onFastForward() {
                PlaybackController.bindToMedia3Service(Media3VideoPlayerActivity.this, MediaController::seekForward);
            }

            @Override
            public void onSeek(int positionMs) {
                PlaybackController.bindToMedia3Service(Media3VideoPlayerActivity.this,
                        controller -> controller.seekTo(positionMs));
            }
        });

        viewBinding.getRoot().setOnTouchListener((v, event) -> {
            viewBinding.controlsView.handleTouchEvent(event, PictureInPictureUtil.isInPictureInPictureMode(this));
            return true;
        });
    }

    private void setupFullScreenMode() {
        viewBinding.playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    private void setupPictureInPicture() {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }

        PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder();

        if (mediaController != null) {
            int videoWidth = mediaController.getVideoSize().width;
            int videoHeight = mediaController.getVideoSize().height;
            if (videoWidth > 0 && videoHeight > 0) {
                if (Build.VERSION.SDK_INT >= 33) {
                    Rational aspectRatio = new Rational(videoWidth, videoHeight);
                    builder.setAspectRatio(aspectRatio);
                }
            }
        }

        if (Build.VERSION.SDK_INT >= 31) {
            builder.setAutoEnterEnabled(true);
            if (viewBinding.playerView.getClipBounds() != null) {
                builder.setSourceRectHint(viewBinding.playerView.getClipBounds());
            }
        }

        setPictureInPictureParams(builder.build());
    }

    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (Build.VERSION.SDK_INT >= 26
                && PictureInPictureUtil.supportsPictureInPicture(this)
                && !PictureInPictureUtil.isInPictureInPictureMode(this)) {
            viewBinding.controlsView.hideControls(false);
            enterPictureInPictureMode(new PictureInPictureParams.Builder().build());
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode,
                                              @NonNull android.content.res.Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        viewBinding.playerView.setUseController(!isInPictureInPictureMode);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        SessionToken sessionToken = new SessionToken(this,
                new ComponentName(this, Media3PlaybackService.class));
        controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                mediaController = controllerFuture.get();
                viewBinding.playerView.setPlayer(mediaController);
                setupPictureInPicture();
                setupMedia3Listeners();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error getting media controller", e);
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        if (mediaLoadDisposable != null) {
            mediaLoadDisposable.dispose();
        }
        viewBinding.playerView.setPlayer(null);
        if (mediaController != null) {
            mediaController.release();
            mediaController = null;
        }
        if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
            controllerFuture = null;
        }
        viewBinding.controlsView.cancelAutoHide();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewBinding.playerView.setUseController(false);
    }

    private void setupMedia3Listeners() {
        if (mediaController == null) {
            return;
        }
        mediaController.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                viewBinding.controlsView.setPlayButtonShowsPlay(Util.shouldShowPlayButton(mediaController));
                if (isPlaying) {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                } else {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
                loadMediaInfo();
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                viewBinding.controlsView.setPlayButtonShowsPlay(Util.shouldShowPlayButton(mediaController));
                if (playbackState == Player.STATE_BUFFERING) {
                    viewBinding.controlsView.setProgressBarVisibility(View.VISIBLE);
                } else {
                    viewBinding.controlsView.setProgressBarVisibility(View.INVISIBLE);
                }
            }
        });
        loadMediaInfo();
    }

    private void loadMediaInfo() {
        if (mediaLoadDisposable != null) {
            mediaLoadDisposable.dispose();
        }
        mediaLoadDisposable = Maybe.fromCallable(() -> DBReader.getFeedMedia(
                        PlaybackPreferences.getCurrentlyPlayingFeedMediaId()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(media -> {
                    currentMedia = media;
                    FeedItemMenuHandler.onPrepareMenu(viewBinding.controlsView.getToolbar().getMenu(),
                             Collections.singletonList(currentMedia.getItem()));
                });
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.player_switch_to_audio_only) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.player_show_chapters) {
            new ChaptersFragment().show(getSupportFragmentManager(), ChaptersFragment.TAG);
            return true;
        } else if (item.getItemId() == R.id.transcript_item) {
            new TranscriptDialogFragment().show(getSupportFragmentManager(), TranscriptDialogFragment.TAG);
            return true;
        } else if (item.getItemId() == R.id.disable_sleeptimer_item
                || item.getItemId() == R.id.set_sleeptimer_item) {
            new SleepTimerDialog().show(getSupportFragmentManager(), "SleepTimerDialog");
            return true;
        } else if (item.getItemId() == R.id.audio_controls) {
            PlaybackControlsDialog dialog = PlaybackControlsDialog.newInstance();
            dialog.show(getSupportFragmentManager(), "playback_controls");
            return true;
        } else if (item.getItemId() == R.id.playback_speed) {
            new VariableSpeedDialog().show(getSupportFragmentManager(), null);
            return true;
        }

        if (currentMedia == null) {
            return false;
        }

        if (item.getItemId() == R.id.add_to_favorites_item) {
            DBWriter.addFavoriteItem(currentMedia.getItem());
        } else if (item.getItemId() == R.id.remove_from_favorites_item) {
            DBWriter.removeFavoriteItem(currentMedia.getItem());
        } else if (item.getItemId() == R.id.open_feed_item) {
            new MainActivityStarter(this).withOpenFeed(currentMedia.getItem().getFeedId())
                    .withClearTop().start();
        } else if (item.getItemId() == R.id.visit_website_item) {
            IntentUtils.openInBrowser(this, getWebsiteLinkWithFallback(currentMedia));
        } else if (item.getItemId() == R.id.share_item) {
            ShareDialog.newInstance(currentMedia.getItem()).show(getSupportFragmentManager(), "ShareDialog");
        } else {
            return false;
        }
        return true;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void favoritesChanged(FavoritesEvent event) {
        loadMediaInfo();
    }

    private static String getWebsiteLinkWithFallback(FeedMedia media) {
        if (media == null) {
            return null;
        } else if (StringUtils.isNotBlank(media.getWebsiteLink())) {
            return media.getWebsiteLink();
        } else  {
            return media.getItem().getFeed().getLink();
        }
    }
}
