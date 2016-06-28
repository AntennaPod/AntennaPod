package de.danoeh.antennapod.cast;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.MediaRouteControllerDialog;
import android.support.v7.media.MediaRouter;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.danoeh.antennapod.R;

public class CustomMRControllerDialog extends MediaRouteControllerDialog {
    public static final String TAG = "CustomMRContrDialog";

    private MediaRouter mediaRouter;
    private MediaSessionCompat.Token token;

    private ImageView artView;
    private TextView titleView;
    private TextView subtitleView;
    private ImageButton playPauseButton;
    private LinearLayout rootView;

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

        artView = new ImageView(getContext()) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int desiredWidth = widthMeasureSpec;
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

                super.onMeasure(desiredWidth, desiredHeight);
            }
        };
        artView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        artView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        rootView.addView(artView);
        View playbackControlLayout = View.inflate(getContext(), R.layout.media_router_controller, rootView);

        titleView = (TextView) playbackControlLayout.findViewById(R.id.mrc_control_title);
        subtitleView = (TextView) playbackControlLayout.findViewById(R.id.mrc_control_subtitle);
        playPauseButton = (ImageButton) playbackControlLayout.findViewById(R.id.mrc_control_play_pause);

        updateViews();
        return rootView;
    }

    private void updateViews() {
        if (token == null || artView == null || mediaController == null) {
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
        titleView.setVisibility(showTitle ? View.VISIBLE : View.GONE);
        subtitleView.setVisibility(showSubtitle ? View.VISIBLE : View.GONE);

        updateState();

        Bitmap art = metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ART);
        if (art == null) {
            artView.setVisibility(View.GONE);
            return;
        }
        artView.setImageBitmap(art);
        artView.setVisibility(View.VISIBLE);
        rootView.setVisibility(View.VISIBLE);
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
}
