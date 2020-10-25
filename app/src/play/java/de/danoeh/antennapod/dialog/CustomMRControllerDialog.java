package de.danoeh.antennapod.dialog;

import android.app.PendingIntent;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import androidx.annotation.NonNull;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.core.util.Pair;
import androidx.core.view.MarginLayoutParamsCompat;
import androidx.core.view.accessibility.AccessibilityEventCompat;
import androidx.mediarouter.app.MediaRouteControllerDialog;
import androidx.palette.graphics.Palette;
import androidx.mediarouter.media.MediaRouter;
import androidx.appcompat.widget.AppCompatImageView;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.util.concurrent.ExecutionException;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class CustomMRControllerDialog extends MediaRouteControllerDialog {
    public static final String TAG = "CustomMRContrDialog";

    private MediaRouter mediaRouter;
    private MediaSessionCompat.Token token;

    private ImageView artView;
    private TextView titleView;
    private TextView subtitleView;
    private ImageButton playPauseButton;
    private LinearLayout rootView;

    private boolean viewsCreated = false;

    private Disposable fetchArtSubscription;

    private MediaControllerCompat mediaController;
    private MediaControllerCompat.Callback mediaControllerCallback;

    public CustomMRControllerDialog(Context context) {
        this(context, 0);
    }

    private CustomMRControllerDialog(Context context, int theme) {
        super(context, theme);
        mediaRouter = MediaRouter.getInstance(getContext());
        token = mediaRouter.getMediaSessionToken();
        try {
            if (token != null) {
                mediaController = new MediaControllerCompat(getContext(), token);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error creating media controller", e);
        }

        if (mediaController != null) {
            mediaControllerCallback = new MediaControllerCompat.Callback() {
                @Override
                public void onSessionDestroyed() {
                    if (mediaController != null) {
                        mediaController.unregisterCallback(mediaControllerCallback);
                        mediaController = null;
                    }
                }

                @Override
                public void onMetadataChanged(MediaMetadataCompat metadata) {
                    updateViews();
                }

                @Override
                public void onPlaybackStateChanged(PlaybackStateCompat state) {
                    updateState();
                }
            };
            mediaController.registerCallback(mediaControllerCallback);
        }
    }

    @Override
    public View onCreateMediaControlView(Bundle savedInstanceState) {
        boolean landscape = getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        if (landscape) {
            /*
             * When a horizontal LinearLayout measures itself, it first measures its children and
             * settles their widths on the first pass, and only then figures out its height, never
             * revisiting the widths measurements.
             * When one has a child view that imposes a certain aspect ratio (such as an ImageView),
             * then its width and height are related to each other, and so if one allows for a large
             * height, then it will request for itself a large width as well. However, on the first
             * child measurement, the LinearLayout imposes a very relaxed height bound, that the
             * child uses to tell the width it wants, a value which the LinearLayout will interpret
             * as final, even though the child will want to change it once a more restrictive height
             * bound is imposed later.
             *
             * Our solution is, given that the heights of the children do not depend on their widths
             * in this case, we first figure out the layout's height and only then perform the
             * usual sequence of measurements.
             *
             * Note: this solution does not take into account any vertical paddings nor children's
             * vertical margins in determining the height, as this View as well as its children are
             * defined in code and no paddings/margins that would influence these computations are
             * introduced.
             *
             * There were no resources online for this type of issue as far as I could gather.
             */
            rootView = new LinearLayout(getContext()) {
                @Override
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                    // We'd like to find the overall height before adjusting the widths within the LinearLayout
                    int maxHeight = Integer.MIN_VALUE;
                    if (MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY) {
                        for (int i = 0; i < getChildCount(); i++) {
                            int height = Integer.MIN_VALUE;
                            View child = getChildAt(i);
                            ViewGroup.LayoutParams lp = child.getLayoutParams();
                            // we only measure children whose layout_height is not MATCH_PARENT
                            if (lp.height >= 0) {
                                height = lp.height;
                            } else if (lp.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                                child.measure(widthMeasureSpec, heightMeasureSpec);
                                height = child.getMeasuredHeight();
                            }
                            maxHeight = Math.max(maxHeight, height);
                        }
                    }
                    if (maxHeight > 0) {
                        super.onMeasure(widthMeasureSpec,
                                MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.EXACTLY));
                    } else {
                        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                    }
                }
            };
            rootView.setOrientation(LinearLayout.HORIZONTAL);
        } else {
            rootView = new LinearLayout(getContext());
            rootView.setOrientation(LinearLayout.VERTICAL);
        }
        FrameLayout.LayoutParams rootParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        rootParams.setMargins(0, 0, 0,
                getContext().getResources().getDimensionPixelSize(R.dimen.media_router_controller_bottom_margin));
        rootView.setLayoutParams(rootParams);

        // Start the session activity when a content item (album art, title or subtitle) is clicked.
        View.OnClickListener onClickListener = v -> {
            if (mediaController != null) {
                PendingIntent pi = mediaController.getSessionActivity();
                if (pi != null) {
                    try {
                        pi.send();
                        dismiss();
                    } catch (PendingIntent.CanceledException e) {
                        Log.e(TAG, pi + " was not sent, it had been canceled.");
                    }
                }
            }
        };

        LinearLayout.LayoutParams artParams;
        /*
         * On portrait orientation, we want to limit the artView's height to 9/16 of the available
         * width. Reason is that we need to choose the height wisely otherwise we risk the dialog
         * being much larger than the screen, and there doesn't seem to be a good way to know the
         * available height beforehand.
         *
         * On landscape orientation, we want to limit the artView's width to its available height.
         * Otherwise, horizontal images would take too much space and severely restrict the space
         * for episode title and play/pause button.
         *
         * Internal implementation of ImageView only uses the source image's aspect ratio, but we
         * want to impose our own and fallback to the source image's when it is more favorable.
         * Solutions were inspired, among other similar sources, on
         * http://stackoverflow.com/questions/18077325/scale-image-to-fill-imageview-width-and-keep-aspect-ratio
         */
        if (landscape) {
            artView = new AppCompatImageView(getContext()) {
                @Override
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                    int desiredWidth = widthMeasureSpec;
                    int desiredMeasureMode = MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY ?
                            MeasureSpec.EXACTLY : MeasureSpec.AT_MOST;
                    if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY) {
                        Drawable drawable = getDrawable();
                        if (drawable != null) {
                            int intrHeight = drawable.getIntrinsicHeight();
                            int intrWidth = drawable.getIntrinsicWidth();
                            int originalHeight = MeasureSpec.getSize(heightMeasureSpec);
                            if (intrHeight < intrWidth) {
                                desiredWidth = MeasureSpec.makeMeasureSpec(
                                        originalHeight, desiredMeasureMode);
                            } else {
                                desiredWidth = MeasureSpec.makeMeasureSpec(
                                        Math.round((float) originalHeight * intrWidth / intrHeight),
                                        desiredMeasureMode);
                            }
                        }
                    }
                    super.onMeasure(desiredWidth, heightMeasureSpec);
                }
            };
            artParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            MarginLayoutParamsCompat.setMarginStart(artParams,
                    getContext().getResources().getDimensionPixelSize(R.dimen.media_router_controller_playback_control_horizontal_spacing));
        } else {
            artView = new AppCompatImageView(getContext()) {
                @Override
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                    int desiredHeight = heightMeasureSpec;
                    if (MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY) {
                        Drawable drawable = getDrawable();
                        if (drawable != null) {
                            int originalWidth = MeasureSpec.getSize(widthMeasureSpec);
                            int intrHeight = drawable.getIntrinsicHeight();
                            int intrWidth = drawable.getIntrinsicWidth();
                            float scale;
                            if (intrHeight*16 > intrWidth*9) {
                                // image is taller than 16:9
                                scale = (float) originalWidth * 9 / 16 / intrHeight;
                            } else {
                                // image is more horizontal than 16:9
                                scale = (float) originalWidth / intrWidth;
                            }
                            desiredHeight = MeasureSpec.makeMeasureSpec(
                                    Math.round(intrHeight * scale),
                                    MeasureSpec.EXACTLY);
                        }
                    }
                    super.onMeasure(widthMeasureSpec, desiredHeight);
                }
            };
            artParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        // When we fetch the bitmap, we want to know if we should set a background color or not.
        artView.setTag(landscape);

        artView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        artView.setOnClickListener(onClickListener);

        artView.setLayoutParams(artParams);
        rootView.addView(artView);

        ViewGroup wrapper = rootView;

        if (landscape) {
            // Here we wrap with a frame layout because we want to set different layout parameters
            // for landscape orientation.
            wrapper = new FrameLayout(getContext());
            wrapper.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            rootView.addView(wrapper);
            rootView.setWeightSum(1f);
        }

        View playbackControlLayout = View.inflate(getContext(), R.layout.media_router_controller, wrapper);

        titleView = playbackControlLayout.findViewById(R.id.mrc_control_title);
        subtitleView = playbackControlLayout.findViewById(R.id.mrc_control_subtitle);
        playbackControlLayout.findViewById(R.id.mrc_control_title_container).setOnClickListener(onClickListener);
        playPauseButton = playbackControlLayout.findViewById(R.id.mrc_control_play_pause);
        playPauseButton.setOnClickListener(v -> {
            PlaybackStateCompat state;
            if (mediaController != null && (state = mediaController.getPlaybackState()) != null) {
                boolean isPlaying = state.getState() == PlaybackStateCompat.STATE_PLAYING;
                if (isPlaying) {
                    mediaController.getTransportControls().pause();
                } else {
                    mediaController.getTransportControls().play();
                }
                // Announce the action for accessibility.
                AccessibilityManager accessibilityManager = (AccessibilityManager)
                        getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
                if (accessibilityManager != null && accessibilityManager.isEnabled()) {
                    AccessibilityEvent event = AccessibilityEvent.obtain(
                            AccessibilityEventCompat.TYPE_ANNOUNCEMENT);
                    event.setPackageName(getContext().getPackageName());
                    event.setClassName(getClass().getName());
                    int resId = isPlaying ? R.string.mr_controller_pause : R.string.mr_controller_play;
                    event.getText().add(getContext().getString(resId));
                    accessibilityManager.sendAccessibilityEvent(event);
                }
            }
        });

        viewsCreated = true;
        updateViews();
        return rootView;
    }

    @Override
    public void onDetachedFromWindow() {
        if (fetchArtSubscription != null) {
            fetchArtSubscription.dispose();
            fetchArtSubscription = null;
        }
        super.onDetachedFromWindow();
    }

    private void updateViews() {
        if (!viewsCreated || token == null || mediaController == null) {
            rootView.setVisibility(View.GONE);
            return;
        }
        MediaMetadataCompat metadata = mediaController.getMetadata();
        MediaDescriptionCompat description = metadata == null ? null : metadata.getDescription();
        if (description == null) {
            rootView.setVisibility(View.GONE);
            return;
        }

        PlaybackStateCompat state = mediaController.getPlaybackState();
        MediaRouter.RouteInfo route = MediaRouter.getInstance(getContext()).getSelectedRoute();

        CharSequence title = description.getTitle();
        boolean hasTitle = !TextUtils.isEmpty(title);
        CharSequence subtitle = description.getSubtitle();
        boolean hasSubtitle = !TextUtils.isEmpty(subtitle);

        boolean showTitle = false;
        boolean showSubtitle = false;
        if (route.getPresentationDisplay() != null &&
                route.getPresentationDisplay().getDisplayId() != MediaRouter.RouteInfo.PRESENTATION_DISPLAY_ID_NONE) {
            // The user is currently casting screen.
            titleView.setText(R.string.mr_controller_casting_screen);
            showTitle = true;
        } else if (state == null || state.getState() == PlaybackStateCompat.STATE_NONE) {
            // Show "No media selected" as we don't yet know the playback state.
            // (Only exception is bluetooth where we don't show anything.)
            if (!route.isBluetooth()) {
                titleView.setText(R.string.mr_controller_no_media_selected);
                showTitle = true;
            }
        } else if (!hasTitle && !hasSubtitle) {
            titleView.setText(R.string.mr_controller_no_info_available);
            showTitle = true;
        } else {
            if (hasTitle) {
                titleView.setText(title);
                showTitle = true;
            }
            if (hasSubtitle) {
                subtitleView.setText(subtitle);
                showSubtitle = true;
            }
        }
        if (showSubtitle) {
            titleView.setSingleLine();
        } else {
            titleView.setMaxLines(2);
        }
        titleView.setVisibility(showTitle ? View.VISIBLE : View.GONE);
        subtitleView.setVisibility(showSubtitle ? View.VISIBLE : View.GONE);

        updateState();

        if(rootView.getVisibility() != View.VISIBLE) {
            artView.setVisibility(View.GONE);
            rootView.setVisibility(View.VISIBLE);
        }

        if (fetchArtSubscription != null) {
            fetchArtSubscription.dispose();
        }

        fetchArtSubscription = Observable.fromCallable(() -> fetchArt(description))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    fetchArtSubscription = null;
                    if (artView == null) {
                        return;
                    }
                    if (result.first != null) {
                        if (!((Boolean) artView.getTag())) {
                            artView.setBackgroundColor(result.second);
                        }
                        artView.setImageBitmap(result.first);
                        artView.setVisibility(View.VISIBLE);
                    } else {
                        artView.setVisibility(View.GONE);
                    }
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));

    }

    private void updateState() {
        PlaybackStateCompat state;
        if (!viewsCreated || mediaController == null ||
                (state = mediaController.getPlaybackState()) == null) {
            return;
        }
        boolean isPlaying = state.getState() == PlaybackStateCompat.STATE_BUFFERING
                || state.getState() == PlaybackStateCompat.STATE_PLAYING;
        boolean supportsPlay = (state.getActions() & (PlaybackStateCompat.ACTION_PLAY
                | PlaybackStateCompat.ACTION_PLAY_PAUSE)) != 0;
        boolean supportsPause = (state.getActions() & (PlaybackStateCompat.ACTION_PAUSE
                | PlaybackStateCompat.ACTION_PLAY_PAUSE)) != 0;
        if (isPlaying && supportsPause) {
            playPauseButton.setVisibility(View.VISIBLE);
            playPauseButton.setImageResource(getThemeResource(getContext(), R.attr.mediaRoutePauseDrawable));
            playPauseButton.setContentDescription(getContext().getResources().getText(R.string.mr_controller_pause));
        } else if (!isPlaying && supportsPlay) {
            playPauseButton.setVisibility(View.VISIBLE);
            playPauseButton.setImageResource(getThemeResource(getContext(), R.attr.mediaRoutePlayDrawable));
            playPauseButton.setContentDescription(getContext().getResources().getText(R.string.mr_controller_play));
        } else {
            playPauseButton.setVisibility(View.GONE);
        }
    }

    private static int getThemeResource(Context context, int attr) {
        TypedValue value = new TypedValue();
        return context.getTheme().resolveAttribute(attr, value, true) ? value.resourceId : 0;
    }

    @NonNull
    private Pair<Bitmap, Integer> fetchArt(@NonNull MediaDescriptionCompat description) {
        Bitmap iconBitmap = description.getIconBitmap();
        Uri iconUri = description.getIconUri();
        Bitmap art = null;
        if (iconBitmap != null) {
            art = iconBitmap;
        } else if (iconUri != null) {
            try {
                art = Glide.with(getContext().getApplicationContext())
                        .asBitmap()
                        .load(iconUri.toString())
                        .apply(RequestOptions.diskCacheStrategyOf(ApGlideSettings.AP_DISK_CACHE_STRATEGY))
                        .submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                        .get();
            } catch (InterruptedException | ExecutionException e) {
                Log.e(TAG, "Image art load failed", e);
            }
        }
        int backgroundColor = 0;
        if (art != null && art.getWidth()*9 < art.getHeight()*16) {
            // Portrait art requires dominant color as background color.
            Palette palette = new Palette.Builder(art).maximumColorCount(1).generate();
            backgroundColor = palette.getSwatches().isEmpty()
                    ? 0 : palette.getSwatches().get(0).getRgb();
        }
        return new Pair<>(art, backgroundColor);
    }
}
