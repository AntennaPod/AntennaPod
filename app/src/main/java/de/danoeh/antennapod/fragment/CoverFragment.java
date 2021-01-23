package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.RequestOptions;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.event.PlaybackPositionEvent;
import de.danoeh.antennapod.core.feed.util.ImageResourceUtils;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.util.ChapterUtils;
import de.danoeh.antennapod.core.util.EmbeddedChapterImage;
import de.danoeh.antennapod.core.util.playback.Playable;
import de.danoeh.antennapod.core.util.playback.PlaybackController;
import io.reactivex.Maybe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Displays the cover and the title of a FeedItem.
 */
public class CoverFragment extends Fragment {

    private static final String TAG = "CoverFragment";
    static final double SIXTEEN_BY_NINE = 1.7;

    private View root;
    private TextView txtvPodcastTitle;
    private TextView txtvEpisodeTitle;
    private ImageView imgvCover;
    private PlaybackController controller;
    private Disposable disposable;
    private int displayedChapterIndex = -2;
    private Playable media;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setRetainInstance(true);
        root = inflater.inflate(R.layout.cover_fragment, container, false);
        txtvPodcastTitle = root.findViewById(R.id.txtvPodcastTitle);
        txtvEpisodeTitle = root.findViewById(R.id.txtvEpisodeTitle);
        imgvCover = root.findViewById(R.id.imgvCover);
        imgvCover.setOnClickListener(v -> onPlayPause());
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        configureForOrientation(getResources().getConfiguration());
    }

    private void loadMediaInfo() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Maybe.<Playable>create(emitter -> {
            Playable media = controller.getMedia();
            if (media != null) {
                emitter.onSuccess(media);
            } else {
                emitter.onComplete();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(media -> {
                    this.media = media;
                    displayMediaInfo(media);
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    private void displayMediaInfo(@NonNull Playable media) {
        txtvPodcastTitle.setText(media.getFeedTitle());
        txtvEpisodeTitle.setText(media.getEpisodeTitle());
        displayedChapterIndex = -2; // Force refresh
        displayCoverImage(media.getPosition());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // prevent memory leaks
        root = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        controller = new PlaybackController(getActivity()) {
            @Override
            public boolean loadMediaInfo() {
                CoverFragment.this.loadMediaInfo();
                return true;
            }

            @Override
            public void setupGUI() {
                CoverFragment.this.loadMediaInfo();
            }
        };
        controller.init();
        loadMediaInfo();
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
        if (media == null) {
            return;
        }
        displayCoverImage(event.getPosition());
    }

    private void displayCoverImage(int position) {
        int chapter = ChapterUtils.getCurrentChapterIndex(media, position);
        if (chapter != displayedChapterIndex) {
            displayedChapterIndex = chapter;

            RequestOptions options = new RequestOptions()
                    .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                    .dontAnimate()
                    .transforms(new FitCenter(),
                            new RoundedCorners((int) (16 * getResources().getDisplayMetrics().density)));

            RequestBuilder<Drawable> cover = Glide.with(this)
                    .load(ImageResourceUtils.getImageLocation(media))
                    .error(Glide.with(this)
                            .load(ImageResourceUtils.getFallbackImageLocation(media))
                            .apply(options))
                    .apply(options);

            if (chapter == -1 || TextUtils.isEmpty(media.getChapters().get(chapter).getImageUrl())) {
                cover.into(imgvCover);
            } else {
                Glide.with(this)
                        .load(EmbeddedChapterImage.getModelFor(media, chapter))
                        .apply(options)
                        .thumbnail(cover)
                        .error(cover)
                        .into(imgvCover);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        configureForOrientation(newConfig);
    }

    public float convertDpToPixel(float dp) {
        Context context = this.getActivity().getApplicationContext();
        return dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    private void configureForOrientation(Configuration newConfig) {
        LinearLayout mainContainer = getView().findViewById(R.id.cover_fragment);
        LinearLayout textContainer = getView().findViewById(R.id.cover_fragment_text_container);

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) imgvCover.getLayoutParams();
        LinearLayout.LayoutParams textParams = (LinearLayout.LayoutParams) textContainer.getLayoutParams();
        double ratio = (float) newConfig.screenHeightDp / (float) newConfig.screenWidthDp;

        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            double percentageWidth = 0.8;
            if (ratio <= SIXTEEN_BY_NINE) {
                percentageWidth = (ratio / SIXTEEN_BY_NINE) * percentageWidth * 0.8;
            }
            mainContainer.setOrientation(LinearLayout.VERTICAL);
            if (newConfig.screenWidthDp > 0) {
                params.width = (int) (convertDpToPixel(newConfig.screenWidthDp) * percentageWidth);
                params.height = params.width;
                textParams.weight = 0;
                imgvCover.setLayoutParams(params);
            }
        } else {
            double percentageHeight = ratio * 0.8;
            mainContainer.setOrientation(LinearLayout.HORIZONTAL);
            if (newConfig.screenHeightDp > 0) {
                params.height = (int) (convertDpToPixel(newConfig.screenHeightDp) * percentageHeight);
                params.width = params.height;
                textParams.weight = 1;
                imgvCover.setLayoutParams(params);
            }
        }
    }

    void onPlayPause() {
        if (controller == null) {
            return;
        }
        controller.playPause();
    }
}
