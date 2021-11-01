package de.danoeh.antennapod.playback.cast;

import android.content.Context;
import androidx.annotation.NonNull;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;

public class CastStateListener implements SessionManagerListener<CastSession> {
    private final CastContext castContext;

    public CastStateListener(Context context) {
        castContext = CastContext.getSharedInstance(context);
        castContext.getSessionManager().addSessionManagerListener(this, CastSession.class);
    }

    public void destroy() {
        castContext.getSessionManager().removeSessionManagerListener(this, CastSession.class);
    }

    @Override
    public void onSessionStarting(@NonNull CastSession castSession) {
    }

    @Override
    public void onSessionStarted(@NonNull CastSession session, @NonNull String sessionId) {
        onSessionStartedOrEnded();
    }

    @Override
    public void onSessionStartFailed(@NonNull CastSession castSession, int i) {
    }

    @Override
    public void onSessionEnding(@NonNull CastSession castSession) {
    }

    @Override
    public void onSessionResumed(@NonNull CastSession session, boolean wasSuspended) {
    }

    @Override
    public void onSessionResumeFailed(@NonNull CastSession castSession, int i) {
    }

    @Override
    public void onSessionSuspended(@NonNull CastSession castSession, int i) {
    }

    @Override
    public void onSessionEnded(@NonNull CastSession session, int error) {
        onSessionStartedOrEnded();
    }

    @Override
    public void onSessionResuming(@NonNull CastSession castSession, @NonNull String s) {
    }

    public void onSessionStartedOrEnded() {
    }
}
