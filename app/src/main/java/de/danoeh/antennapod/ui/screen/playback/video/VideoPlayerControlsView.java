package de.danoeh.antennapod.ui.screen.playback.video;

import android.content.Context;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.VideoPlayerControlsBinding;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.common.Converter;
import de.danoeh.antennapod.ui.episodes.TimeSpeedConverter;
import de.danoeh.antennapod.ui.screen.feed.preferences.SkipPreferenceDialog;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.event.playback.SpeedChangedEvent;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class VideoPlayerControlsView extends FrameLayout {
    private static final long AUTO_HIDE_DELAY_MS = 2500;

    private VideoPlayerControlsBinding binding;
    private boolean controlsShowing = true;
    private long lastScreenTap = 0;
    private final Point tapDownPosition = new Point();
    private final Handler autoHideHandler = new Handler(Looper.getMainLooper());
    private int maxInsetBottom = 0;
    private float seekProgress = 0f;
    private int currentDuration = 0;
    private float currentSpeedMultiplier = 1.0f;

    public interface ControlsListener {
        void onPlayPause();

        void onRewind();

        void onFastForward();

        void onSeek(int positionMs);
    }

    private ControlsListener listener;

    public VideoPlayerControlsView(@NonNull Context context) {
        super(context);
        init();
    }

    public VideoPlayerControlsView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VideoPlayerControlsView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelAutoHide();
        EventBus.getDefault().unregister(this);
        super.onDetachedFromWindow();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(PlaybackPositionEvent event) {
        updatePosition(event.getPosition(), event.getDuration(), currentSpeedMultiplier);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(SpeedChangedEvent event) {
        currentSpeedMultiplier = event.getNewSpeed();
    }

    private void init() {
        binding = VideoPlayerControlsBinding.inflate(LayoutInflater.from(getContext()), this, true);
        hideControls(false);
        binding.playButton.setIsVideoScreen(true);
        binding.playButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlayPause();
            }
            resetAutoHide();
        });

        binding.rewindButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRewind();
            }
            resetAutoHide();
        });

        binding.rewindButton.setOnLongClickListener(v -> {
            SkipPreferenceDialog.showSkipPreference(getContext(),
                    SkipPreferenceDialog.SkipDirection.SKIP_REWIND, null);
            return true;
        });

        binding.fastForwardButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFastForward();
            }
            resetAutoHide();
        });

        binding.fastForwardButton.setOnLongClickListener(v -> {
            SkipPreferenceDialog.showSkipPreference(getContext(),
                    SkipPreferenceDialog.SkipDirection.SKIP_FORWARD, null);
            return true;
        });

        binding.durationLabel.setOnClickListener(v -> {
            UserPreferences.setShowRemainTimeSetting(!UserPreferences.shouldShowRemainingTime());
            resetAutoHide();
        });

        binding.bottomControlsContainer.setOnTouchListener((view, motionEvent) -> true);

        binding.sbPosition.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    seekProgress = progress / ((float) seekBar.getMax());
                    updateSeekPosition((int) (seekProgress * currentDuration), currentSpeedMultiplier);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                binding.seekCardView.setScaleX(.8f);
                binding.seekCardView.setScaleY(.8f);
                binding.seekCardView.animate()
                        .setInterpolator(new FastOutSlowInInterpolator())
                        .alpha(1f).scaleX(1f).scaleY(1f)
                        .setDuration(200)
                        .start();
                cancelAutoHide();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (listener != null) {
                    listener.onSeek((int) (seekProgress * currentDuration));
                }
                binding.seekCardView.setScaleX(1f);
                binding.seekCardView.setScaleY(1f);
                binding.seekCardView.animate()
                        .setInterpolator(new FastOutSlowInInterpolator())
                        .alpha(0f).scaleX(.8f).scaleY(.8f)
                        .setDuration(200)
                        .start();
                resetAutoHide();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(this, (v, insets) -> {
            Insets systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            maxInsetBottom = Math.max(maxInsetBottom, systemBarInsets.bottom);
            int horizontal = Math.max(systemBarInsets.left, systemBarInsets.right);
            binding.seekBarContainer.setPadding(horizontal, 0, horizontal, maxInsetBottom);
            binding.toolbarContainer.setPadding(horizontal, 0, horizontal, 0);
            return insets;
        });
    }

    public void setListener(ControlsListener listener) {
        this.listener = listener;
    }

    public Toolbar getToolbar() {
        return binding.toolbar;
    }

    public void setOnTouchListener(View.OnTouchListener listener) {
        super.setOnTouchListener(listener);
    }

    public void updatePosition(int positionMs, int durationMs, float speedMultiplier) {
        currentDuration = durationMs;
        currentSpeedMultiplier = speedMultiplier;
        TimeSpeedConverter converter = new TimeSpeedConverter(speedMultiplier);
        int currentPosition = converter.convert(positionMs);
        int duration = converter.convert(durationMs);

        if (currentPosition == Playable.INVALID_TIME || duration == Playable.INVALID_TIME) {
            return;
        }

        binding.positionLabel.setText(Converter.getDurationStringLong(currentPosition));

        if (UserPreferences.shouldShowRemainingTime()) {
            int remainingTime = converter.convert(durationMs - positionMs);
            binding.durationLabel.setText("-" + Converter.getDurationStringLong(remainingTime));
        } else {
            binding.durationLabel.setText(Converter.getDurationStringLong(duration));
        }

        float progress = ((float) currentPosition) / duration;
        binding.sbPosition.setProgress((int) (progress * binding.sbPosition.getMax()));
    }

    public void updateSeekPosition(int positionMs, float speedMultiplier) {
        TimeSpeedConverter converter = new TimeSpeedConverter(speedMultiplier);
        int seekPosition = converter.convert(positionMs);
        binding.seekPositionLabel.setText(Converter.getDurationStringLong(seekPosition));
    }

    public void setPlayButtonShowsPlay(boolean showPlay) {
        binding.playButton.setIsShowPlay(showPlay);
    }

    public void setBufferingProgress(float progress) {
        binding.sbPosition.setSecondaryProgress((int) (progress * binding.sbPosition.getMax()));
    }

    public void showControls() {
        if (controlsShowing) {
            return;
        }
        controlsShowing = true;
        binding.bottomControlsContainer.setVisibility(View.VISIBLE);
        binding.controlsContainer.setVisibility(View.VISIBLE);
        binding.toolbarContainer.setVisibility(View.VISIBLE);
        Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in);
        if (animation != null) {
            binding.bottomControlsContainer.startAnimation(animation);
            binding.controlsContainer.startAnimation(animation);
            binding.toolbarContainer.startAnimation(animation);
        }
        resetAutoHide();
    }

    public void hideControls(boolean animate) {
        if (!controlsShowing) {
            return;
        }
        controlsShowing = false;
        if (animate) {
            Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.fade_out);
            if (animation != null) {
                binding.bottomControlsContainer.startAnimation(animation);
                binding.controlsContainer.startAnimation(animation);
                binding.toolbarContainer.startAnimation(animation);
            }
        }
        binding.bottomControlsContainer.setVisibility(View.GONE);
        binding.controlsContainer.setVisibility(View.GONE);
        binding.toolbarContainer.setVisibility(View.GONE);
        cancelAutoHide();
    }

    public void toggleControls() {
        if (controlsShowing) {
            hideControls(true);
        } else {
            showControls();
        }
    }

    public void resetAutoHide() {
        cancelAutoHide();
        autoHideHandler.postDelayed(this::autoHide, AUTO_HIDE_DELAY_MS);
    }

    public void cancelAutoHide() {
        autoHideHandler.removeCallbacksAndMessages(null);
    }

    private void autoHide() {
        if (controlsShowing) {
            hideControls(true);
        }
    }

    public void handleTouchEvent(MotionEvent event, boolean isPictureInPictureMode) {
        if (isPictureInPictureMode) {
            return;
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            tapDownPosition.x = (int) event.getX();
            tapDownPosition.y = (int) event.getY();
            return;
        }

        if (event.getAction() != MotionEvent.ACTION_UP) {
            return;
        }

        cancelAutoHide();

        if (System.currentTimeMillis() - lastScreenTap < 300) {
            if (event.getX() > getMeasuredWidth() / 2.0f) {
                if (listener != null) {
                    listener.onFastForward();
                }
                showSkipAnimation(true);
            } else {
                if (listener != null) {
                    listener.onRewind();
                }
                showSkipAnimation(false);
            }
            if (controlsShowing) {
                hideControls(false);
            }
            lastScreenTap = System.currentTimeMillis();
            return;
        }

        double moveDistance = Math.sqrt(Math.pow(event.getX() - tapDownPosition.x, 2)
                + Math.pow(event.getY() - tapDownPosition.y, 2));
        if (moveDistance > 0.1 * getMeasuredHeight()) {
            return;
        }

        toggleControls();
        lastScreenTap = System.currentTimeMillis();
    }

    private void showSkipAnimation(boolean isForward) {
        AnimationSet skipAnimation = new AnimationSet(true);
        skipAnimation.addAnimation(new ScaleAnimation(1f, 2f, 1f, 2f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f));
        skipAnimation.addAnimation(new AlphaAnimation(1f, 0f));
        skipAnimation.setFillAfter(false);
        skipAnimation.setDuration(800);

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) binding.skipAnimationImage.getLayoutParams();
        if (isForward) {
            binding.skipAnimationImage.setImageResource(R.drawable.ic_fast_forward_video_white);
            params.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
        } else {
            binding.skipAnimationImage.setImageResource(R.drawable.ic_fast_rewind_video_white);
            params.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
        }

        binding.skipAnimationImage.setVisibility(View.VISIBLE);
        binding.skipAnimationImage.setLayoutParams(params);
        binding.skipAnimationImage.startAnimation(skipAnimation);
        skipAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                binding.skipAnimationImage.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    public void setProgressBarVisibility(int visibility) {
        binding.progressBar.setVisibility(visibility);
    }
}
