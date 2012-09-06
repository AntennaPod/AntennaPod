package de.danoeh.antennapod.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.VideoView;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.service.PlaybackService;
import de.danoeh.antennapod.service.PlayerStatus;

/** Activity for playing audio files. */
public class VideoplayerActivity extends MediaplayerActivity implements
		SurfaceHolder.Callback {
	private static final String TAG = "VideoplayerActivity";

	/** True if video controls are currently visible. */
	private boolean videoControlsShowing = true;
	private boolean videoSurfaceCreated = false;
	private VideoControlsHider videoControlsToggler;

	private LinearLayout videoOverlay;
	private VideoView videoview;
	private ProgressBar progressIndicator;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (videoControlsToggler != null) {
			videoControlsToggler.cancel(true);
		}
	}

	@Override
	protected void setupGUI() {
		super.setupGUI();
		videoOverlay = (LinearLayout) findViewById(R.id.overlay);
		videoview = (VideoView) findViewById(R.id.videoview);
		progressIndicator = (ProgressBar) findViewById(R.id.progressIndicator);
		videoview.getHolder().addCallback(this);
		videoview.setOnTouchListener(onVideoviewTouched);

		setupVideoControlsToggler();
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}

	@Override
	protected void onAwaitingVideoSurface() {
		if (videoSurfaceCreated) {
			if (AppConfig.DEBUG)
				Log.d(TAG,
						"Videosurface already created, setting videosurface now");
			controller.setVideoSurface(videoview.getHolder());
		}
	}

	@Override
	protected void postStatusMsg(int resId) {
		if (resId == R.string.player_preparing_msg) {
			progressIndicator.setVisibility(View.VISIBLE);
		} else {
			progressIndicator.setVisibility(View.INVISIBLE);
		}

	}

	@Override
	protected void clearStatusMsg() {
		progressIndicator.setVisibility(View.INVISIBLE);
	}

	View.OnTouchListener onVideoviewTouched = new View.OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				if (videoControlsToggler != null) {
					videoControlsToggler.cancel(true);
				}
				toggleVideoControlsVisibility();
				if (videoControlsShowing) {
					setupVideoControlsToggler();
				}

				return true;
			} else {
				return false;
			}
		}
	};

	@SuppressLint("NewApi")
	void setupVideoControlsToggler() {
		if (videoControlsToggler != null) {
			videoControlsToggler.cancel(true);
		}
		videoControlsToggler = new VideoControlsHider();
		if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
			videoControlsToggler
					.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			videoControlsToggler.execute();
		}
	}

	private void toggleVideoControlsVisibility() {
		if (videoControlsShowing) {
			getSupportActionBar().hide();
			videoOverlay.setVisibility(View.GONE);
		} else {
			getSupportActionBar().show();
			videoOverlay.setVisibility(View.VISIBLE);
		}
		videoControlsShowing = !videoControlsShowing;
	}

	/** Hides the videocontrols after a certain period of time. */
	public class VideoControlsHider extends AsyncTask<Void, Void, Void> {
		@Override
		protected void onCancelled() {
			videoControlsToggler = null;
		}

		@Override
		protected void onPostExecute(Void result) {
			videoControlsToggler = null;
		}

		private static final int WAITING_INTERVALL = 5000;
		private static final String TAG = "VideoControlsToggler";

		@Override
		protected void onProgressUpdate(Void... values) {
			if (videoControlsShowing) {
				if (AppConfig.DEBUG)
					Log.d(TAG, "Hiding video controls");
				getSupportActionBar().hide();
				videoOverlay.setVisibility(View.GONE);
				videoControlsShowing = false;
			}
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				Thread.sleep(WAITING_INTERVALL);
			} catch (InterruptedException e) {
				return null;
			}
			publishProgress();
			return null;
		}

	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		holder.setFixedSize(width, height);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Videoview holder created");
		videoSurfaceCreated = true;
		if (controller.getStatus() == PlayerStatus.AWAITING_VIDEO_SURFACE) {
			if (controller.serviceAvailable()) {
				controller.setVideoSurface(holder);
			} else {
				Log.e(TAG,
						"Could'nt attach surface to mediaplayer - reference to service was null");
			}
		}

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Videosurface was destroyed");
		videoSurfaceCreated = false;
	}

	@Override
	protected void onReloadNotification(int notificationCode) {
		if (notificationCode == PlaybackService.EXTRA_CODE_AUDIO) {
			if (AppConfig.DEBUG)
				Log.d(TAG,
						"ReloadNotification received, switching to Audioplayer now");
			startActivity(new Intent(this, AudioplayerActivity.class));
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		super.onStartTrackingTouch(seekBar);
		if (videoControlsToggler != null) {
			videoControlsToggler.cancel(true);
		}
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		super.onStopTrackingTouch(seekBar);
		setupVideoControlsToggler();
	}

	@Override
	protected void onBufferStart() {
		progressIndicator.setVisibility(View.VISIBLE);
	}

	@Override
	protected void onBufferEnd() {
		progressIndicator.setVisibility(View.INVISIBLE);
	}

}
