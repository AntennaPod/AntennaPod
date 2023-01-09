package de.danoeh.antennapod.core.dialog;

public interface StatusListener {
    void onActionSuccess();

    void onActionFailure(Throwable throwableError);
}
