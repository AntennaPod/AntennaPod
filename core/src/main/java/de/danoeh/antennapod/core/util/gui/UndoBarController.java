package de.danoeh.antennapod.core.util.gui;

import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.view.ViewHelper;
import com.nineoldandroids.view.ViewPropertyAnimator;

import de.danoeh.antennapod.core.R;

import static com.nineoldandroids.view.ViewPropertyAnimator.animate;

public class UndoBarController<T> {
    private View mBarView;
    private TextView mMessageView;
    private ViewPropertyAnimator mBarAnimator;
    private Handler mHideHandler = new Handler();

    private UndoListener<T> mUndoListener;

    // State objects
    private T mUndoToken;
    private CharSequence mUndoMessage;

    public interface UndoListener<T> {
        /**
         * This callback function is called when the undo button is pressed
         *
         * @param token
         */
        void onUndo(T token);

        /**
         *
         * This callback function is called when the bar fades out without button press
         *
         * @param token
         */
        void onHide(T token);
    }

    public UndoBarController(View undoBarView, UndoListener<T> undoListener) {
        mBarView = undoBarView;
        mBarAnimator = animate(mBarView);
        mUndoListener = undoListener;

        mMessageView = (TextView) mBarView.findViewById(R.id.undobar_message);
        mBarView.findViewById(R.id.undobar_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        hideUndoBar(false);
                        mUndoListener.onUndo(mUndoToken);
                    }
                });

        hideUndoBar(true);
    }

    public void showUndoBar(boolean immediate, CharSequence message, T undoToken) {
        mUndoToken = undoToken;
        mUndoMessage = message;
        mMessageView.setText(mUndoMessage);

        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable,
                mBarView.getResources().getInteger(R.integer.undobar_hide_delay));

        mBarView.setVisibility(View.VISIBLE);
        if (immediate) {
            ViewHelper.setAlpha(mBarView, 1);
        } else {
            mBarAnimator.cancel();
            mBarAnimator
                    .alpha(1)
                    .setDuration(
                            mBarView.getResources()
                                    .getInteger(android.R.integer.config_shortAnimTime))
                    .setListener(null);
        }
    }

    public boolean isShowing() {
        return mBarView.getVisibility() == View.VISIBLE;
    }

    public void close() {
        hideUndoBar(true);
        mUndoListener.onHide(mUndoToken);
    }

    public void hideUndoBar(boolean immediate) {
        mHideHandler.removeCallbacks(mHideRunnable);
        if (immediate) {
            mBarView.setVisibility(View.GONE);
            ViewHelper.setAlpha(mBarView, 0);
            mUndoMessage = null;
        } else {
            mBarAnimator.cancel();
            mBarAnimator
                    .alpha(0)
                    .setDuration(mBarView.getResources()
                            .getInteger(android.R.integer.config_shortAnimTime))
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mBarView.setVisibility(View.GONE);
                            mUndoMessage = null;
                            mUndoToken = null;
                        }
                    });
        }
    }

    private Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hideUndoBar(false);
            mUndoListener.onHide(mUndoToken);
        }
    };
}
