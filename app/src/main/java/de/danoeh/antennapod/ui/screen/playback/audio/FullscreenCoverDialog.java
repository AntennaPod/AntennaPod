package de.danoeh.antennapod.ui.screen.playback.audio;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.DialogFragment;
import androidx.media3.session.MediaController;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.request.RequestOptions;
import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.databinding.FullscreenCoverDialogBinding;
import de.danoeh.antennapod.event.PlayerStatusEvent;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.model.feed.Chapter;
import de.danoeh.antennapod.model.feed.EmbeddedChapterImage;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.playback.service.PlaybackController;
import de.danoeh.antennapod.playback.service.PlaybackService;
import de.danoeh.antennapod.playback.service.PlaybackServiceStarter;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import de.danoeh.antennapod.ui.appstartintent.MediaButtonStarter;
import de.danoeh.antennapod.ui.chapters.ChapterUtils;
import de.danoeh.antennapod.ui.episodes.ImageResourceUtils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Shows the cover (or the current chapter image) of the playing episode in an immersive
 * fullscreen view. Tapping toggles play/pause, the system back button/gesture closes the dialog
 * and the image is kept in sync with chapter changes while it is open.
 */
public class FullscreenCoverDialog extends DialogFragment {
    public static final String TAG = "FullscreenCoverDialog";
    private FullscreenCoverDialogBinding viewBinding;
    private Disposable disposable;
    private int displayedChapterIndex = -1;
    private Playable media;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_FRAME, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        viewBinding = FullscreenCoverDialogBinding.inflate(inflater, container, false);
        viewBinding.imgvCover.setOnClickListener(v -> togglePlayback());
        return viewBinding.getRoot();
    }

    private void togglePlayback() {
        if (BuildConfig.USE_MEDIA3_PLAYBACK_SERVICE) {
            if (PlaybackService.isRunning) {
                PlaybackController.bindToMedia3Service(getActivity(), MediaController::pause);
            } else if (media != null) {
                new PlaybackServiceStarter(getContext(), media)
                        .callEvenIfRunning(true)
                        .start();
            }
            return;
        }
        if (PlaybackService.isRunning
                && PlaybackPreferences.getCurrentPlayerStatus() == PlaybackPreferences.PLAYER_STATUS_PLAYING) {
            getContext().sendBroadcast(MediaButtonStarter.createIntent(getContext(), KeyEvent.KEYCODE_MEDIA_PAUSE));
        } else if (media != null) {
            new PlaybackServiceStarter(getContext(), media)
                    .callEvenIfRunning(true)
                    .start();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        applyImmersiveFullscreen();
        loadMediaInfo();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (disposable != null) {
            disposable.dispose();
        }
        viewBinding = null;
    }

    private void applyImmersiveFullscreen() {
        Window window = getDialog() != null ? getDialog().getWindow() : null;
        if (window == null) {
            return;
        }
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        WindowCompat.setDecorFitsSystemWindows(window, false);
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(window, window.getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    private void loadMediaInfo() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Maybe.<Playable>create(emitter -> {
            Playable media = DBReader.getFeedMedia(PlaybackPreferences.getCurrentlyPlayingFeedMediaId());
            if (media != null) {
                ChapterUtils.loadChapters(media, getContext(), false);
                emitter.onSuccess(media);
            } else {
                emitter.onComplete();
            }
        }).subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(media -> {
                    this.media = media;
                    displayedChapterIndex = Chapter.getAfterPosition(media.getChapters(), media.getPosition());
                    displayCoverImage();
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerStatusEvent(PlayerStatusEvent event) {
        loadMediaInfo();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(PlaybackPositionEvent event) {
        if (media == null) {
            return;
        }
        int newChapterIndex = Chapter.getAfterPosition(media.getChapters(), event.getPosition());
        if (newChapterIndex > -1 && newChapterIndex != displayedChapterIndex) {
            displayedChapterIndex = newChapterIndex;
            displayCoverImage();
        }
    }

    private void displayCoverImage() {
        if (viewBinding == null || media == null) {
            return;
        }
        RequestOptions options = new RequestOptions()
                .dontAnimate()
                .transform(new FitCenter());

        RequestBuilder<Drawable> cover = Glide.with(this)
                .load(media.getImageLocation())
                .error(Glide.with(this)
                        .load(ImageResourceUtils.getFallbackImageLocation(media))
                        .apply(options))
                .apply(options);

        if (displayedChapterIndex == -1 || media.getChapters() == null
                || TextUtils.isEmpty(media.getChapters().get(displayedChapterIndex).getImageUrl())) {
            cover.into(viewBinding.imgvCover);
        } else {
            Glide.with(this)
                    .load(EmbeddedChapterImage.getModelFor(media, displayedChapterIndex))
                    .apply(options)
                    .thumbnail(cover)
                    .error(cover)
                    .into(viewBinding.imgvCover);
        }
    }
}
