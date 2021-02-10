package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayoutMediator;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.CastEnabledActivity;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.event.FavoritesEvent;
import de.danoeh.antennapod.core.event.PlaybackPositionEvent;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.util.PlaybackSpeedUtils;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.IntentUtils;
import de.danoeh.antennapod.core.util.TimeSpeedConverter;
import de.danoeh.antennapod.core.util.playback.MediaPlayerError;
import de.danoeh.antennapod.core.util.playback.Playable;
import de.danoeh.antennapod.core.util.playback.PlaybackController;
import de.danoeh.antennapod.databinding.AudioplayerFragmentBinding;
import de.danoeh.antennapod.dialog.PlaybackControlsDialog;
import de.danoeh.antennapod.dialog.SkipPreferenceDialog;
import de.danoeh.antennapod.dialog.SleepTimerDialog;
import de.danoeh.antennapod.dialog.VariableSpeedDialog;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import io.reactivex.Maybe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

/**
 * Shows the audio player.
 */
public class AudioPlayerFragment extends Fragment implements
        SeekBar.OnSeekBarChangeListener, Toolbar.OnMenuItemClickListener {
    public static final String TAG = "AudioPlayerFragment";
    private static final int POS_COVER = 0;
    private static final int POS_DESCR = 1;
    private static final int POS_CHAPTERS = 2;
    private static final int NUM_CONTENT_FRAGMENTS = 3;
    private static final String PREFS = "AudioPlayerFragmentPreferences";
    private static final String PREF_SHOW_TIME_LEFT = "showTimeLeft";
    private static final float EPSILON = 0.001f;

    private AudioplayerFragmentBinding viewBinding;

    private PlaybackController controller;
    private Disposable disposable;
    private boolean showTimeLeft;
    private boolean hasChapters = false;
    private TabLayoutMediator tabLayoutMediator;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        viewBinding = AudioplayerFragmentBinding.inflate(getLayoutInflater());

        viewBinding.toolbar.setTitle("");
        viewBinding.toolbar.setNavigationOnClickListener(v ->
                ((MainActivity) getActivity()).getBottomSheet().setState(BottomSheetBehavior.STATE_COLLAPSED));
        viewBinding.toolbar.setOnMenuItemClickListener(this);

        ExternalPlayerFragment externalPlayerFragment = new ExternalPlayerFragment();
        getChildFragmentManager().beginTransaction()
                .replace(viewBinding.playerFragment.getId(), externalPlayerFragment, ExternalPlayerFragment.TAG)
                .commit();

        setupLengthTextView();
        setupControlButtons();
        setupPlaybackSpeedButton();
        viewBinding.positionSeek.setOnSeekBarChangeListener(this);

        viewBinding.pager.setAdapter(new AudioPlayerPagerAdapter(this));
        // Required for getChildAt(int) in ViewPagerBottomSheetBehavior to return the correct page
        viewBinding.pager.setOffscreenPageLimit((int) NUM_CONTENT_FRAGMENTS);
        viewBinding.pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                viewBinding.pager.post(() -> {
                    if (getActivity() != null) {
                        // By the time this is posted, the activity might be closed again.
                        ((MainActivity) getActivity()).getBottomSheet().updateScrollingChild();
                    }
                });
            }
        });

        tabLayoutMediator = new TabLayoutMediator(viewBinding.tabLayout, viewBinding.pager, (tab, position) -> {
            tab.view.setAlpha(1.0f);
            switch (position) {
                case POS_COVER:
                    tab.setText(R.string.cover_label);
                    break;
                case POS_DESCR:
                    tab.setText(R.string.description_label);
                    break;
                case POS_CHAPTERS:
                    tab.setText(R.string.chapters_label);
                    if (!hasChapters) {
                        tab.view.setAlpha(0.5f);
                    }
                    break;
                default:
                    break;
            }
        });
        tabLayoutMediator.attach();
        return viewBinding.getRoot();
    }

    public void setHasChapters(boolean hasChapters) {
        this.hasChapters = hasChapters;
        tabLayoutMediator.detach();
        tabLayoutMediator.attach();
    }

    public View getExternalPlayerHolder() {
        return viewBinding.playerFragment;
    }

    private void setupControlButtons() {
        viewBinding.rewindButton.setOnClickListener(v -> {
            if (controller != null) {
                int curr = controller.getPosition();
                controller.seekTo(curr - UserPreferences.getRewindSecs() * 1000);
            }
        });
        viewBinding.rewindButton.setOnLongClickListener(v -> {
            SkipPreferenceDialog.showSkipPreference(getContext(),
                    SkipPreferenceDialog.SkipDirection.SKIP_REWIND, viewBinding.rewindSecondsLabel);
            return true;
        });
        viewBinding.playButton.setOnClickListener(v -> {
            if (controller != null) {
                controller.init();
                controller.playPause();
            }
        });
        viewBinding.forwardButton.setOnClickListener(v -> {
            if (controller != null) {
                int curr = controller.getPosition();
                controller.seekTo(curr + UserPreferences.getFastForwardSecs() * 1000);
            }
        });
        viewBinding.forwardButton.setOnLongClickListener(v -> {
            SkipPreferenceDialog.showSkipPreference(getContext(),
                    SkipPreferenceDialog.SkipDirection.SKIP_FORWARD, viewBinding.forwardLabel);
            return false;
        });
        viewBinding.skipButton.setOnClickListener(v ->
                IntentUtils.sendLocalBroadcast(getActivity(), PlaybackService.ACTION_SKIP_CURRENT_EPISODE));
    }

    private void setupLengthTextView() {
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        showTimeLeft = prefs.getBoolean(PREF_SHOW_TIME_LEFT, false);
        viewBinding.durationLabel.setOnClickListener(v -> {
            if (controller == null) {
                return;
            }
            showTimeLeft = !showTimeLeft;
            prefs.edit().putBoolean(PREF_SHOW_TIME_LEFT, showTimeLeft).apply();
            updatePosition(new PlaybackPositionEvent(controller.getPosition(), controller.getDuration()));
        });
    }

    private void setupPlaybackSpeedButton() {
        viewBinding.playbackSpeedButton.setOnClickListener(v -> {
            if (controller == null) {
                return;
            }
            if (!controller.canSetPlaybackSpeed()) {
                VariableSpeedDialog.showGetPluginDialog(getContext());
                return;
            }
            List<Float> availableSpeeds = UserPreferences.getPlaybackSpeedArray();
            float currentSpeed = controller.getCurrentPlaybackSpeedMultiplier();

            int newSpeedIndex = 0;
            while (newSpeedIndex < availableSpeeds.size()
                    && availableSpeeds.get(newSpeedIndex) < currentSpeed + EPSILON) {
                newSpeedIndex++;
            }

            float newSpeed;
            if (availableSpeeds.size() == 0) {
                newSpeed = 1.0f;
            } else if (newSpeedIndex == availableSpeeds.size()) {
                newSpeed = availableSpeeds.get(0);
            } else {
                newSpeed = availableSpeeds.get(newSpeedIndex);
            }

            controller.setPlaybackSpeed(newSpeed);
            loadMediaInfo();
        });
        viewBinding.playbackSpeedButton.setOnLongClickListener(v -> {
            new VariableSpeedDialog().show(getChildFragmentManager(), null);
            return true;
        });
        viewBinding.playbackSpeedButton.setVisibility(View.VISIBLE);
        viewBinding.playbackSpeedLabel.setVisibility(View.VISIBLE);
    }

    protected void updatePlaybackSpeedButton(Playable media) {
        if (controller == null) {
            return;
        }
        float speed = 1.0f;
        if (controller.canSetPlaybackSpeed()) {
            speed = PlaybackSpeedUtils.getCurrentPlaybackSpeed(media);
        }
        String speedStr = new DecimalFormat("0.00").format(speed);
        viewBinding.playbackSpeedLabel.setText(speedStr);
        viewBinding.playbackSpeedButton.setSpeed(speed);
        viewBinding.playbackSpeedButton.setAlpha(controller.canSetPlaybackSpeed() ? 1.0f : 0.5f);
        viewBinding.playbackSpeedButton.setVisibility(View.VISIBLE);
        viewBinding.playbackSpeedLabel.setVisibility(View.VISIBLE);
    }

    private void loadMediaInfo() {
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
                        error -> Log.e(TAG, Log.getStackTraceString(error)),
                        () -> updateUi(null));
    }

    private PlaybackController newPlaybackController() {
        return new PlaybackController(getActivity()) {

            @Override
            public void setupGUI() {
                AudioPlayerFragment.this.loadMediaInfo();
            }

            @Override
            public void onBufferStart() {
                viewBinding.progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onBufferEnd() {
                viewBinding.progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onBufferUpdate(float progress) {
                if (isStreaming()) {
                    viewBinding.positionSeek.setSecondaryProgress((int) (progress * viewBinding.positionSeek.getMax()));
                } else {
                    viewBinding.positionSeek.setSecondaryProgress(0);
                }
            }

            @Override
            public void handleError(int code) {
                final AlertDialog.Builder errorDialog = new AlertDialog.Builder(getContext());
                errorDialog.setTitle(R.string.error_label);
                errorDialog.setMessage(MediaPlayerError.getErrorString(getContext(), code));
                errorDialog.setPositiveButton(android.R.string.ok, (dialog, which) ->
                        ((MainActivity) getActivity()).getBottomSheet().setState(BottomSheetBehavior.STATE_COLLAPSED));
                if (!UserPreferences.useExoplayer()) {
                    errorDialog.setNeutralButton(R.string.media_player_switch_to_exoplayer, (dialog, which) -> {
                        UserPreferences.enableExoplayer();
                        ((MainActivity) getActivity()).showSnackbarAbovePlayer(
                                R.string.media_player_switched_to_exoplayer, Snackbar.LENGTH_LONG);
                    });
                }
                errorDialog.create().show();
            }

            @Override
            public void onSleepTimerUpdate() {
                AudioPlayerFragment.this.loadMediaInfo();
            }

            @Override
            public ImageButton getPlayButton() {
                return viewBinding.playButton;
            }

            @Override
            public boolean loadMediaInfo() {
                AudioPlayerFragment.this.loadMediaInfo();
                return true;
            }

            @Override
            public void onShutdownNotification() {
                ((MainActivity) getActivity()).getBottomSheet().setState(BottomSheetBehavior.STATE_COLLAPSED);
            }

            @Override
            public void onPlaybackEnd() {
                ((MainActivity) getActivity()).getBottomSheet().setState(BottomSheetBehavior.STATE_COLLAPSED);
            }

            @Override
            public void onPlaybackSpeedChange() {
                updatePlaybackSpeedButton(getMedia());
            }

            @Override
            public void onSetSpeedAbilityChanged() {
                updatePlaybackSpeedButton(getMedia());
            }
        };
    }

    private void updateUi(Playable media) {
        if (controller == null) {
            return;
        }
        updatePosition(new PlaybackPositionEvent(controller.getPosition(), controller.getDuration()));
        updatePlaybackSpeedButton(media);
        setupOptionsMenu(media);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        controller = newPlaybackController();
        controller.init();
        loadMediaInfo();
        EventBus.getDefault().register(this);
        viewBinding.rewindSecondsLabel.setText(NumberFormat.getInstance().format(UserPreferences.getRewindSecs()));
        viewBinding.forwardLabel.setText(NumberFormat.getInstance().format(UserPreferences.getFastForwardSecs()));
    }

    @Override
    public void onStop() {
        super.onStop();
        controller.release();
        controller = null;
        viewBinding.progressBar.setVisibility(View.GONE); // Controller released; we will not receive buffering updates
        EventBus.getDefault().unregister(this);
        if (disposable != null) {
            disposable.dispose();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updatePosition(PlaybackPositionEvent event) {
        if (controller == null) {
            return;
        }

        TimeSpeedConverter converter = new TimeSpeedConverter(controller.getCurrentPlaybackSpeedMultiplier());
        int currentPosition = converter.convert(event.getPosition());
        int duration = converter.convert(event.getDuration());
        int remainingTime = converter.convert(event.getDuration() - event.getPosition());
        Log.d(TAG, "currentPosition " + Converter.getDurationStringLong(currentPosition));
        if (currentPosition == PlaybackService.INVALID_TIME || duration == PlaybackService.INVALID_TIME) {
            Log.w(TAG, "Could not react to position observer update because of invalid time");
            return;
        }
        viewBinding.positionLabel.setText(Converter.getDurationStringLong(currentPosition));
        if (showTimeLeft) {
            viewBinding.durationLabel.setText("-" + Converter.getDurationStringLong(remainingTime));
        } else {
            viewBinding.durationLabel.setText(Converter.getDurationStringLong(duration));
        }
        float progress = ((float) event.getPosition()) / event.getDuration();
        viewBinding.positionSeek.setProgress((int) (progress * viewBinding.positionSeek.getMax()));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void favoritesChanged(FavoritesEvent event) {
        AudioPlayerFragment.this.loadMediaInfo();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (controller == null) {
            return;
        }
        if (fromUser) {
            float prog = progress / ((float) seekBar.getMax());
            TimeSpeedConverter converter = new TimeSpeedConverter(controller.getCurrentPlaybackSpeedMultiplier());
            int position = converter.convert((int) (prog * duration));
            viewBinding.positionLabel.setText(Converter.getDurationStringLong(position));

            if (showTimeLeft && prog != 0) {
                int timeLeft = converter.convert(duration - (int) (prog * duration));
                String length = "-" + Converter.getDurationStringLong(timeLeft);
                viewBinding.durationLabel.setText(length);
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // interrupt position Observer, restart later
        viewBinding.playtimeContainer.setScaleX(.8f);
        viewBinding.playtimeContainer.setScaleY(.8f);
        viewBinding.playtimeContainer.animate()
                .setInterpolator(new FastOutSlowInInterpolator())
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(200)
                .start();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (controller != null) {
            float prog = seekBar.getProgress() / ((float) seekBar.getMax());
            controller.seekTo((int) (prog * controller.getDuration()));
        }
        cardViewSeek.setScaleX(1f);
        cardViewSeek.setScaleY(1f);
        cardViewSeek.animate()
                .setInterpolator(new FastOutSlowInInterpolator())
                .alpha(0f).scaleX(.8f).scaleY(.8f)
                .setDuration(200)
                .start();
    }

    public void setupOptionsMenu(Playable media) {
        if (viewBinding.toolbar.getMenu().size() == 0) {
            viewBinding.toolbar.inflateMenu(R.menu.mediaplayer);
        }
        if (controller == null) {
            return;
        }
        boolean isFeedMedia = media instanceof FeedMedia;
        viewBinding.toolbar.getMenu().findItem(R.id.open_feed_item).setVisible(isFeedMedia);
        if (isFeedMedia) {
            FeedItemMenuHandler.onPrepareMenu(viewBinding.toolbar.getMenu(), ((FeedMedia) media).getItem());
        }

        viewBinding.toolbar.getMenu().findItem(R.id.set_sleeptimer_item).setVisible(!controller.sleepTimerActive());
        viewBinding.toolbar.getMenu().findItem(R.id.disable_sleeptimer_item).setVisible(controller.sleepTimerActive());

        ((CastEnabledActivity) getActivity()).requestCastButton(viewBinding.toolbar.getMenu());
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (controller == null) {
            return false;
        }
        Playable media = controller.getMedia();
        if (media == null) {
            return false;
        }

        final @Nullable FeedItem feedItem = (media instanceof FeedMedia) ? ((FeedMedia) media).getItem() : null;
        if (feedItem != null && FeedItemMenuHandler.onMenuItemClicked(this, item.getItemId(), feedItem)) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.disable_sleeptimer_item: // Fall-through
            case R.id.set_sleeptimer_item:
                new SleepTimerDialog().show(getChildFragmentManager(), "SleepTimerDialog");
                return true;
            case R.id.audio_controls:
                PlaybackControlsDialog dialog = PlaybackControlsDialog.newInstance();
                dialog.show(getChildFragmentManager(), "playback_controls");
                return true;
            case R.id.open_feed_item:
                if (feedItem != null) {
                    Intent intent = MainActivity.getIntentToOpenFeed(getContext(), feedItem.getFeedId());
                    startActivity(intent);
                }
                return true;
        }
        return false;
    }

    private static class AudioPlayerPagerAdapter extends FragmentStateAdapter {
        private static final String TAG = "AudioPlayerPagerAdapter";

        public AudioPlayerPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Log.d(TAG, "getItem(" + position + ")");
            switch (position) {
                case POS_COVER:
                    return new CoverFragment();
                case POS_DESCR:
                    return new ItemDescriptionFragment();
                default:
                case POS_CHAPTERS:
                    return new ChaptersFragment();
            }
        }

        @Override
        public int getItemCount() {
            return NUM_CONTENT_FRAGMENTS;
        }
    }
}
