package de.danoeh.antennapod.ui.screen.playback.video;

import android.app.PictureInPictureParams;
import android.app.PictureInPictureUiState;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.databinding.VideoplayerActivityBinding;
import de.danoeh.antennapod.event.FeedItemEvent;
import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.event.PlayerErrorEvent;
import de.danoeh.antennapod.event.playback.BufferUpdateEvent;
import de.danoeh.antennapod.event.playback.PlaybackServiceEvent;
import de.danoeh.antennapod.event.playback.SleepTimerUpdatedEvent;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.playback.base.PlayerStatus;
import de.danoeh.antennapod.playback.cast.CastEnabledActivity;
import de.danoeh.antennapod.playback.service.PlaybackController;
import de.danoeh.antennapod.playback.service.PlaybackService;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.appstartintent.MainActivityStarter;
import de.danoeh.antennapod.ui.common.IntentUtils;
import de.danoeh.antennapod.ui.episodeslist.FeedItemMenuHandler;
import de.danoeh.antennapod.ui.screen.chapter.ChaptersFragment;
import de.danoeh.antennapod.ui.screen.playback.MediaPlayerErrorDialog;
import de.danoeh.antennapod.ui.screen.playback.PlaybackControlsDialog;
import de.danoeh.antennapod.ui.screen.playback.SleepTimerDialog;
import de.danoeh.antennapod.ui.screen.playback.TranscriptDialogFragment;
import de.danoeh.antennapod.ui.screen.playback.VariableSpeedDialog;
import de.danoeh.antennapod.ui.share.ShareDialog;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Collections;

/**
 * Activity for playing video files.
 */
public class VideoplayerActivity extends CastEnabledActivity
        implements Toolbar.OnMenuItemClickListener {
    private static final String TAG = "VideoplayerActivity";

    private boolean videoSurfaceCreated = false;
    private boolean destroyingDueToReload = false;
    private boolean switchToAudioOnly = false;
    private VideoplayerActivityBinding viewBinding;
    private PlaybackController controller;
    private Disposable disposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        supportRequestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        setTheme(R.style.Theme_AntennaPod_Dark);
        getSupportActionBar().hide();
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate()");

        getWindow().setFormat(PixelFormat.TRANSPARENT);
        viewBinding = VideoplayerActivityBinding.inflate(LayoutInflater.from(this));
        setContentView(viewBinding.getRoot());
        setupControlsView();
        setupView();
        setupPip();
    }

    private void setupControlsView() {
        viewBinding.controlsView.setListener(new VideoPlayerControlsView.ControlsListener() {
            @Override
            public void onPlayPause() {
                VideoplayerActivity.this.onPlayPause();
            }

            @Override
            public void onRewind() {
                VideoplayerActivity.this.onRewind();
            }

            @Override
            public void onFastForward() {
                VideoplayerActivity.this.onFastForward();
            }

            @Override
            public void onSeek(int positionMs) {
                if (controller != null) {
                    controller.seekTo(positionMs);
                }
            }
        });

        viewBinding.videoPlayerContainer.setOnTouchListener((v, event) -> {
            viewBinding.controlsView.handleTouchEvent(event, PictureInPictureUtil.isInPictureInPictureMode(this));
            return true;
        });

        Toolbar toolbar = viewBinding.controlsView.getToolbar();
        toolbar.inflateMenu(R.menu.mediaplayer);
        requestCastButton(toolbar.getMenu());
        toolbar.setOnMenuItemClickListener(this);
        toolbar.setNavigationOnClickListener(v -> {
            Intent intent = new Intent(VideoplayerActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    void setupPip() {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder();
        if (Build.VERSION.SDK_INT >= 31) {
            builder.setAutoEnterEnabled(true);
            builder.setSourceRectHint(viewBinding.getRoot().getClipBounds());
        }
        setPictureInPictureParams(builder.build());
    }

    @Override
    public void onPictureInPictureUiStateChanged(@NonNull PictureInPictureUiState pipState) {
        super.onPictureInPictureUiStateChanged(pipState);
        if (Build.VERSION.SDK_INT < 35) {
            return;
        }
        if (pipState.isTransitioningToPip()) {
            viewBinding.controlsView.hideControls(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        switchToAudioOnly = false;
        if (PlaybackService.isCasting()) {
            Intent intent = PlaybackService.getPlayerActivityIntent(this);
            if (!intent.getComponent().getClassName().equals(VideoplayerActivity.class.getName())) {
                destroyingDueToReload = true;
                finish();
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onStop() {
        if (controller != null) {
            controller.release();
            controller = null; // prevent leak
        }
        if (disposable != null) {
            disposable.dispose();
        }
        viewBinding.controlsView.cancelAutoHide();
        EventBus.getDefault().unregister(this);
        super.onStop();
        if (!PictureInPictureUtil.isInPictureInPictureMode(this)) {
            viewBinding.controlsView.hideControls(false);
        }
        // Controller released; we will not receive buffering updates
        viewBinding.controlsView.setProgressBarVisibility(View.GONE);
    }

    @Override
    public void onUserLeaveHint() {
        if (!PictureInPictureUtil.isInPictureInPictureMode(this)) {
            compatEnterPictureInPicture();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        controller = newPlaybackController();
        controller.init();
        loadMediaInfo();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onPause() {
        if (!PictureInPictureUtil.isInPictureInPictureMode(this)) {
            if (controller != null && controller.getStatus() == PlayerStatus.PLAYING) {
                controller.pause();
            }
        }
        super.onPause();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Glide.get(this).trimMemory(level);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Glide.get(this).clearMemory();
    }

    private PlaybackController newPlaybackController() {
        return new PlaybackController(this) {
            @Override
            protected void updatePlayButtonShowsPlay(boolean showPlay) {
                viewBinding.controlsView.setPlayButtonShowsPlay(showPlay);
                if (showPlay) {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                } else {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    setupVideoAspectRatio();
                    if (videoSurfaceCreated && controller != null) {
                        Log.d(TAG, "Videosurface already created, setting videosurface now");
                        controller.setVideoSurface(viewBinding.videoView.getHolder());
                    }
                }
            }

            @Override
            public void loadMediaInfo() {
                VideoplayerActivity.this.loadMediaInfo();
            }

            @Override
            public void onPlaybackEnd() {
                finish();
            }
        };
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    public void bufferUpdate(BufferUpdateEvent event) {
        if (event.hasStarted()) {
            viewBinding.controlsView.setProgressBarVisibility(View.VISIBLE);
        } else if (event.hasEnded()) {
            viewBinding.controlsView.setProgressBarVisibility(View.INVISIBLE);
        } else {
            viewBinding.controlsView.setBufferingProgress(event.getProgress());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    public void sleepTimerUpdate(SleepTimerUpdatedEvent event) {
        if (event.isCancelled() || event.wasJustEnabled()) {
            updateToolbar(null);
        }
    }

    protected void loadMediaInfo() {
        Log.d(TAG, "loadMediaInfo()");
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Maybe.<Pair<Playable, FeedItem>>create(emitter -> {
            if (controller == null) {
                emitter.onComplete();
                return;
            }
            Playable media = controller.getMedia();
            if (media == null) {
                emitter.onComplete();
                return;
            }
            FeedItem feedItem = getFeedItem(controller.getMedia());
            if (feedItem != null) {
                feedItem = DBReader.getFeedItem(feedItem.getId());
            }
            emitter.onSuccess(new Pair<>(media, feedItem));
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            final Playable media = result.first;
                            if (controller.getStatus() == PlayerStatus.PLAYING
                                    && !controller.isPlayingVideoLocally()) {
                                Log.d(TAG, "Closing, no longer video");
                                destroyingDueToReload = true;
                                finish();
                                new MainActivityStarter(this).withOpenPlayer().start();
                                return;
                            }
                            updateToolbar(media);
                        }, error -> Log.e(TAG, Log.getStackTraceString(error))
                );
    }

    protected void setupView() {
        viewBinding.videoView.getHolder().addCallback(surfaceHolderCallback);
        viewBinding.videoView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        viewBinding.videoPlayerContainer.getViewTreeObserver().addOnGlobalLayoutListener(() ->
                viewBinding.videoView.setAvailableSize(
                        viewBinding.videoPlayerContainer.getWidth(), viewBinding.videoPlayerContainer.getHeight()));
    }

    private void setupVideoAspectRatio() {
        if (videoSurfaceCreated && controller != null) {
            Pair<Integer, Integer> videoSize = controller.getVideoSize();
            if (videoSize != null && videoSize.first > 0 && videoSize.second > 0) {
                Log.d(TAG, "Width,height of video: " + videoSize.first + ", " + videoSize.second);
                viewBinding.videoView.setVideoSize(videoSize.first, videoSize.second);
            } else {
                Log.e(TAG, "Could not determine video size");
            }
        }
    }

    void onRewind() {
        if (controller == null) {
            return;
        }
        int curr = controller.getPosition();
        controller.seekTo(curr - UserPreferences.getRewindSecs() * 1000);
    }

    void onPlayPause() {
        if (controller == null) {
            return;
        }
        controller.playPause();
    }

    void onFastForward() {
        if (controller == null) {
            return;
        }
        int curr = controller.getPosition();
        controller.seekTo(curr + UserPreferences.getFastForwardSecs() * 1000);
    }

    private final SurfaceHolder.Callback surfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            holder.setFixedSize(width, height);
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "Videoview holder created");
            videoSurfaceCreated = true;
            if (controller != null && controller.getStatus() == PlayerStatus.PLAYING) {
                controller.setVideoSurface(holder);
            }
            setupVideoAspectRatio();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "Videosurface was destroyed");
            videoSurfaceCreated = false;
            if (controller != null && !destroyingDueToReload && !switchToAudioOnly) {
                controller.notifyVideoSurfaceAbandoned();
            }
        }
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlaybackServiceChanged(PlaybackServiceEvent event) {
        if (event.action == PlaybackServiceEvent.Action.SERVICE_SHUT_DOWN) {
            finish();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaPlayerError(PlayerErrorEvent event) {
        MediaPlayerErrorDialog.show(this, event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFeedItemEvent(FeedItemEvent event) {
        loadMediaInfo();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(MessageEvent event) {
        Log.d(TAG, "onEvent(" + event + ")");
        final MaterialAlertDialogBuilder errorDialog = new MaterialAlertDialogBuilder(this);
        errorDialog.setMessage(event.message);
        if (event.action != null) {
            errorDialog.setPositiveButton(event.actionText, (dialog, which) -> event.action.accept(this));
        }
        errorDialog.show();
    }

    private void updateToolbar(Playable media) {
        Toolbar toolbar = viewBinding.controlsView.getToolbar();
        if (media != null) {
            toolbar.setSubtitle(media.getEpisodeTitle());
            toolbar.setTitle(media.getFeedTitle());
        }
        boolean isFeedMedia = (media instanceof FeedMedia);

        Menu menu = toolbar.getMenu();
        menu.findItem(R.id.open_feed_item).setVisible(isFeedMedia); // FeedMedia implies it belongs to a Feed
        if (isFeedMedia) {
            FeedItemMenuHandler.onPrepareMenu(menu, Collections.singletonList(((FeedMedia) media).getItem()));
        }

        if (controller != null) {
            menu.findItem(R.id.set_sleeptimer_item).setVisible(!controller.sleepTimerActive());
            menu.findItem(R.id.disable_sleeptimer_item).setVisible(controller.sleepTimerActive());
            menu.findItem(R.id.audio_controls).setVisible(controller.getAudioTracks().size() >= 2);
        }

        menu.findItem(R.id.player_switch_to_audio_only).setVisible(true);
        menu.findItem(R.id.playback_speed).setVisible(true);
        menu.findItem(R.id.player_show_chapters).setVisible(true);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.player_switch_to_audio_only) {
            switchToAudioOnly = true;
            finish();
            return true;
        } else if (item.getItemId() == R.id.player_show_chapters) {
            new ChaptersFragment().show(getSupportFragmentManager(), ChaptersFragment.TAG);
            return true;
        } else if (item.getItemId() == R.id.transcript_item) {
            new TranscriptDialogFragment().show(getSupportFragmentManager(), TranscriptDialogFragment.TAG);
            return true;
        }

        if (controller == null) {
            return false;
        }

        Playable media = controller.getMedia();
        if (media == null) {
            return false;
        }
        final @Nullable FeedItem feedItem = getFeedItem(media);
        if (item.getItemId() == R.id.add_to_favorites_item && feedItem != null) {
            DBWriter.addFavoriteItem(feedItem);
        } else if (item.getItemId() == R.id.remove_from_favorites_item && feedItem != null) {
            DBWriter.removeFavoriteItem(feedItem);
        } else if (item.getItemId() == R.id.disable_sleeptimer_item
                || item.getItemId() == R.id.set_sleeptimer_item) {
            new SleepTimerDialog().show(getSupportFragmentManager(), "SleepTimerDialog");
        } else if (item.getItemId() == R.id.audio_controls) {
            PlaybackControlsDialog dialog = PlaybackControlsDialog.newInstance();
            dialog.show(getSupportFragmentManager(), "playback_controls");
        } else if (item.getItemId() == R.id.open_feed_item && feedItem != null) {
            new MainActivityStarter(this).withOpenFeed(feedItem.getFeedId()).withClearTop().start();
        } else if (item.getItemId() == R.id.visit_website_item) {
            IntentUtils.openInBrowser(VideoplayerActivity.this, getWebsiteLinkWithFallback(media));
        } else if (item.getItemId() == R.id.share_item && feedItem != null) {
            ShareDialog shareDialog = ShareDialog.newInstance(feedItem);
            shareDialog.show(getSupportFragmentManager(), "ShareEpisodeDialog");
        } else if (item.getItemId() == R.id.playback_speed) {
            new VariableSpeedDialog().show(getSupportFragmentManager(), null);
        } else {
            return false;
        }
        return true;
    }

    private static String getWebsiteLinkWithFallback(Playable media) {
        if (media == null) {
            return null;
        } else if (StringUtils.isNotBlank(media.getWebsiteLink())) {
            return media.getWebsiteLink();
        } else if (media instanceof FeedMedia) {
            return (((FeedMedia) media).getItem()).getLinkWithFallback();
        }
        return null;
    }

    @Nullable
    private static FeedItem getFeedItem(@Nullable Playable playable) {
        if (playable instanceof FeedMedia) {
            return ((FeedMedia) playable).getItem();
        } else {
            return null;
        }
    }

    private void compatEnterPictureInPicture() {
        if (PictureInPictureUtil.supportsPictureInPicture(this) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            viewBinding.controlsView.hideControls(false);
            enterPictureInPictureMode();
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        View currentFocus = getCurrentFocus();
        if (currentFocus instanceof EditText) {
            return super.onKeyUp(keyCode, event);
        }

        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        switch (keyCode) {
            case KeyEvent.KEYCODE_P: //Fallthrough
            case KeyEvent.KEYCODE_SPACE:
                onPlayPause();
                viewBinding.controlsView.toggleControls();
                return true;
            case KeyEvent.KEYCODE_J: //Fallthrough
            case KeyEvent.KEYCODE_A:
            case KeyEvent.KEYCODE_COMMA:
                onRewind();
                return true;
            case KeyEvent.KEYCODE_K: //Fallthrough
            case KeyEvent.KEYCODE_D:
            case KeyEvent.KEYCODE_PERIOD:
                onFastForward();
                return true;
            case KeyEvent.KEYCODE_F: //Fallthrough
            case KeyEvent.KEYCODE_ESCAPE:
                //Exit fullscreen mode
                onBackPressed();
                return true;
            case KeyEvent.KEYCODE_I:
                compatEnterPictureInPicture();
                return true;
            case KeyEvent.KEYCODE_PLUS: //Fallthrough
            case KeyEvent.KEYCODE_W:
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                return true;
            case KeyEvent.KEYCODE_MINUS: //Fallthrough
            case KeyEvent.KEYCODE_S:
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                return true;
            case KeyEvent.KEYCODE_M:
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI);
                return true;
            default:
                break;
        }

        //Go to x% of video:
        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
            controller.seekTo((int) (0.1f * (keyCode - KeyEvent.KEYCODE_0) * controller.getDuration()));
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }
}
