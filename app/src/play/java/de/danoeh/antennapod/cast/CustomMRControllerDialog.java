package de.danoeh.antennapod.cast;

import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.support.v7.app.MediaRouteControllerDialog;
import android.support.v7.graphics.Palette;
import android.support.v7.media.MediaRouter;
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
import com.bumptech.glide.request.target.Target;

import java.util.concurrent.ExecutionException;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.glide.ApGlideSettings;

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

    private FetchArtTask fetchArtTask;

    private MediaControllerCompat mediaController;
    private MediaControllerCompat.Callback mediaControllerCallback;

    public CustomMRControllerDialog(Context context) {
        this(context, 0);
    }

    public CustomMRControllerDialog(Context context, int theme) {
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
        rootView = new LinearLayout(getContext());
        rootView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        rootView.setOrientation(LinearLayout.VERTICAL);

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

        artView = new ImageView(getContext()) {
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
                        desiredHeight = MeasureSpec.makeMeasureSpec((int) (intrHeight * scale + 0.5f), MeasureSpec.EXACTLY);
                    }
                }

                super.onMeasure(widthMeasureSpec, desiredHeight);
            }
        };
        artView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        artView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        artView.setOnClickListener(onClickListener);

        rootView.addView(artView);
        View playbackControlLayout = View.inflate(getContext(), R.layout.media_router_controller, rootView);

        titleView = (TextView) playbackControlLayout.findViewById(R.id.mrc_control_title);
        subtitleView = (TextView) playbackControlLayout.findViewById(R.id.mrc_control_subtitle);
        playbackControlLayout.findViewById(R.id.mrc_control_title_container).setOnClickListener(onClickListener);
        playPauseButton = (ImageButton) playbackControlLayout.findViewById(R.id.mrc_control_play_pause);
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
                    int resId = isPlaying ?
                            android.support.v7.mediarouter.R.string.mr_controller_pause : android.support.v7.mediarouter.R.string.mr_controller_play;
                    event.getText().add(getContext().getString(resId));
                    accessibilityManager.sendAccessibilityEvent(event);
                }
            }
        });

        viewsCreated = true;
        updateViews();
        return rootView;
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
        if (route.getPresentationDisplayId() != MediaRouter.RouteInfo.PRESENTATION_DISPLAY_ID_NONE) {
            // The user is currently casting screen.
            titleView.setText(android.support.v7.mediarouter.R.string.mr_controller_casting_screen);
            showTitle = true;
        } else if (state == null || state.getState() == PlaybackStateCompat.STATE_NONE) {
            // Show "No media selected" as we don't yet know the playback state.
            // (Only exception is bluetooth where we don't show anything.)
            if (!route.isDeviceTypeBluetooth()) {
                titleView.setText(android.support.v7.mediarouter.R.string.mr_controller_no_media_selected);
                showTitle = true;
            }
        } else if (!hasTitle && !hasSubtitle) {
            titleView.setText(android.support.v7.mediarouter.R.string.mr_controller_no_info_available);
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

        if (fetchArtTask != null) {
            fetchArtTask.cancel(true);
        }

        fetchArtTask = new FetchArtTask(description);
        fetchArtTask.execute();
    }

    private void updateState() {
        if (mediaController == null) {
            return;
        }
        PlaybackStateCompat state = mediaController.getPlaybackState();
        if (state != null) {
            boolean isPlaying = state.getState() == PlaybackStateCompat.STATE_BUFFERING
                    || state.getState() == PlaybackStateCompat.STATE_PLAYING;
            boolean supportsPlay = (state.getActions() & (PlaybackStateCompat.ACTION_PLAY
                    | PlaybackStateCompat.ACTION_PLAY_PAUSE)) != 0;
            boolean supportsPause = (state.getActions() & (PlaybackStateCompat.ACTION_PAUSE
                    | PlaybackStateCompat.ACTION_PLAY_PAUSE)) != 0;
            if (isPlaying && supportsPause) {
                playPauseButton.setVisibility(View.VISIBLE);
                playPauseButton.setImageResource(getThemeResource(getContext(),
                        android.support.v7.mediarouter.R.attr.mediaRoutePauseDrawable));
                playPauseButton.setContentDescription(getContext().getResources()
                        .getText(android.support.v7.mediarouter.R.string.mr_controller_pause));
            } else if (!isPlaying && supportsPlay) {
                playPauseButton.setVisibility(View.VISIBLE);
                playPauseButton.setImageResource(getThemeResource(getContext(),
                        android.support.v7.mediarouter.R.attr.mediaRoutePlayDrawable));
                playPauseButton.setContentDescription(getContext().getResources()
                        .getText(android.support.v7.mediarouter.R.string.mr_controller_play));
            } else {
                playPauseButton.setVisibility(View.GONE);
            }
        }
    }

    private static int getThemeResource(Context context, int attr) {
        TypedValue value = new TypedValue();
        return context.getTheme().resolveAttribute(attr, value, true) ? value.resourceId : 0;
    }

    private class FetchArtTask extends AsyncTask<Void, Void, Bitmap> {
        final Bitmap iconBitmap;
        final Uri iconUri;
        int backgroundColor;

        FetchArtTask(@NonNull MediaDescriptionCompat description) {
            iconBitmap = description.getIconBitmap();
            iconUri = description.getIconUri();
        }

        @Override
        protected Bitmap doInBackground(Void... arg) {
            Bitmap art = null;
            if (iconBitmap != null) {
                art = iconBitmap;
            } else if (iconUri != null) {
                try {
                    art = Glide.with(getContext().getApplicationContext())
                            .load(iconUri.toString())
                            .asBitmap()
                            .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                            .into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                            .get();
                } catch (InterruptedException | ExecutionException e) {
                    Log.e(TAG, "Image art load failed", e);
                }
            }
            if (art != null && art.getWidth()*9 < art.getHeight()*16) {
                // Portrait art requires dominant color as background color.
                Palette palette = new Palette.Builder(art).maximumColorCount(1).generate();
                backgroundColor = palette.getSwatches().isEmpty()
                        ? 0 : palette.getSwatches().get(0).getRgb();
            }
            return art;
        }

        @Override
        protected void onCancelled() {
            fetchArtTask = null;
        }

        @Override
        protected void onPostExecute(Bitmap art) {
            fetchArtTask = null;
            if (art != null) {
                artView.setBackgroundColor(backgroundColor);
                artView.setImageBitmap(art);
                artView.setVisibility(View.VISIBLE);
            } else {
                artView.setVisibility(View.GONE);
            }
        }
    }
}
