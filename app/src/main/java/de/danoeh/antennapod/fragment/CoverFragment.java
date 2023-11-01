package de.danoeh.antennapod.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.snackbar.Snackbar;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.feed.util.ImageResourceUtils;
import de.danoeh.antennapod.core.util.ChapterUtils;
import de.danoeh.antennapod.core.util.DateFormatter;
import de.danoeh.antennapod.core.util.playback.PlaybackController;
import de.danoeh.antennapod.databinding.CoverFragmentBinding;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.model.feed.Chapter;
import de.danoeh.antennapod.model.feed.EmbeddedChapterImage;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.playback.Playable;
import io.reactivex.Maybe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static android.widget.LinearLayout.LayoutParams.MATCH_PARENT;
import static android.widget.LinearLayout.LayoutParams.WRAP_CONTENT;

/**
 * Displays the cover and the title of a FeedItem.
 */
public class CoverFragment extends Fragment {
    private static final String TAG = "CoverFragment";
    private CoverFragmentBinding viewBinding;
    private PlaybackController controller;
    private Disposable disposable;
    private int displayedChapterIndex = -1;
    private Playable media;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = CoverFragmentBinding.inflate(inflater);
        viewBinding.imgvCover.setOnClickListener(v -> onPlayPause());
        viewBinding.openDescription.setOnClickListener(view -> ((AudioPlayerFragment) requireParentFragment())
                .scrollToPage(AudioPlayerFragment.POS_DESCRIPTION, true));
        viewBinding.transcriptButton.setOnClickListener(view -> ((AudioPlayerFragment) requireParentFragment())
                .scrollToPage(AudioPlayerFragment.POS_TRANSCRIPT, true));
        ColorFilter colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                viewBinding.txtvPodcastTitle.getCurrentTextColor(), BlendModeCompat.SRC_IN);
        viewBinding.butNextChapter.setColorFilter(colorFilter);
        viewBinding.butPrevChapter.setColorFilter(colorFilter);
        viewBinding.descriptionIcon.setColorFilter(colorFilter);
        viewBinding.chapterButton.setOnClickListener(v ->
                new ChaptersFragment().show(getChildFragmentManager(), ChaptersFragment.TAG));
        viewBinding.butPrevChapter.setOnClickListener(v -> seekToPrevChapter());
        viewBinding.butNextChapter.setOnClickListener(v -> seekToNextChapter());
        return viewBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        configureForOrientation(getResources().getConfiguration());
    }

    private void loadMediaInfo(boolean includingChapters) {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Maybe.<Playable>create(emitter -> {
            Playable media = controller.getMedia();
            if (media != null) {
                if (includingChapters) {
                    ChapterUtils.loadChapters(media, getContext(), false);
                }
                emitter.onSuccess(media);
            } else {
                emitter.onComplete();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(media -> {
                    this.media = media;
                    displayMediaInfo(media);
                    if (media.getChapters() == null && !includingChapters) {
                        loadMediaInfo(true);
                    }
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    private void displayMediaInfo(@NonNull Playable media) {
        String pubDateStr = DateFormatter.formatAbbrev(getActivity(), media.getPubDate());
        viewBinding.txtvPodcastTitle.setText(StringUtils.stripToEmpty(media.getFeedTitle())
                + "\u00A0"
                + "・"
                + "\u00A0"
                + StringUtils.replace(StringUtils.stripToEmpty(pubDateStr), " ", "\u00A0"));
        if (media instanceof FeedMedia) {
            Intent openFeed = MainActivity.getIntentToOpenFeed(requireContext(),
                    ((FeedMedia) media).getItem().getFeedId());
            viewBinding.txtvPodcastTitle.setOnClickListener(v -> startActivity(openFeed));
        } else {
            viewBinding.txtvPodcastTitle.setOnClickListener(null);
        }
        viewBinding.txtvPodcastTitle.setOnLongClickListener(v -> copyText(media.getFeedTitle()));
        viewBinding.txtvEpisodeTitle.setText(media.getEpisodeTitle());
        viewBinding.txtvEpisodeTitle.setOnLongClickListener(v -> copyText(media.getEpisodeTitle()));
        viewBinding.txtvEpisodeTitle.setOnClickListener(v -> {
            int lines = viewBinding.txtvEpisodeTitle.getLineCount();
            int animUnit = 1500;
            if (lines > viewBinding.txtvEpisodeTitle.getMaxLines()) {
                int titleHeight = viewBinding.txtvEpisodeTitle.getHeight()
                        - viewBinding.txtvEpisodeTitle.getPaddingTop()
                        - viewBinding.txtvEpisodeTitle.getPaddingBottom();
                ObjectAnimator verticalMarquee = ObjectAnimator.ofInt(
                        viewBinding.txtvEpisodeTitle, "scrollY", 0, (lines - viewBinding.txtvEpisodeTitle.getMaxLines())
                                        * (titleHeight / viewBinding.txtvEpisodeTitle.getMaxLines()))
                        .setDuration(lines * animUnit);
                ObjectAnimator fadeOut = ObjectAnimator.ofFloat(
                        viewBinding.txtvEpisodeTitle, "alpha", 0);
                fadeOut.setStartDelay(animUnit);
                fadeOut.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        viewBinding.txtvEpisodeTitle.scrollTo(0, 0);
                    }
                });
                ObjectAnimator fadeBackIn = ObjectAnimator.ofFloat(
                        viewBinding.txtvEpisodeTitle, "alpha", 1);
                AnimatorSet set = new AnimatorSet();
                set.playSequentially(verticalMarquee, fadeOut, fadeBackIn);
                set.start();
            }
        });
        
        displayedChapterIndex = -1;
        refreshChapterData(ChapterUtils.getCurrentChapterIndex(media, media.getPosition())); //calls displayCoverImage
        updateChapterControlVisibility();

        if (media.hasTranscript()) {
            if (viewBinding.transcriptButton.getVisibility() != View.VISIBLE) {
                viewBinding.transcriptButton.setVisibility(View.VISIBLE);
            }
        } else {
            viewBinding.transcriptButton.setVisibility(View.GONE);
        }
    }

    private void updateChapterControlVisibility() {
        boolean chapterControlVisible = false;
        if (media.getChapters() != null) {
            chapterControlVisible = media.getChapters().size() > 0;
        } else if (media instanceof FeedMedia) {
            FeedMedia fm = ((FeedMedia) media);
            // If an item has chapters but they are not loaded yet, still display the button.
            chapterControlVisible = fm.getItem() != null && fm.getItem().hasChapters();
        }
        int newVisibility = chapterControlVisible ? View.VISIBLE : View.GONE;
        if (viewBinding.chapterButton.getVisibility() != newVisibility) {
            viewBinding.chapterButton.setVisibility(newVisibility);
            ObjectAnimator.ofFloat(viewBinding.chapterButton,
                    "alpha",
                    chapterControlVisible ? 0 : 1,
                    chapterControlVisible ? 1 : 0)
                    .start();
        }
    }

    private void refreshChapterData(int chapterIndex) {
        if (chapterIndex > -1) {
            if (media.getPosition() > media.getDuration() || chapterIndex >= media.getChapters().size() - 1) {
                displayedChapterIndex = media.getChapters().size() - 1;
                viewBinding.butNextChapter.setVisibility(View.INVISIBLE);
            } else {
                displayedChapterIndex = chapterIndex;
                viewBinding.butNextChapter.setVisibility(View.VISIBLE);
            }
        }

        displayCoverImage();
    }

    private Chapter getCurrentChapter() {
        if (media == null || media.getChapters() == null || displayedChapterIndex == -1) {
            return null;
        }
        return media.getChapters().get(displayedChapterIndex);
    }

    private void seekToPrevChapter() {
        Chapter curr = getCurrentChapter();

        if (controller == null || curr == null || displayedChapterIndex == -1) {
            return;
        }

        if (displayedChapterIndex < 1) {
            controller.seekTo(0);
        } else if ((controller.getPosition() - 10000 * controller.getCurrentPlaybackSpeedMultiplier())
                < curr.getStart()) {
            refreshChapterData(displayedChapterIndex - 1);
            controller.seekTo((int) media.getChapters().get(displayedChapterIndex).getStart());
        } else {
            controller.seekTo((int) curr.getStart());
        }
    }

    private void seekToNextChapter() {
        if (controller == null || media == null || media.getChapters() == null
                || displayedChapterIndex == -1 || displayedChapterIndex + 1 >= media.getChapters().size()) {
            return;
        }

        refreshChapterData(displayedChapterIndex + 1);
        controller.seekTo((int) media.getChapters().get(displayedChapterIndex).getStart());
    }

    @Override
    public void onStart() {
        super.onStart();
        controller = new PlaybackController(getActivity()) {
            @Override
            public void loadMediaInfo() {
                CoverFragment.this.loadMediaInfo(false);
            }
        };
        controller.init();
        loadMediaInfo(false);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        if (disposable != null) {
            disposable.dispose();
        }
        controller.release();
        controller = null;
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(PlaybackPositionEvent event) {
        int newChapterIndex = ChapterUtils.getCurrentChapterIndex(media, event.getPosition());
        if (newChapterIndex > -1 && newChapterIndex != displayedChapterIndex) {
            refreshChapterData(newChapterIndex);
        }
    }

    private void displayCoverImage() {
        RequestOptions options = new RequestOptions()
                .dontAnimate()
                .transform(new FitCenter(),
                        new RoundedCorners((int) (16 * getResources().getDisplayMetrics().density)));

        RequestBuilder<Drawable> cover = Glide.with(this)
                .load(media.getImageLocation())
                .error(Glide.with(this)
                        .load(ImageResourceUtils.getFallbackImageLocation(media))
                        .apply(options))
                .apply(options);

        if (displayedChapterIndex == -1 || media == null || media.getChapters() == null
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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        configureForOrientation(newConfig);
    }

    private void configureForOrientation(Configuration newConfig) {
        boolean isPortrait = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT;

        viewBinding.coverFragment.setOrientation(isPortrait ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);

        if (isPortrait) {
            viewBinding.coverHolder.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, 0, 1));
            viewBinding.coverFragmentTextContainer.setLayoutParams(
                    new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        } else {
            viewBinding.coverHolder.setLayoutParams(new LinearLayout.LayoutParams(0, MATCH_PARENT, 1));
            viewBinding.coverFragmentTextContainer.setLayoutParams(new LinearLayout.LayoutParams(0, MATCH_PARENT, 1));
        }

        ((ViewGroup) viewBinding.episodeDetails.getParent()).removeView(viewBinding.episodeDetails);
        if (isPortrait) {
            viewBinding.coverFragment.addView(viewBinding.episodeDetails);
        } else {
            viewBinding.coverFragmentTextContainer.addView(viewBinding.episodeDetails);
        }
    }

    void onPlayPause() {
        if (controller == null) {
            return;
        }
        controller.playPause();
    }

    private boolean copyText(String text) {
        ClipboardManager clipboardManager = ContextCompat.getSystemService(requireContext(), ClipboardManager.class);
        if (clipboardManager != null) {
            clipboardManager.setPrimaryClip(ClipData.newPlainText("AntennaPod", text));
        }
        if (Build.VERSION.SDK_INT < 32) {
            ((MainActivity) requireActivity()).showSnackbarAbovePlayer(
                    getResources().getString(R.string.copied_to_clipboard), Snackbar.LENGTH_SHORT);
        }
        return true;
    }
}
